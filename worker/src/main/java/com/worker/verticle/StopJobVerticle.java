package com.worker.verticle;


import com.common.annotation.VerticlePath;
import com.common.constant.Constant;
import com.common.entity.Event;
import com.common.entity.JobInstance;
import com.common.vertx.AbstractEventVerticle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 处理任务执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
@VerticlePath(Constant.STOP_JOB)
public class StopJobVerticle extends AbstractEventVerticle<JobInstance> {

    private final JobThreadManager taskThreadManager;

    @Override
    public void process(Event<JobInstance> event) {
        taskThreadManager.stopJob(event.getBody(), true);
    }
}
