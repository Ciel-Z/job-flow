package com.common.entity;


import com.common.enums.JobStatusEnum;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class JobReport implements Serializable {

    private JobInstance instance;

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

    /**
     * 处理节点地址
     */
    private String workerAddress = NodeInfo.getServerAddress();

    /**
     * 时间戳
     */
    private LocalDateTime timestamp = LocalDateTime.now();


    private JobReport(String result, Integer status) {
        this.result = result;
        this.status = status;
    }

    private JobReport(String result, Integer status, Throwable throwable) {
        this(result, status);
        this.throwable = throwable;
    }

    public static JobReport success(String massage) {
        return new JobReport(massage, JobStatusEnum.SUCCESS.getCode());
    }

    public static JobReport fail(String massage) {
        return new JobReport(massage, JobStatusEnum.FAIL.getCode());
    }

    public static JobReport fail(String massage, Throwable throwable) {
        return new JobReport(massage, JobStatusEnum.FAIL.getCode(), throwable);
    }

    public static JobReport pause(String massage) {
        return new JobReport(massage, JobStatusEnum.PAUSE.getCode());
    }

    public static JobReport running(String massage) {
        return new JobReport(massage, JobStatusEnum.RUNNING.getCode());
    }

    public JobReport replenish(JobInstance instance) {
        this.instance = instance;
        return this;
    }
}