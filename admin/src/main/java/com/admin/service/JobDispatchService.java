package com.admin.service;


import com.admin.vo.JobInstanceVO;

public interface JobDispatchService {

    void start(JobInstanceVO jobInstanceVO);

    void stop(JobInstanceVO jobInstanceVO);

}
