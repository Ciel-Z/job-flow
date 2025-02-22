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
     * Server 节点列表
     */
    public static final String SERVER_LIST = "serverList";

    /**
     * Worker 节点列表
     */
    public static final String WORKER_LIST = "workerList";

    /**
     * 任务调度 path
     */
    public static final String DISPATCH = "worker:run:job";

    /**
     * 任务调度报告 path
     */
    public static final String DISPATCH_REPORT = "worker:job:report";

    /**
     * 任务日志 path
     */
    public static final String JOB_LOG = "job:log";

    /**
     * 任务调度停止 path
     */
    public static final String STOP_JOB = "worker:stop:job";

    /**
     * worker 任务计数器
     */
    public static final String WORKER_JOB_COUNTER = "worker_job_counter_%s";

    /**
     * 任务实例终止标识
     */
    public static final String JOB_TERMINATION = "JobTermination";

    /**
     * 工作流实例终止标识
     */
    public static final String JOB_FLOW_TERMINATION = "JobFlowTermination";

    /**
     * 工作流事件
     */
    public static final String JOB_FLOW_EVENT = "jobFlowEvent";

    /**
     * 工作流事件 websocket 地址
     */
    public static final String JOB_EVENT_WS_ADDRESS = "/ws/job/flow";
}
