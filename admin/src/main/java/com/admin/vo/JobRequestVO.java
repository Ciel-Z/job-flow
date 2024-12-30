package com.admin.vo;

import lombok.Data;

@Data
public class JobRequestVO {

    /**
     * 任务id
     */
    private Long jobId;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * corn 表达式
     */
    private String cron;

    /**
     * 节点实现类全路径
     */
    private String processorInfo;

    /**
     * 任务参数
     */
    private String params;

    /**
     * 最大重试次数
     */
    private String maxRetryTimes;

}
