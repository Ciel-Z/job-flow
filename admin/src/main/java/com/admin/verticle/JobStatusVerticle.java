package com.admin.verticle;

import com.admin.mapper.JobInstanceMapper;
import com.admin.service.JobFlowEventService;
import com.common.annotation.VerticlePath;
import com.common.constant.Constant;
import com.common.entity.JobEvent;
import com.common.entity.JobReport;
import com.common.enums.JobStatusEnum;
import com.common.vertx.AbstractEventVerticle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * worker 上报任务状态处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
@VerticlePath(Constant.DISPATCH_REPORT)
public class JobStatusVerticle extends AbstractEventVerticle<JobReport> {

    private final JobInstanceMapper jobInstanceMapper;

    private final JobFlowEventService jobFlowEventService;


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
            jobFlowEventService.processJobFlowEvent(jobReport);
        }
    }
}
