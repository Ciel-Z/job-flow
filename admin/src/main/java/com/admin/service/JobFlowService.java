package com.admin.service;

import com.admin.entity.TableInfo;
import com.admin.vo.JobFlowVO;
import com.common.entity.JobFlow;
import com.common.entity.JobFlowInstance;
import com.common.entity.JobReport;

public interface JobFlowService {

    void save(JobFlowVO jobFlowVO);

    void delete(Long jobFlowId);

    JobFlowVO detail(Long jobFlowId);

    TableInfo<JobFlow> list(JobFlowVO jobFlowVO);

    TableInfo<JobFlowInstance> instanceList(JobFlowVO jobFlowVO);

    JobFlowVO progress(Long flowInstanceId);

}
