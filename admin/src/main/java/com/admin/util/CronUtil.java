package com.admin.util;

import org.quartz.CronExpression;

import java.util.Date;

public class CronUtil {


    /**
     * 获取下一次的执行时间
     *
     * @param cron
     * @return
     */
    public static Date nextTime(String cron) {
        return nextTime(cron, new Date());
    }


    /**
     * 获取下一次的执行时间
     *
     * @param cron
     * @return
     */
    public static Date nextTime(String cron, Date date) {
        try {
            CronExpression cronExpression = new CronExpression(cron);
            return cronExpression.getNextValidTimeAfter(date);
        } catch (Exception e) {
            return null;
        }
    }


}
