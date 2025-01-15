package com.common.entity;

import lombok.Data;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 任务信息表
 * @TableName t_job_info
 */
@Data
public class JobInfo implements java.io.Serializable {
    /**
     * ID
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
     * 调度策略 (1-轮询, 2-随机, 3-hash, 4-指定)
     */
    private Integer dispatchStrategy;

    /**
     * 额外信息
     */
    private String extra;

    /**
     * worker tag
     */
    private String tag;

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
     * 重试间隔(毫秒)
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

    @Serial
    private static final long serialVersionUID = 1L;
}