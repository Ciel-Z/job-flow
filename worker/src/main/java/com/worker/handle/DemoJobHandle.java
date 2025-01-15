package com.worker.handle;

import com.common.entity.JobContext;
import com.common.entity.JobHandler;
import com.common.entity.JobReport;

public class DemoJobHandle implements JobHandler {

    @Override
    public JobReport handle(JobContext context) throws InterruptedException {
        // 模拟任务执行
        Thread.sleep(20000);
        return JobReport.success("任务执行成功");
    }

}
