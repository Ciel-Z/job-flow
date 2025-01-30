package com.admin.util;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Slf4j
public class CronUtil {

    private static final CronParser PARSER = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(com.cronutils.model.CronType.QUARTZ));

    /**
     * 获取下一次的执行时间
     *
     * @param cron
     * @return
     */
    public static Long nextTime(String cron) {
        return nextTime(cron, System.currentTimeMillis());
    }


    /**
     * 获取下一次的执行时间
     *
     * @param cron
     * @return
     */
    public static Long nextTime(String cron, Long timestamp) {
        try {
            Cron cronExpression = PARSER.parse(cron);
            ExecutionTime executionTime = ExecutionTime.forCron(cronExpression);
            Optional<ZonedDateTime> next = executionTime.nextExecution(ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()));
            return next.map(ZonedDateTime::toInstant).map(Instant::toEpochMilli).orElse(null);
        } catch (Exception e) {
            log.error("CronUtil nextTime error", e);
            return null;
        }
    }
}
