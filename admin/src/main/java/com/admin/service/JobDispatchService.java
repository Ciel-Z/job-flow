package com.admin.service;


import com.admin.entity.TableInfo;
import com.admin.vo.JobInstanceVO;
import com.common.entity.JobInfo;
import com.common.entity.JobInstance;
import com.common.entity.JobLog;

public interface JobDispatchService {

    void start(Long jobId, String instanceParams, long delayMS);

    void start(JobInstance instance);

    void stop(JobInstanceVO jobInstanceVO);

    TableInfo<JobLog> log(Long instanceId);

    JobInstance instance(JobInfo jobInfo, String instanceParams);

    JobInstance instance(JobInfo jobInfo, String instanceParams, Long flowInstanceId, Long flowNodeId);

}
