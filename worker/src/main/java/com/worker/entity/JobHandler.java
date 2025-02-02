package com.worker.entity;

import com.common.entity.JobReport;

public interface JobHandler {

    JobReport handle(JobContext context) throws InterruptedException;

}
