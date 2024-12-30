package com.worker.verticle;

import com.common.entity.JobInstance;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hazelcast.core.HazelcastInstance;
import io.vertx.core.Vertx;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobThreadManager {

//    private final Vertx vertx;
//
//    private final HazelcastInstance hazelcastInstance;
//
//    private ExecutorService jobExecutor;
//
//    private ScheduledExecutorService monitorExecutor;
//
//    private Map<Long, List<Future<?>>> jobMap;
//
//
//    // TODO 线程池要优化, 线程数根据配置文件配置
//    @PostConstruct
//    private void init() {
//        // 任务线程池
//        ThreadFactory jobFactory = new ThreadFactoryBuilder().setNameFormat("job-worker-%d").build();
//        jobExecutor = Executors.newFixedThreadPool(5, jobFactory);
//
//        // 任务监控线程池
//        final int availableProcessors = Runtime.getRuntime().availableProcessors();
//        ThreadFactory monitorFactory = new ThreadFactoryBuilder().setNameFormat("job-worker-monitor-%d").build();
//        monitorExecutor = Executors.newScheduledThreadPool(availableProcessors, monitorFactory);
//
//        jobMap = new ConcurrentHashMap<>();
//    }
//
//
//    @PreDestroy
//    private void destroy() {
//        jobExecutor.shutdown();
//        monitorExecutor.shutdown();
//    }
//
//
//    public void runJob(JobInstance instance, Supplier<JobReport> task, BiConsumer<JobReport, Throwable> callback) {
//        // 异步执行任务并绑定回调函数
//        CompletableFuture<JobReport> taskFuture = CompletableFuture.supplyAsync(task, jobExecutor);
//        taskFuture.whenComplete(callback);
//
//        // 监控任务执行状态
//        ScheduledFuture<?> monitorFuture = monitorExecutor.scheduleWithFixedDelay(() -> checkStateMachine(instance), 15, 15, TimeUnit.SECONDS);
//        jobMap.put(instance.getInstanceId(), Arrays.asList(taskFuture, monitorFuture));
//    }
//
//
//    public void stopJob(JobInstance instance) {
//        List<Future<?>> list = jobMap.remove(instance.getInstanceId());
//        Optional.ofNullable(list).ifPresent(futures -> futures.forEach(future -> future.cancel(true)));
//    }
//
//
//    public boolean isJobRunning(JobInstance instance) {
//        return jobMap.containsKey(instance.getInstanceId());
//    }
//
//
//    // TODO 改为超时检测
//    private void checkStateMachine(JobInstance instance) {
//        log.info("task-monitor id = {}", instance.getInstanceId());
//
//
//    }

}