package com.common.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务实例表
 *
 * @TableName t_job_instance
 */
@Data
public class JobInstance implements Serializable, Cloneable {
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
     * 工作流实例 ID
     */
    private Long flowInstanceId;

    /**
     * 工作流节点 ID
     */
    private Long flowNodeId;

    /**
     * 调度策略 (1-轮询, 2-随机, 3-hash, 4-指定)
     */
    private Integer dispatchStrategy;

    /**
     * worker tag
     */
    private String tag;

    /**
     * 处理器信息(节点实现类全路径)
     */
    private String processorInfo;

    /**
     * 参数
     */
    private String params;

    /**
     * Worker地址
     */
    private String workerAddress;

    /**
     * 触发时间
     */
    private LocalDateTime triggerTime;

    /**
     * 最后上报时间
     */
    private LocalDateTime replyTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 结果
     */
    private String result;

    /**
     * 执行状态（0-等待Worker接收 1-运行中 2-失败 3-成功 4-暂停）
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

    //>>>> 以下为非数据库字段

    /**
     * 任务信息
     */
    private JobInfo jobInfo;

    /**
     * 当前重试次数
     */
    private Long currentRetryTimes;

    /**
     * 任务流版本
     */
    private Integer jobFlowVersion;

    @Override
    public JobInstance clone() {
        try {
            JobInstance clone = (JobInstance) super.clone();
            clone.setId(null);
            clone.setEndTime(null);
            clone.setResult(null);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;
}