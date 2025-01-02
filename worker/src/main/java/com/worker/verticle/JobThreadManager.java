package com.worker.verticle;

import com.common.constant.Constant;
import com.common.entity.JobInstance;
import com.common.entity.JobReport;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.worker.config.JobThreadConfigure;
import io.vertx.core.Vertx;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobThreadManager {

    private final Vertx vertx;

    private final JobThreadConfigure configure;

    private ExecutorService jobExecutor;

    private ScheduledExecutorService monitorExecutor;

    private Map<Long, JobThreadHolder<?>> jobMap;

    @PostConstruct
    private void init() {
        // 任务线程池
        ThreadFactory jobFactory = new ThreadFactoryBuilder().setNameFormat("job-worker-%d").build();
        jobExecutor = new ThreadPoolExecutor(configure.getCorePoolSize(), configure.getMaxPoolSize(), configure.getKeepAliveSeconds(), TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(configure.getQueueCapacity()), jobFactory);

        // 任务监控线程池
        final int availableProcessors = Runtime.getRuntime().availableProcessors();
        ThreadFactory monitorFactory = new ThreadFactoryBuilder().setNameFormat("job-worker-monitor-%d").build();
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
        ScheduledFuture<?> monitorFuture = monitorExecutor.scheduleWithFixedDelay(() -> statusReport(instance, jobFuture), 15, 15, TimeUnit.SECONDS);
        jobMap.put(instance.getId(), JobThreadHolder.of(jobFuture, monitorFuture));
    }


    public void stopJob(JobInstance instance, boolean isForceStop) {
        JobThreadHolder<?> jobThreadHolder = jobMap.remove(instance.getId());
        Optional.ofNullable(jobThreadHolder.getMonitorFuture()).ifPresent(f -> f.cancel(true));
        if (isForceStop) {
            Optional.ofNullable(jobThreadHolder.getJobfuture()).ifPresent(f -> f.cancel(true));
        }
    }


    public boolean isJobRunning(JobInstance instance) {
        return jobMap.containsKey(instance.getId());
    }


    private void statusReport(JobInstance instance, Future<JobReport> jobFuture) {
        log.info("task-monitor id = {}", instance.getInstanceId());
        JobReport report;
        if (isJobRunning(instance)) {
            report = JobReport.running("任务执行中");
        } else if (jobFuture.isCancelled()) {
            report = JobReport.fail("任务被取消");
        } else { // job done
            return;
        }
        vertx.eventBus().send(Constant.DISPATCH_REPORT, report.replenish(instance));
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