package com.worker.logger;

import com.common.entity.JobInstance;
import com.common.enums.LogLevel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

@Slf4j
public abstract class AbstractJobLogger implements JobLogger {

    protected final JobInstance instance;
    protected final LoggerHandler loggerHandler;

    public AbstractJobLogger(JobInstance instance, LoggerHandler loggerHandler) {
        this.instance = instance;
        this.loggerHandler = loggerHandler;
    }

    protected void appendLog(LogLevel level, String messagePattern, Object... args) {
        loggerHandler.appendLog(instance.getJobId(), instance.getInstanceId(), level, formatMessage(messagePattern, args));
    }

    private String formatMessage(String messagePattern, Object... args) {
        FormattingTuple formattingTuple = MessageFormatter.arrayFormat(messagePattern, args);
        if (formattingTuple.getThrowable() != null) {
            String stackTrace = ExceptionUtils.getStackTrace(formattingTuple.getThrowable());
            return formattingTuple.getMessage() + System.lineSeparator() + stackTrace;
        } else {
            return formattingTuple.getMessage();
        }
    }
}
