package com.common.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务日志表
 * @TableName t_job_log
 */

@Data
@NoArgsConstructor
public class JobLog implements java.io.Serializable {
    /**
     * ID
     */
    private Long id;

    /**
     * 任务 ID
     */
    private Long jobId;

    /**
     * 实例 ID
     */
    private Long instanceId;

    /**
     * Worker 地址
     */
    private String workerAddress;

    /**
     * 日志时间
     */
    private LocalDateTime timestamp;

    /**
     * 日志级别
     */
    private Integer level;

    /**
     * 日志内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createdDate;

    /**
     * 更新时间
     */
    private LocalDateTime updatedDate;
}