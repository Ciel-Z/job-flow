package com.admin.service.impl;

import com.admin.mapper.JobInfoMapper;
import com.admin.mapper.JobInstanceMapper;
import com.admin.service.JobDispatchService;
import com.admin.verticle.MessageDispatcher;
import com.admin.vo.JobInstanceVO;
import com.common.constant.Constant;
import com.common.entity.JobInfo;
import com.common.entity.JobInstance;
import com.common.entity.JobReport;
import com.common.enums.JobStatusEnum;
import com.common.util.AssertUtils;
import com.common.util.PathUtil;
import com.hazelcast.core.HazelcastInstance;
import io.vertx.core.Vertx;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobDispatchServiceImpl implements JobDispatchService {

    private final Vertx vertx;

    private final JobInfoMapper jobMapper;

    private final JobInstanceMapper jobInstanceMapper;

    private final HazelcastInstance hazelcast;

    private final MessageDispatcher messageDispatcher;

    @Override
    public void start(Long jobId, String instanceParams, long delayMS) {
        JobInfo jobInfo = jobMapper.selectByPrimaryKey(jobId);
        AssertUtils.isNotNull(jobInfo, "Job not found");
        AssertUtils.notEmpty(jobInfo.getProcessorInfo(), "ProcessorInfo is empty");

        JobInstance instance = instance(jobInfo, instanceParams);
        // 符合启动条件, 异步启动任务
        vertx.setTimer(Math.max(1, jobInfo.getRetryInterval()), id -> {
            log.info("start job jobId: {}, instanceId: {}", instance.getJobId(), instance.getInstanceId());
            doStart(instance);
        });
    }


    public void doStart(JobInstance instance) {
        instance.setStatus(JobStatusEnum.DISPATCH.getCode());
        JobInfo jobInfo = instance.getJobInfo();
        jobInstanceMapper.insert(instance);
        messageDispatcher.dispatcher(jobInfo.getProcessorInfo()).doDispatch(instance).request(instance, JobReport.class, (report, e) -> {
            instance.setStatus(Optional.ofNullable(report).map(JobReport::getStatus).orElse(JobStatusEnum.FAIL.getCode()));
            instance.setResult(Optional.ofNullable(report).map(JobReport::getResult).orElse(""));
            if (e != null) {
                instance.setEndTime(LocalDateTime.now());
                instance.setResult(e.getMessage());
                if (instance.getCurrentRetryTimes() < jobInfo.getMaxRetryTimes()) {
                    // retry
                    vertx.setTimer(Math.max(1, jobInfo.getRetryInterval()), id -> {
                        JobInstance nextInstance = instance.clone();
                        nextInstance.setCurrentRetryTimes(instance.getCurrentRetryTimes() + 1);
                        doStart(nextInstance);
                    });
                }
            }
            jobInstanceMapper.updateByPrimaryKey(instance);
        });
    }


    @Override
    public void stop(JobInstanceVO jobInstanceVO) {
        JobInstance jobInstance = jobInstanceMapper.selectByPrimaryKey(jobInstanceVO.getId());
        AssertUtils.isNotNull(jobInstance, "Instance not found");
        AssertUtils.isTrue(jobInstance.getStatus() == 1, "The current state does not allow stopping");
        jobInstance.setStatus(JobStatusEnum.FAIL.getCode());
        jobInstance.setEndTime(LocalDateTime.now());
        jobInstance.setResult("强制终止");
        jobInstanceMapper.updateByPrimaryKey(jobInstance);

        // 修改任务实例全局状态 | 任务监控线程上报状态前会检查此状态, 非运行状态时会停止任务线程
        hazelcast.getMap(Constant.STATE_MACHINE).put(jobInstance.getId(), JobStatusEnum.FAIL.getCode());

        // 发送停止任务消息, 通知执行器停止任务
        messageDispatcher.dispatcher(PathUtil.getGlobalPath(jobInstance.getWorkerAddress(), Constant.STOP_JOB)).send(jobInstance);
    }


    private JobInstance instance(JobInfo jobInfo, String instanceParams) {
        JobInstance jobInstance = new JobInstance();
        BeanUtils.copyProperties(jobInfo, jobInstance);
        jobInstance.setTriggerTime(LocalDateTime.now());
        long instanceId = hazelcast.getFlakeIdGenerator("JobInstanceId").newId();
        jobInstance.setInstanceId(instanceId);
        jobInstance.setParams(Optional.ofNullable(instanceParams).orElse(jobInfo.getParams()));
        jobInstance.setJobInfo(jobInfo);
        jobInstance.setCurrentRetryTimes(1L);
        return jobInstance;
    }
}
