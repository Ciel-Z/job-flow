package com.admin.service.impl;

import com.admin.entity.TableInfo;
import com.admin.mapper.JobInfoMapper;
import com.admin.mapper.JobInstanceMapper;
import com.admin.service.JobService;
import com.admin.util.CronUtil;
import com.admin.vo.JobRequestVO;
import com.common.entity.JobInfo;
import com.github.pagehelper.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobInfoMapper jobMapper;

    private final JobInstanceMapper jobInstanceMapper;

    @Override
    public TableInfo<JobInfo> list(JobRequestVO requestVO) {
        Page<JobInfo> page = jobMapper.selectPage(requestVO);
        return TableInfo.of(page.getTotal(), page.getResult());
    }

    @Override
    public void save(JobInfo jobInfo) {
        fullDefault(jobInfo);
        if (jobInfo.getJobId() == null) {
            jobMapper.insertSelective(jobInfo);
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

    @Override
    public void toggle(Long jobId) {
        JobInfo jobInfo = jobMapper.selectByPrimaryKey(jobId);
        jobInfo.setStatus(jobInfo.getStatus() == null || jobInfo.getStatus() == 0 ? 1 : 0);
        jobMapper.updateByPrimaryKeySelective(jobInfo);
    }

    private void fullDefault(JobInfo jobInfo) {
        if (jobInfo.getStatus() == null) {
            jobInfo.setStatus(0);
        }
        if (StringUtils.hasText(jobInfo.getCron())) {
            jobInfo.setCron(jobInfo.getCron().trim());
            Long nextTriggerTime = CronUtil.nextTime(jobInfo.getCron());
            jobInfo.setNextTriggerTime(nextTriggerTime);
        }
    }
}
