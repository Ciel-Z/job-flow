package com.common.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务流实例表
 *
 * @TableName t_job_instance
 */
@Data
public class JobInstance implements Serializable {
    /**
     * ID
     */
    private Long id;

    /**
     * 任务 ID
     */
    private Long jobId;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 任务实例 ID
     */
    private Long instanceId;

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
     * 触发时间
     */
    private LocalDateTime triggerTime;

    /**
     * 响应时间
     */
    private LocalDateTime replyTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 最后上报时间
     */
    private Long lastReportTime;

    /**
     * 结果
     */
    private String result;

    /**
     * 执行状态（0-等待Worker接收 1-运行中 2-失败 3-成功 4-暂停 5-停止中）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdDate;

    /**
     * 更新时间
     */
    private LocalDateTime updatedDate;

    /**
     * 任务信息
     */
    private JobInfo jobInfo;

    private static final long serialVersionUID = 1L;
}