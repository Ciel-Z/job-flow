package com.admin.scheduler;

import com.admin.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Lazy
@Slf4j
@Service
@RequiredArgsConstructor
public class ServerScheduleManager implements InitializingBean, DisposableBean {

    private final ScheduleService scheduleService;

    private final List<Thread> threadContainer = new ArrayList<>();


    @Override
    public void afterPropertiesSet() {
        // 心跳检测
        addThread("Heartbeat", ScheduleService.RUNNING_INTERVAL, scheduleService::heartbeat);

        // 任务检查并分配
        addThread("ProcessOverdueJob", ScheduleService.RUNNING_INTERVAL, scheduleService::processOverdueJob);

        // 任务调度
        addThread("ScheduleJob", ScheduleService.RUNNING_INTERVAL, scheduleService::scheduleJob);

        // 任务心跳检查
        addThread("CheckJobHeartbeat", ScheduleService.RUNNING_INTERVAL, scheduleService::checkJobHeartbeat);

        // 启动所有线程
        threadContainer.forEach(Thread::start);
    }

    @Override
    public void destroy() {
        threadContainer.forEach(Thread::interrupt);
    }

    private void addThread(String name, Long runningInterval, Runnable task) {
        Thread thread = new Thread(new SafeLoopRunnable(task, runningInterval), name);
        threadContainer.add(thread);
    }

    public record SafeLoopRunnable(Runnable task, Long runningInterval) implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    long start = System.currentTimeMillis();
                    task.run();
                    long cost = System.currentTimeMillis() - start;
                    if (cost > runningInterval) {
                        log.warn("[{}] Task cost {}ms, exceed runningInterval: {}ms", Thread.currentThread().getName(), cost, runningInterval);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("[{}] Task cost {}ms, exceed runningInterval: {}ms", Thread.currentThread().getName(), cost, runningInterval);
                    }
                } catch (Exception e) {
                    // 记录异常
                    log.error("[{}] task has exception: {}", Thread.currentThread().getName(), e.getMessage(), e);
                    break;
                }
            }
        }
    }

}
