package com.admin.service.impl;

import com.admin.entity.TableInfo;
import com.admin.mapper.JobInfoMapper;
import com.admin.mapper.JobInstanceMapper;
import com.admin.service.JobService;
import com.admin.vo.JobRequestVO;
import com.common.entity.JobInfo;
import com.common.vertx.MessageService;
import com.github.pagehelper.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final MessageService messageService;

    private final JobInfoMapper jobMapper;

    private final JobInstanceMapper jobInstanceMapper;

    @Override
    public TableInfo<JobInfo> list(JobRequestVO requestVO) {
        Page<JobInfo> page = jobMapper.selectPage(requestVO);
        return TableInfo.of(page.getTotal(), page.getResult());
    }

    @Override
    public void save(JobInfo jobInfo) {
        if (jobInfo.getJobId() == null) {
            jobMapper.insert(jobInfo);
        } else {
            jobMapper.updateByPrimaryKey(jobInfo);
        }
    }

    @Override
    public void delete(Long jobId) {
        jobMapper.deleteByPrimaryKey(jobId);
        jobInstanceMapper.deleteByJobId(jobId);
    }

    @Override
    public JobInfo detail(Long jobId) {
        return jobMapper.selectByPrimaryKey(jobId);
    }
}
