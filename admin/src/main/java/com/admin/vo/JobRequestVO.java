package com.admin.vo;

import lombok.Data;

@Data
public class JobRequestVO {

    /**
     * 任务id
     */
    private Long jobId;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * worker 节点 tag
     */
    private String tag;

    /**
     * corn 表达式
     */
    private String cron;

    /**
     * 节点实现类全路径
     */
    private String processorInfo;

}
