package com.admin.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import com.common.entity.JobInfo;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobInstanceVO {

    /**
     * 任务实例 ID
     */
    private Long id;

    /**
     * 逻辑任务实例 ID (重试时不变化)
     */
    private Long instanceId;

    /**
     * 任务 ID
     */
    private Long jobId;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 延时时间
     */
    private Integer delayMS;

    /**
     * 开始时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 当前重试次数
     */
    private int currentRetryTimes;

    /**
     * 执行状态（0-等待Worker接收 1-运行中 2-失败 3-成功 4-暂停 5-停止中）
     */
    private Integer status;

    /**
     * 任务信息
     */
    private JobInfo jobInfo;
}
