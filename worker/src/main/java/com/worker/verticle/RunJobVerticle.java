package com.worker.verticle;

import com.alibaba.fastjson2.JSON;
import com.common.annotation.VerticlePath;
import com.common.constant.Constant;
import com.common.entity.*;
import com.common.util.AssertUtils;
import com.common.vertx.AbstractEventVerticle;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.worker.config.JobThreadManager;
import lombok.RequiredArgsConstructor;
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

    private final HazelcastInstance hazelcast;

    private final JobThreadManager taskThreadManager;

    private final ApplicationContext applicationContext;

    private final NodeInfo localNode;


    @Override
    public void process(JobEvent<JobInstance> event) {
        JobInstance instance = event.getBody();
        // 通知任务已开始
        notifyBeginningJob(event, instance);
        // 异步执行任务 & 绑定执行结果处理回调
        taskThreadManager.runJob(instance, () -> run(instance), (result, exception) -> {
            // 任务执行中被停止的情况
            if (!taskThreadManager.isJobRunning(instance)) {
                log.warn("RunJobVerticle-warn 任务人为终止 {} {}", instance.getJobName(), instance.getJobId());
                return;
            }
            reply(result.jobInstance(instance));
            taskThreadManager.stopJob(instance, false);
        });
        log.info("RunJobVerticle dispatched {} jobName = {}", instance.getProcessorInfo(), instance.getJobName());
    }


    public JobReport run(JobInstance instance) {
        try {
            Class<?> clazz = Class.forName(instance.getProcessorInfo());
            Object job = applicationContext.getBean(clazz);
            AssertUtils.isNotNull(job, "{} 任务处理器未找到", instance.getProcessorInfo());

            // 任务处理器类型校验
            AssertUtils.isTrue(job instanceof JobHandler, "{} 任务处理器未实现 com.common.entity.JobHandler 接口", instance.getProcessorInfo());
            JobHandler handler = (JobHandler) job;

            // 整理上下文
            IMap<String, Object> workflowContext = hazelcast.getMap(String.valueOf(instance.getFlowInstanceId()));
            JobContext jobContext = new JobContext();
            BeanUtils.copyProperties(instance, jobContext);
            jobContext.setWorkflowContext(workflowContext);

            // 执行任务
            JobReport handle = handler.handle(jobContext);
            AssertUtils.isNotNull(handle, "{} 任务处理器返回结果为空", instance.getProcessorInfo());
            log.info("RunJobVerticle [{}]-{} param = {} result = {}", instance.getJobName(), instance.getInstanceId(), instance.getParams(), handle.getResult());
            return handle.workerAddress(localNode.getServerAddress()).jobInstance(instance);
        } catch (Exception e) {
            log.error("RunJobVerticle [{}]-{} param = {} error", instance.getJobName(), instance.getInstanceId(), instance.getParams(), e);
            return JobReport.fail("执行失败", e).workerAddress(localNode.getServerAddress()).jobInstance(instance);
        }
    }

    /**
     * 通知任务已开始
     */
    private void notifyBeginningJob(JobEvent<JobInstance> event, JobInstance instance) {
        event.getMessage().reply(JSON.toJSONString(JobReport.running("开始执行").workerAddress(localNode.getServerAddress()).jobInstance(instance)));
    }

    /**
     * 回执执行结果
     */
    private void reply(JobReport jobReport) {
        vertx.eventBus().send(Constant.DISPATCH_REPORT, JSON.toJSONString(jobReport));
    }
}
