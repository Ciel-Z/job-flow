package com.worker.config;

import com.worker.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WorkerScheduleManager implements InitializingBean, DisposableBean {

    private final ScheduleService scheduleService;

    private final List<Thread> threadContainer = new ArrayList<>();


    @Override
    public void afterPropertiesSet() {
        // 心跳检查
        addThread("heartbeat", ScheduleService.RUNNING_INTERVAL, scheduleService::heartbeat);

        // 启动线程
        threadContainer.forEach(Thread::start);
    }


    @Override
    public void destroy() throws Exception {
        threadContainer.forEach(Thread::interrupt);
    }


    private void addThread(String name, Long runningInterval, Runnable task) {
        Thread thread = new Thread(new SafeLoopRunnable(task, runningInterval), name);
        threadContainer.add(thread);
    }


    public record SafeLoopRunnable(Runnable task, Long runningInterval) implements Runnable {
        @Override
        public void run() {
            boolean isFirst = true;
            while (true) {
                try {
                    if (!isFirst) {
                        Thread.sleep(runningInterval);
                    }
                    task.run();
                } catch (Exception e) {
                    // 记录异常
                    log.error("[{}] task has exception: {}", Thread.currentThread().getName(), e.getMessage(), e);
                    break;
                }
                isFirst = false;
            }
        }
    }

}