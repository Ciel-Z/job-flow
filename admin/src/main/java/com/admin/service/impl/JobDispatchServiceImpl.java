package com.admin.service.impl;

import com.admin.entity.TableInfo;
import com.admin.mapper.JobInfoMapper;
import com.admin.mapper.JobInstanceMapper;
import com.admin.mapper.JobLogMapper;
import com.admin.service.JobDispatchService;
import com.admin.service.JobFlowEventService;
import com.admin.verticle.MessageDispatcher;
import com.admin.vo.JobInstanceVO;
import com.common.constant.Constant;
import com.common.entity.*;
import com.common.enums.JobStatusEnum;
import com.common.util.AssertUtils;
import com.common.util.PathUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.pagehelper.Page;
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

    private final HazelcastInstance hazelcast;

    private final JobInfoMapper jobMapper;

    private final JobLogMapper jobLogMapper;

    private final JobInstanceMapper jobInstanceMapper;

    private final JobFlowEventService jobFlowEventService;

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
            start(instance);
        });
    }


    @Override
    public void start(JobInstance instance) {
        instance.setStatus(JobStatusEnum.DISPATCH.getCode());
        JobInfo jobInfo = instance.getJobInfo();
        jobInstanceMapper.insert(instance);
        messageDispatcher.dispatcher(jobInfo.getProcessorInfo()).doDispatch(instance).request(instance, JobReport.class, (report, e) -> {
            instance.setStatus(Optional.ofNullable(report).map(JobReport::getStatus).orElse(JobStatusEnum.FAIL.getCode()));
            instance.setResult(Optional.ofNullable(report).map(JobReport::getResult).orElse(null));
            instance.setWorkerAddress(Optional.ofNullable(report).map(JobReport::getWorkerAddress).orElse(null));
            if (e != null) {
                instance.setEndTime(LocalDateTime.now());
                instance.setResult(e.getMessage());
                if (instance.getCurrentRetryTimes() < jobInfo.getMaxRetryTimes()) {
                    // retry
                    vertx.setTimer(Math.max(1, jobInfo.getRetryInterval()), id -> {
                        JobInstance nextInstance = instance.clone();
                        nextInstance.setCurrentRetryTimes(instance.getCurrentRetryTimes() + 1);
                        start(nextInstance);
                    });
                }
            }
            jobFlowStatusProcess(instance);
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

        // 修改任务实例全局状态 | 任务监控线程上报状态检查此状态, 非运行状态时会停止任务线程
        hazelcast.getMap(Constant.JOB_TERMINATION).put(jobInstance.getId(), true);

        // 发送停止任务消息, 通知执行器停止任务
        messageDispatcher.dispatcher(PathUtil.getGlobalPath(jobInstance.getWorkerAddress(), Constant.STOP_JOB)).send(jobInstance);

        // 更新任务实例全局状态
        jobFlowStatusProcess(jobInstance);
    }


    @Override
    public TableInfo<JobLog> log(Long instanceId) {
        Page<JobLog> page = jobLogMapper.selectPageByInstanceId(instanceId);
        return TableInfo.of(page.getTotal(), page.getResult());
    }


    @Override
    public JobInstance instance(JobInfo jobInfo, String instanceParams) {
        return instance(jobInfo, instanceParams, null, null);
    }


    @Override
    public JobInstance instance(JobInfo jobInfo, String instanceParams, Long flowInstanceId, Long flowNodeId) {
        JobInstance jobInstance = new JobInstance();
        BeanUtils.copyProperties(jobInfo, jobInstance);
        jobInstance.setTriggerTime(LocalDateTime.now());
        jobInstance.setReplyTime(jobInstance.getTriggerTime());
        jobInstance.setInstanceId(hazelcast.getFlakeIdGenerator("JobInstanceId").newId());
        jobInstance.setParams(Optional.ofNullable(instanceParams).orElse(jobInfo.getParams()));
        jobInstance.setJobInfo(jobInfo);
        jobInstance.setFlowInstanceId(flowInstanceId);
        jobInstance.setFlowNodeId(flowNodeId);
        // 设置重试次数 (任务流触发时, 暂不自动重试)
        jobInstance.setCurrentRetryTimes(flowInstanceId != null && flowNodeId != null ? jobInfo.getMaxRetryTimes() : 1L);
        return jobInstance;
    }


    /**
     * 任务流实例状态处理 | 1.worker 任务响应 2.强制停止
     *
     * @param instance 任务实例
     */
    private void jobFlowStatusProcess(JobInstance instance) {
        if (instance.getFlowInstanceId() == null || instance.getFlowNodeId() == null) {
            return;
        }
        JobReport jobReport = JobStatusEnum.RUNNING.getCode().equals(instance.getStatus()) ? JobReport.running("运行中") : JobReport.fail(instance.getResult());
        jobReport.jobInstance(instance);
        jobFlowEventService.processJobFlowEvent(jobReport);
    }

    private static String instance2Json(JobFlowInstance flowInstance) {
        try {
            return new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(flowInstance);
        } catch (JsonProcessingException e) {
            log.info("Failed to serialize JobFlowInstance to JSON", e);
            return "";
        }
    }
}
