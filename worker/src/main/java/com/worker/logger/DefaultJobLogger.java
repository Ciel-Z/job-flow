package com.worker.logger;

import com.common.entity.JobInstance;
import com.common.enums.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DefaultJobLogger extends AbstractJobLogger implements JobLogger {

    private final Logger log;

    public DefaultJobLogger(JobInstance instance, LoggerHandler loggerHandler) {
        super(instance, loggerHandler);
        this.log = LoggerFactory.getLogger(instance.getProcessorInfo());
    }

    @Override
    public void debug(String messagePattern, Object... args) {
        log.debug(messagePattern, args);
        appendLog(LogLevel.DEBUG, messagePattern, args);
    }

    @Override
    public void info(String messagePattern, Object... args) {
        log.info(messagePattern, args);
        appendLog(LogLevel.INFO, messagePattern, args);
    }

    @Override
    public void warn(String messagePattern, Object... args) {
        log.warn(messagePattern, args);
        appendLog(LogLevel.WARN, messagePattern, args);
    }

    @Override
    public void error(String messagePattern, Object... args) {
        log.error(messagePattern, args);
        appendLog(LogLevel.ERROR, messagePattern, args);
    }

}
