package com.worker.logger;

public interface JobLogger {

    /**
     * 输出 DEBUG 类型的日志，与 Slf4j 用法一致
     *
     * @param messagePattern 日志格式，比如 [XXXService] process task(taskId={},jobId={}) failed.
     * @param args           填充 messagePattern 中 {} 的对象
     */
    void debug(String messagePattern, Object... args);

    /**
     * 输出 INFO 类型的日志，与 Slf4j 用法一致
     *
     * @param messagePattern 日志格式
     * @param args           填充对象
     */
    void info(String messagePattern, Object... args);

    /**
     * 输出 WARN 类型的日志，与 Slf4j 用法一致
     *
     * @param messagePattern 日志格式
     * @param args           填充对象
     */
    void warn(String messagePattern, Object... args);

    /**
     * 输出 ERROR 类型的日志，与 Slf4j 用法一致
     *
     * @param messagePattern 日志格式
     * @param args           填充对象
     */
    void error(String messagePattern, Object... args);

}
