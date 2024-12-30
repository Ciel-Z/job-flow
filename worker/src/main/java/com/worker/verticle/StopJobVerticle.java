package com.worker.verticle;


import com.common.entity.Event;
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
public class StopJobVerticle extends AbstractEventVerticle<Object> {
    @Override
    public String signature() {
        return "";
    }

    @Override
    public void process(Event<Object> event) {

    }

//    private final JobThreadManager taskThreadManager;
//
//    @Override
//    public String path() {
//        return String.format("%s-%s", JobConstant.STOP_JOB, VertxConfig.localIP());
//    }
//
//    @Override
//    public void process(Event<JobInstanceTrack> event) {
//        JobInstanceTrack track = event.getBody();
//        taskThreadManager.stopJob(track);
//    }

}
