package com.common.constant;

public class Constant {

    /**
     * 特性所属节点
     */
    public static final String FEATURE_HOLDER = "featureHolder";

    /**
     * 节点所属节点
     */
    public static final String NODE_HOLDER = "nodeHolder";

    /**
     * 任务调度 path
     */
    public static final String DISPATCH = "worker:run:job";

    /**
     * 任务调度报告 path
     */
    public static final String DISPATCH_REPORT = "worker:job:report";

    /**
     * 任务调度停止 path
     */
    public static final String STOP_JOB = "stop.job";

    /**
     * 任务状态机
     */
    public static final String STATE_MACHINE = "StateMachine";

    /**
     * 任务事件 websocket 地址
     */
    public static final String JOB_EVENT_WS_ADDRESS = "/ws/task";
}
