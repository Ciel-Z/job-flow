package com.admin.verticle;

import com.admin.mapper.JobInstanceMapper;
import com.common.annotation.VerticlePath;
import com.common.constant.Constant;
import com.common.entity.JobEvent;
import com.common.entity.JobReport;
import com.common.vertx.AbstractEventVerticle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Slf4j
@Component
@RequiredArgsConstructor
@VerticlePath(Constant.DISPATCH_REPORT)
public class JobStatusVerticle extends AbstractEventVerticle<JobReport> {

    private final JobInstanceMapper jobInstanceMapper;

    @Override
    public void process(JobEvent<JobReport> jobEvent) {
        JobReport jobReport = jobEvent.getBody();
        log.info("WorkerJObReport {} instanceId = {} worker = {} status = {} {}", jobReport.getJobName(), jobReport.getInstanceId(), jobReport.getWorkerAddress(), jobReport.getStatus(), jobReport.getResult());

        // executeBlocking 处理阻塞任务
        getVertx().executeBlocking((Callable<Void>) () -> {
            // update job instance info
            jobInstanceMapper.updateByEvent(jobReport);
            return null;
        }).onFailure(e -> {
            log.error("WorkerJObReport {} instanceId = {} worker = {} jobReport = {}", jobReport.getJobName(), jobReport.getInstanceId(), jobReport.getWorkerAddress(), jobReport, e);
        });
    }

}
