package com.admin.service;


import com.admin.entity.TableInfo;
import com.admin.vo.JobRequestVO;
import com.common.entity.JobInfo;

public interface JobService {

    TableInfo<JobInfo> list(JobRequestVO requestVO);

    void save(JobInfo jobInfo);

    void delete(Long jobId);

    JobInfo detail(Long jobId);

}
