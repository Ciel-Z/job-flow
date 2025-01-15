package com.worker.verticle;

import com.alibaba.fastjson2.JSON;
import com.common.annotation.VerticlePath;
import com.common.constant.Constant;
import com.common.entity.*;
import com.common.vertx.AbstractEventVerticle;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.vertx.core.Vertx;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 处理任务执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
@VerticlePath(Constant.DISPATCH)
public class RunJobVerticle extends AbstractEventVerticle<JobInstance> {

    private final Vertx vertx;

    private final HazelcastInstance hazelcast;

    private final JobThreadManager taskThreadManager;

    private final ApplicationContext applicationContext;


    @Override
    public void process(JobEvent<JobInstance> event) {
        JobInstance instance = event.getBody();

        // 回执任务已开始
        event.getMessage().reply(JSON.toJSONString(JobReport.running("开始执行").replenish(instance)));
        // 异步执行任务 & 绑定执行结果处理回调
        taskThreadManager.runJob(instance, () -> run(instance), (result, exception) -> {
            // 任务执行中被停止的情况
            if (!taskThreadManager.isJobRunning(instance)) {
                log.warn("RunJobVerticle-warn 任务被人为终止 {} {}", instance.getJobName(), instance.getJobId());
                return;
            }
            if (result != null && exception == null) {
                vertx.eventBus().send(Constant.DISPATCH_REPORT, result.replenish(instance));
            } else {
                vertx.eventBus().send(Constant.DISPATCH_REPORT, JobReport.fail("执行失败", exception).replenish(instance));
            }
            taskThreadManager.stopJob(instance, false);
        });
        log.info("RunJobVerticle processed jobName = {}", instance.getJobName());
    }


    @SneakyThrows
    public JobReport run(JobInstance instance) {
        Class<?> clazz = Class.forName(instance.getProcessorInfo());
        Object job = applicationContext.getBean(clazz);

        // 任务处理器类型校验
        if (!(job instanceof JobHandler handler)) {
            throw new IllegalArgumentException("job is not instance of JobHandler");
        }

        // 整理上下文
        IMap<String, Object> workflowContext = hazelcast.getMap(String.valueOf(instance.getFlowInstanceId()));
        JobContext jobContext = new JobContext();
        BeanUtils.copyProperties(instance, jobContext);
        jobContext.setWorkflowContext(workflowContext);

        // 执行任务
        return handler.handle(jobContext);
    }

}
