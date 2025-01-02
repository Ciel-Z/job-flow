package com.admin.service.impl;

import com.admin.mapper.JobInfoMapper;
import com.admin.mapper.JobInstanceMapper;
import com.admin.service.JobDispatchService;
import com.admin.uid.IdGenerateService;
import com.admin.vo.JobInstanceVO;
import com.common.constant.Constant;
import com.common.entity.JobInfo;
import com.common.entity.JobInstance;
import com.common.entity.JobReport;
import com.common.enums.JobStatusEnum;
import com.common.util.AssertUtils;
import com.common.vertx.MessageService;
import io.vertx.core.Vertx;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobDispatchServiceImpl implements JobDispatchService {

    private final Vertx vertx;

    private final JobInfoMapper jobMapper;

    private final JobInstanceMapper jobInstanceMapper;

    private final IdGenerateService idGenerateService;

    private final MessageService messageService;

    @Override
    public void start(JobInstanceVO request) {
        JobInfo jobInfo = Optional.ofNullable(request.getJobInfo()).orElse(jobMapper.selectByPrimaryKey(request.getJobId()));
        AssertUtils.notNull(jobInfo, "job not found");
        AssertUtils.notEmpty(jobInfo.getProcessorInfo(), "processorInfo is empty");

        JobInstance instance = instance(request, jobInfo);
        jobInstanceMapper.insert(instance);
        messageService.call(Constant.DISPATCH, instance, JobReport.class, (report, e) -> {
            instance.setStatus(Optional.ofNullable(report).map(JobReport::getStatus).orElse(JobStatusEnum.FAIL.getCode()));
            // retry
            if (e != null) {
                instance.setEndTime(LocalDateTime.now());
                if (request.getCurrentRetryTimes() < jobInfo.getMaxRetryTimes()) {
                    vertx.setTimer(jobInfo.getRetryInterval(), id -> {
                        request.setCurrentRetryTimes(request.getCurrentRetryTimes() + 1);
                        request.setJobInfo(jobInfo);
                        request.setInstanceId(instance.getInstanceId());
                        start(request);
                    });
                }
                // TODO 异常 log 处理
            }
            jobInstanceMapper.updateByPrimaryKey(instance);
        });
    }

    @Override
    public void stop(JobInstanceVO jobInstanceVO) {
        JobInstance jobInstance = jobInstanceMapper.selectByPrimaryKey(jobInstanceVO.getId());
        AssertUtils.notNull(jobInstance, "jobInstance not found");
        jobInstance.setStatus(JobStatusEnum.FAIL.getCode());
        jobInstance.setEndTime(LocalDateTime.now());
        jobInstance.setResult("强制终止");
        jobInstanceMapper.updateByPrimaryKey(jobInstance);

        // 修改任务实例全局状态
        messageService.getHazelcast().getMap(Constant.STATE_MACHINE).put(jobInstance.getId(), JobStatusEnum.FAIL.getCode());
        // 发布停止任务消息, 通知执行器停止任务
        messageService.publish(Constant.STOP_JOB, jobInstance);
    }


    private JobInstance instance(JobInstanceVO request, JobInfo jobInfo) {
        JobInstance jobInstance = new JobInstance();
        BeanUtils.copyProperties(jobInfo, jobInstance);
        jobInstance.setTriggerTime(LocalDateTime.now());
        jobInstance.setInstanceId(Optional.ofNullable(request.getInstanceId()).orElse(idGenerateService.allocate()));
        jobInstance.setJobInfo(jobInfo);
        return jobInstance;
    }
}
