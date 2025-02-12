package com.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流信息表
 *
 * @TableName t_job_flow
 */
@Data
public class JobFlow {
    /**
     * ID
     */
    private Long flowId;

    /**
     * 工作流名称
     */
    private String name;

    /**
     * DAG
     */
    private String dag;

    /**
     * 工作流级别参数
     */
    private String params;

    /**
     * 创建时间
     */
    private LocalDateTime createdDate;

    /**
     * 更新时间
     */
    private LocalDateTime updatedDate;
}