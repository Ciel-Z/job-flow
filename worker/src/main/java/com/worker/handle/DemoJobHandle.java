package com.worker.handle;

import com.worker.entity.JobContext;
import com.worker.entity.JobHandler;
import com.common.entity.JobReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DemoJobHandle implements JobHandler {

    @Override
    public JobReport handle(JobContext context) throws InterruptedException {
        // 模拟任务执行
        Thread.sleep(60000);
        context.getLogger().error("测试 log {}", 1, new RuntimeException("测试异常"));
        return JobReport.success("任务执行成功");
    }

}
