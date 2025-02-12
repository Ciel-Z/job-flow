package com.admin.service;


import com.admin.entity.TableInfo;
import com.admin.vo.JobInstanceVO;
import com.admin.vo.JobRequestVO;
import com.common.entity.JobInfo;
import com.common.entity.JobInstance;

public interface JobService {

    TableInfo<JobInfo> list(JobRequestVO requestVO);

    void save(JobInfo jobInfo);

    void delete(Long jobId);

    JobInfo detail(Long jobId);

    void toggle(Long jobId);

    TableInfo<JobInstance> instanceList(JobInstanceVO requestVO);
}
