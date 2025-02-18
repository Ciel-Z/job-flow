package com.worker.verticle;

import com.alibaba.fastjson2.JSON;
import com.common.annotation.VerticlePath;
import com.common.constant.Constant;
import com.common.entity.JobEvent;
import com.common.entity.JobInstance;
import com.common.entity.JobReport;
import com.common.entity.NodeInfo;
import com.common.util.AssertUtils;
import com.common.vertx.AbstractEventVerticle;
import com.hazelcast.core.HazelcastInstance;
import com.worker.entity.JobContext;
import com.worker.entity.JobHandler;
import com.worker.logger.DefaultJobLogger;
import com.worker.logger.LoggerHandler;
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

    private final LoggerHandler loggerHandler;

    private final HazelcastInstance hazelcast;

    private final JobThreadManager taskThreadManager;

    private final ApplicationContext applicationContext;

    private final NodeInfo localNode;


    @Override
    public void process(JobEvent<JobInstance> event) {
        JobInstance instance = event.getBody();
        instance.setWorkerAddress(localNode.getServerAddress());
        // 通知任务已开始
        replyBeginning(event, instance);
        // 异步执行任务 & 绑定执行结果处理回调
        taskThreadManager.runJob(instance, () -> run(instance), (result, exception) -> {
            // 任务执行中被停止的情况
            if (!taskThreadManager.isJobRunning(instance)) {
                log.warn("RunJobVerticle-warn 任务被强制终止 {} {}", instance.getJobName(), instance.getJobId());
                return;
            }
            replyResult(result, instance);
            taskThreadManager.stopJob(instance);
        });
        log.info("RunJobVerticle dispatched {} jobName = {}", instance.getProcessorInfo(), instance.getJobName());
    }


    public JobReport run(JobInstance instance) {
        try {
            Class<?> clazz = Class.forName(instance.getProcessorInfo());
            Object job = applicationContext.getBean(clazz);
            AssertUtils.isNotNull(job, "{} 任务处理器未找到", instance.getProcessorInfo());

            // 任务处理器类型校验
            AssertUtils.isTrue(job instanceof JobHandler, "{} 任务处理器未实现 com.worker.entity.JobHandler 接口", instance.getProcessorInfo());
            JobHandler handler = (JobHandler) job;

            // 整理上下文
            JobContext jobContext = new JobContext();
            BeanUtils.copyProperties(instance, jobContext);
            jobContext.setLogger(new DefaultJobLogger(instance, loggerHandler));
            jobContext.setWorkflowContext(hazelcast.getMap(String.format("job_flow_%d", instance.getFlowInstanceId())));

            // 执行任务
            JobReport report = handler.handle(jobContext);
            AssertUtils.isNotNull(report, "{} 任务处理器返回结果为空", instance.getProcessorInfo());
            log.info("RunJobVerticle [{}]-{} param = {} result = {}", instance.getJobName(), instance.getInstanceId(), instance.getParams(), report.getResult());
            return report.jobInstance(instance);
        } catch (Exception e) {
            log.error("RunJobVerticle [{}]-{} param = {} error", instance.getJobName(), instance.getInstanceId(), instance.getParams(), e);
            return JobReport.fail("执行失败", e).jobInstance(instance);
        }
    }


    /**
     * 通知任务已开始
     */
    private void replyBeginning(JobEvent<JobInstance> event, JobInstance instance) {
        event.getMessage().reply(JSON.toJSONString(JobReport.running("开始执行").jobInstance(instance)));
    }


    /**
     * 回执执行结果
     */
    private void replyResult(JobReport jobReport, JobInstance instance) {
        vertx.eventBus().send(Constant.DISPATCH_REPORT, JSON.toJSONString(jobReport.jobInstance(instance)));
    }
}
