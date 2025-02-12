package com.common.entity;


import com.common.enums.JobStatusEnum;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class JobReport implements Serializable {

    /**
     * 执行返回消息
     */
    private String result;

    /**
     * 执行状态（0-等待Worker接收 1-运行中 2-失败 3-成功 4-暂停 5-停止中）
     */
    private Integer status;

    /**
     * 异常信息
     */
    private Throwable throwable;

    //>>>>>>>> 以下为调度相关信息

    /**
     * 实例 ID
     */
    private Long id;

    /**
     * 任务实例 ID (宏观意义上一次调度, 重试不会变)
     */
    private Long instanceId;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 处理节点地址
     */
    private String workerAddress;

    /**
     * 工作流实例 ID
     */
    private Long flowInstanceId;

    /**
     * 工作流节点 ID
     */
    private Long flowNodeId;

    /**
     * 工作流版本
     */
    private Integer jobFlowVersion;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp = LocalDateTime.now();

    private JobReport(Integer status, String result) {
        this.result = result;
        this.status = status;
    }

    private JobReport(Integer status, String result, Throwable throwable) {
        this(status, result);
        this.throwable = throwable;
    }

    public JobReport workerAddress(String workerAddress) {
        this.workerAddress = workerAddress;
        return this;
    }

    public JobReport jobInstance(JobInstance instance) {
        this.id = instance.getId();
        this.instanceId = instance.getInstanceId();
        this.jobName = instance.getJobName();
        this.flowInstanceId = instance.getFlowInstanceId();
        this.flowNodeId = instance.getFlowNodeId();
        this.jobFlowVersion = instance.getJobFlowVersion();
        return this;
    }

    public static JobReport success(String massage) {
        return new JobReport(JobStatusEnum.SUCCESS.getCode(), massage);
    }

    public static JobReport fail(String massage) {
        return new JobReport(JobStatusEnum.FAIL.getCode(), massage);
    }

    public static JobReport fail(String massage, Throwable throwable) {
        return new JobReport(JobStatusEnum.FAIL.getCode(), massage, throwable);
    }

    public static JobReport pause(String massage) {
        return new JobReport(JobStatusEnum.PAUSE.getCode(), massage);
    }

    public static JobReport running(String massage) {
        return new JobReport(JobStatusEnum.RUNNING.getCode(), massage);
    }
}