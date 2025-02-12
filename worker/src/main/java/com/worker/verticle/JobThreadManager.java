package com.worker.verticle;

import com.alibaba.fastjson2.JSON;
import com.common.constant.Constant;
import com.common.entity.JobInstance;
import com.common.entity.JobReport;
import com.common.entity.NodeInfo;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hazelcast.core.HazelcastInstance;
import com.worker.config.WorkerConfigure;
import io.vertx.core.Vertx;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JobThreadManager implements InitializingBean {

    private final Vertx vertx;

    private final HazelcastInstance hazelcast;

    private final WorkerConfigure configure;

    private final NodeInfo nodeInfo;

    private ExecutorService jobExecutor;

    private ScheduledExecutorService monitorExecutor;

    private Map<Long, JobThreadHolder<?>> jobMap;

    @Override
    public void afterPropertiesSet() {
        // 任务线程池
        WorkerConfigure.JobThreadConfigure jobThreadPool = configure.getJobThreadPool();
        ThreadFactory jobFactory = new ThreadFactoryBuilder().setNameFormat(jobThreadPool.getThreadNameFormat()).build();
        ArrayBlockingQueue<Runnable> taskQueue = new ArrayBlockingQueue<>(jobThreadPool.getQueueCapacity());
        jobExecutor = new ThreadPoolExecutor(jobThreadPool.getCorePoolSize(), jobThreadPool.getMaxPoolSize(), jobThreadPool.getKeepAliveSeconds(), TimeUnit.MILLISECONDS, taskQueue, jobFactory);

        // 任务监控线程池
        final int availableProcessors = Runtime.getRuntime().availableProcessors();
        ThreadFactory monitorFactory = new ThreadFactoryBuilder().setNameFormat(String.join("-", "monitor", jobThreadPool.getThreadNameFormat())).build();
        monitorExecutor = new ScheduledThreadPoolExecutor(availableProcessors, monitorFactory);

        jobMap = new ConcurrentHashMap<>();
    }


    @PreDestroy
    private void destroy() {
        jobExecutor.shutdown();
        monitorExecutor.shutdown();
    }


    public void runJob(JobInstance instance, Supplier<JobReport> task, BiConsumer<JobReport, Throwable> callback) {
        // 异步执行任务并绑定回调函数
        CompletableFuture<JobReport> jobFuture = CompletableFuture.supplyAsync(task, jobExecutor);
        jobFuture.whenComplete(callback);

        // 监控任务执行状态
        ScheduledFuture<?> monitorFuture = monitorExecutor.scheduleAtFixedRate(() -> statusReport(instance), 5, 15, TimeUnit.SECONDS);
        jobMap.put(instance.getId(), JobThreadHolder.of(jobFuture, monitorFuture));
    }


    public void stopJob(JobInstance instance) {
        JobThreadHolder<?> jobThreadHolder = jobMap.remove(instance.getId());
        Optional.ofNullable(jobThreadHolder.getJobfuture()).ifPresent(f -> f.cancel(true));
        Optional.ofNullable(jobThreadHolder.getMonitorFuture()).ifPresent(f -> f.cancel(true));
    }


    public boolean isJobRunning(JobInstance instance) {
        // 任务被终止
        if (hazelcast.getMap(Constant.JOB_TERMINATION).containsKey(instance.getId())) {
            return false;
        }
        // 任务结束或被终止
        if (!jobMap.containsKey(instance.getId())) {
            return false;
        }
        // 非工作流任务
        if (instance.getFlowInstanceId() == null || instance.getJobFlowVersion() == null) {
            return true;
        }
        // 工作流实例终止状态
        String key = String.format("%d_%d", instance.getFlowInstanceId(), instance.getJobFlowVersion());
        return !hazelcast.getMap(Constant.JOB_FLOW_TERMINATION).containsKey(key);
    }


    private void statusReport(JobInstance instance) {
        log.info("task-monitor id = {}", instance.getInstanceId());
        JobReport report;
        if (isJobRunning(instance)) {
            report = JobReport.running("任务执行中");
            String jsonString = JSON.toJSONString(report.workerAddress(nodeInfo.getServerAddress()).jobInstance(instance));
            vertx.eventBus().send(Constant.DISPATCH_REPORT, jsonString);
        } else {
            stopJob(instance);
        }
    }


    @Data
    private static final class JobThreadHolder<T> {

        private Future<T> jobfuture;

        private Future<?> monitorFuture;

        public static <T> JobThreadHolder<T> of(Future<T> jobfuture, Future<?> monitorFuture) {
            JobThreadHolder<T> holder = new JobThreadHolder<>();
            holder.jobfuture = jobfuture;
            holder.monitorFuture = monitorFuture;
            return holder;
        }
    }

}