package com.worker.handle;

import com.common.entity.JobContext;
import com.common.entity.JobHandler;
import com.common.entity.JobReport;

public class SqlHandle implements JobHandler {

    @Override
    public JobReport handle(JobContext context) throws InterruptedException {
        return null;
    }

}
