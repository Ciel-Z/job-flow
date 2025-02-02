package com.worker.logger;

import com.alibaba.fastjson2.JSON;
import com.common.constant.Constant;
import com.common.entity.JobLog;
import com.common.entity.JobLogReport;
import com.common.entity.NodeInfo;
import com.common.enums.LogLevel;
import com.hazelcast.core.HazelcastInstance;
import io.vertx.core.Vertx;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoggerHandler {

    private final Vertx vertx;

    private final NodeInfo localNode;

    private final HazelcastInstance hazelcastInstance;

    private final LinkedBlockingQueue<JobLog> logQueue = new LinkedBlockingQueue<>(102400);

    private final ReentrantLock lock = new ReentrantLock();

    private static final int BATCH_SIZE = 50;


    public void appendLog(Long jobId, Long instanceId, LogLevel level, String content) {
        JobLog jobLog = new JobLog();
        jobLog.setJobId(jobId);
        jobLog.setInstanceId(instanceId);
        jobLog.setLevel(level.getLevel());
        jobLog.setContent(content);
        jobLog.setTimestamp(LocalDateTime.now());
        logQueue.offer(jobLog);
        new Thread(this::reportLog).start();
    }


    private void reportLog() {
        boolean locked = lock.tryLock();
        if (!locked) {
            return;
        }
        if (hazelcastInstance.getMap(Constant.SERVER_LIST).isEmpty()) {
            lock.unlock();
            log.warn("No server available, skip log report");
            return;
        }
        try {
            JobLogReport jobLogReport = new JobLogReport();
            List<JobLog> logs = new LinkedList<>();
            jobLogReport.setLogs(logs);
            jobLogReport.setWorkerAddress(localNode.getServerAddress());
            while (!logQueue.isEmpty()) {
                logs.add(logQueue.poll());

                if (logs.size() >= BATCH_SIZE) {
                    vertx.eventBus().send(Constant.JOB_LOG, JSON.toJSONString(jobLogReport));
                    logs.clear();
                }
            }
            if (!logs.isEmpty()) {
                vertx.eventBus().send(Constant.JOB_LOG, JSON.toJSONString(jobLogReport));
            }
            log.info("Reported {} logs", logs.size());
        } finally {
            lock.unlock();
        }
    }
}
