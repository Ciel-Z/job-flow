package com.common.enums;

import lombok.Getter;

/**
 * 此类为 JobStatusEnum 的中执行任务可返回的结果部分
 *
 * @see JobStatusEnum
 */
@Getter
public enum JobStatusEnum {

    DISPATCH(0, "等待Worker接收"),

    RUNNING(1, "运行中"),

    FAIL(2, "失败"),

    SUCCESS(3, "成功"),

    PAUSE(4, "暂停");


    private final int code;
    private final String description;


    JobStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public JobStatusEnum of(int code) {
        for (JobStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid JobResultEnum code: " + code);
    }

}
