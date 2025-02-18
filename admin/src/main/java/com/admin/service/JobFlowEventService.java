package com.admin.service;

import com.common.dag.JobFlowDAG;
import com.common.entity.JobFlowInstance;
import com.common.entity.JobInfo;
import com.common.entity.JobReport;

public interface JobFlowEventService {

    void processJobFlowEvent(JobReport jobReport);

    void startJob(JobFlowInstance instance, JobFlowDAG dag, Long nodeId, JobInfo jobInfo);

}
