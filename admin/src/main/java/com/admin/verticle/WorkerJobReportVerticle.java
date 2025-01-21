package com.admin.verticle;

import com.admin.mapper.JobInstanceMapper;
import com.common.annotation.VerticlePath;
import com.common.constant.Constant;
import com.common.entity.JobEvent;
import com.common.entity.JobInfo;
import com.common.entity.JobInstance;
import com.common.entity.JobReport;
import com.common.vertx.AbstractEventVerticle;
import io.vertx.core.Vertx;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Slf4j
@Component
@RequiredArgsConstructor
@VerticlePath(Constant.DISPATCH_REPORT)
public class WorkerJobReportVerticle extends AbstractEventVerticle<JobReport> {

    private final JobInstanceMapper jobInstanceMapper;

    @Override
    public void process(JobEvent<JobReport> jobEvent) {
        JobReport jobReport = jobEvent.getBody();
        JobInstance instance = jobReport.getInstance();
        JobInfo jobInfo = instance.getJobInfo();
        log.info("WorkerJObReport {} instanceId = {} worker = {} jobReport = {}", jobInfo.getJobName(), jobReport.getInstance().getInstanceId(), jobEvent.getMessage().replyAddress(), jobReport);

        Vertx vertx = getVertx();
        // executeBlocking 处理阻塞任务
        vertx.executeBlocking((Callable<Void>) () -> {
            // update job instance info
            jobInstanceMapper.updateByEvent(jobReport);
            return null;
        }).onFailure(e -> {
            log.error("WorkerJObReport {} instanceId = {} worker = {} jobReport = {}", jobInfo.getJobName(), jobReport.getInstance().getInstanceId(), jobEvent.getMessage().replyAddress(), jobReport, e);
        });
    }

}
