package com.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobInfo implements java.io.Serializable {
    /**
     * 任务ID
     */
    private Long jobId;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * cron表达式
     */
    private String cron;

    /**
     * 处理器信息(节点实现类全路径)
     */
    private String processorInfo;

    /**
     * 调度测量 (0-轮询, 1-发布, 3-随机, 4-哈希, 5-指定)
     */
    private Integer dispatchStrategy;

    /**
     * 额外信息
     */
    private String extra;

    /**
     * 参数
     */
    private String params;

    /**
     * 下次触发时间
     */
    private Long nextTriggerTime;

    /**
     * 最大重试次数
     */
    private Integer maxRetryTimes;

    /**
     * 重试间隔
     */
    private Integer retryInterval;

    /**
     * 创建时间
     */
    private LocalDateTime createdDate;

    /**
     * 更新时间
     */
    private LocalDateTime updatedDate;

    private static final long serialVersionUID = 1L;
}
