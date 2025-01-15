package com.admin.service;


import com.admin.vo.JobInstanceVO;

public interface JobDispatchService {

    void start(Long jobId, String instanceParams, long delayMS);

    void stop(JobInstanceVO jobInstanceVO);

}
