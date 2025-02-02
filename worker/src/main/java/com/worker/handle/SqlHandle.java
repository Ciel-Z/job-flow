package com.worker.handle;

import com.worker.entity.JobContext;
import com.worker.entity.JobHandler;
import com.common.entity.JobReport;

public class SqlHandle implements JobHandler {

    @Override
    public JobReport handle(JobContext context) throws InterruptedException {
        return null;
    }

}
