package com.worker.verticle;

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
public class RunJobVerticle extends AbstractEventVerticle<JobInstance> {
    @Override
    public String signature() {
        return "";
    }

    @Override
    public void process(Event<JobInstance> event) {

    }

//    private final Vertx vertx;
//
//    private final JobThreadManager taskThreadManager;
//
//    private final ApplicationContext applicationContext;
//
//
//    @Override
//    public String path() {
//        return Constant.DISPATCH;
//    }
//
//    @Override
//    public void process(Event<JobInstance> event) {
//        JobInstance instanceTrack = event.getBody();
//
//        // 回执任务已开始
//        event.getMessage().reply(JSON.toJSONString(JobStatusReport.running("开始执行").replenish(instanceTrack)));
//        // 异步执行任务 & 绑定执行结果处理回调
//        taskThreadManager.runJob(instanceTrack, () -> run(instanceTrack), (result, exception) -> {
//            // 任务执行中被停止的情况
//            if (!taskThreadManager.isJobRunning(instanceTrack)) {
//                log.warn("RunJobVerticle-warn 任务被人为终止 {} {} {}", accountPeriod, instanceTrack.getJobNode(), instanceTrack.getPresent());
//                return;
//            }
//            if (result != null && exception == null) {
//                vertx.eventBus().send(JobConstant.DISPATCH_REPORT, result.replenish(instanceTrack));
//            } else {
//                vertx.eventBus().send(JobConstant.DISPATCH_REPORT, JobReport.fail("执行失败", exception).replenish(instanceTrack));
//            }
//            taskThreadManager.stopJob(instanceTrack);
//        });
//        log.info("RunJobVerticle processed accountPeriod = {}", accountPeriod);
//    }
//
//
//    @SneakyThrows
//    public JobReport run(JobInstanceTrack instanceTrack) {
//        // 任务执行中被停止的情况
//        if (!taskThreadManager.isStateMachineRunning(instanceTrack.getAccountPeriod())) {
//            throw new IllegalStateException("state machine is not running");
//        }
//
//        Class<?> clazz = Class.forName(instanceTrack.getProcessorInfo());
//        Object job = applicationContext.getBean(clazz);
//
//        // 任务处理器类型校验
//        if (!(job instanceof JobHandler)) {
//            throw new IllegalArgumentException("job is not instance of JobHandler");
//        }
//
//        // 整理上下文
//        IMap<String, Object> workflowContext = vertxFacade.getIMap(instanceTrack.getAccountPeriod());
//        JobContext jobContext = new JobContext(instanceTrack.getAccountPeriod(), instanceTrack.getParams(), workflowContext);
//
//        // 执行任务
//        JobHandler handler = (JobHandler) job;
//        return handler.handle(jobContext);
//    }

}
