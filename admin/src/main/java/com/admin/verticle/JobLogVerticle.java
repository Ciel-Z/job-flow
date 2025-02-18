package com.admin.verticle;

import com.admin.mapper.JobLogMapper;
import com.common.annotation.VerticlePath;
import com.common.constant.Constant;
import com.common.entity.JobEvent;
import com.common.entity.JobLogReport;
import com.common.vertx.AbstractEventVerticle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * worker 上报任务日志处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
@VerticlePath(Constant.JOB_LOG)
public class JobLogVerticle extends AbstractEventVerticle<JobLogReport> {

    private final JobLogMapper jobLogMapper;

    @Override
    public void process(JobEvent<JobLogReport> jobEvent) {
        JobLogReport jobLogReport = jobEvent.getBody();
        log.info("JobLogVerticle process instanceLogsSize = {}", jobLogReport.getLogs().size());
        // executeBlocking 处理阻塞任务
        getVertx().executeBlocking((Callable<Void>) () -> {
            jobLogMapper.batchInsert(jobLogReport);
            return null;
        }).onFailure(e -> {
            log.error("JobLogVerticle process error", e);
        });
    }
}
