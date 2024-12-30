package com.common.entity;


import com.common.enums.JobStatusEnum;
import lombok.Data;

import java.io.Serializable;

@Data
public class JobReport implements Serializable {

    /**
     * 执行返回消息
     */
    private String massage;

    /**
     * 执行状态
     */
    private Integer status;

    /**
     * 异常信息
     */
    private Throwable throwable;

    /**
     * 处理节点地址
     */
    private String workerAddress;


    private JobReport() {
        this.workerAddress = ServerInfo.getServerAddress();
    }

    public JobReport(String massage, Integer status) {
        this();
        this.massage = massage;
        this.status = status;
    }

    public JobReport(String massage, Integer status, Throwable throwable) {
        this(massage, status);
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
}