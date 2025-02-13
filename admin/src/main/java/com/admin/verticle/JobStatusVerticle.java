package com.admin.verticle;

import com.admin.mapper.JobFlowInstanceMapper;
import com.admin.mapper.JobInstanceMapper;
import com.admin.service.JobFlowDispatchService;
import com.common.annotation.VerticlePath;
import com.common.constant.Constant;
import com.common.entity.JobEvent;
import com.common.entity.JobReport;
import com.common.enums.JobStatusEnum;
import com.common.vertx.AbstractEventVerticle;
import com.hazelcast.core.HazelcastInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Slf4j
@Component
@RequiredArgsConstructor
@VerticlePath(Constant.DISPATCH_REPORT)
public class JobStatusVerticle extends AbstractEventVerticle<JobReport> {

    private final HazelcastInstance hazelcast;

    private final JobInstanceMapper jobInstanceMapper;

    private final JobFlowInstanceMapper jobFlowInstanceMapper;

    private final JobFlowDispatchService jobFlowDispatchService;



    @Override
    public void process(JobEvent<JobReport> jobEvent) {
        JobReport jobReport = jobEvent.getBody();
        log.info("WorkerJObReport {} instanceId = {} worker = {} status = {} {}", jobReport.getJobName(), jobReport.getInstanceId(), jobReport.getWorkerAddress(), jobReport.getStatus(), jobReport.getResult());

        // executeBlocking 处理阻塞任务
        getVertx().executeBlocking((Callable<Void>) () -> {
            processReport(jobReport);
            return null;
        }).onFailure(e -> {
            log.error("WorkerJObReport {} instanceId = {} worker = {} jobReport = {}", jobReport.getJobName(), jobReport.getInstanceId(), jobReport.getWorkerAddress(), jobReport, e);
        });
    }


    private void processReport(JobReport jobReport) {
        jobInstanceMapper.updateByEvent(jobReport);

        // 工作流所属任务
        if (jobReport.getFlowInstanceId() != null && jobReport.getFlowNodeId() != null && !JobStatusEnum.RUNNING.getCode().equals(jobReport.getStatus())) {
            jobFlowDispatchService.processJobFlowEvent(jobReport);
        }
    }
}
