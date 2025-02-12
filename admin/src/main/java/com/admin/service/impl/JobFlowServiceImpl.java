package com.admin.service.impl;

import com.admin.entity.TableInfo;
import com.admin.mapper.JobFlowInstanceMapper;
import com.admin.mapper.JobFlowMapper;
import com.admin.mapper.JobInfoMapper;
import com.admin.service.JobFlowService;
import com.admin.vo.JobFlowVO;
import com.alibaba.fastjson2.JSON;
import com.common.dag.NodeEdgeDAG;
import com.common.entity.JobFlow;
import com.common.entity.JobFlowInstance;
import com.common.util.AssertUtils;
import com.common.util.DAGUtil;
import com.github.pagehelper.Page;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobFlowServiceImpl implements JobFlowService {

    private final JobInfoMapper jobInfoMapper;

    private final JobFlowMapper jobFlowMapper;

    private final JobFlowInstanceMapper jobFlowInstanceMapper;

    @Override
    public void save(JobFlowVO jobFlowVO) {
        // Validate DAG
        boolean validate = DAGUtil.validate(jobFlowVO.getDag());
        AssertUtils.isTrue(validate, "DAG is invalid");

        // Validate Job
        Set<Long> jobIds = jobFlowVO.getDag().getNodes().stream().map(NodeEdgeDAG.Node::getJobId).collect(Collectors.toSet());
        Set<Long> existJobIds = jobInfoMapper.selectIdByIds(jobIds);
        List<Long> errorJobIds = Sets.difference(jobIds, existJobIds).stream().toList();
        AssertUtils.isTrue(errorJobIds.isEmpty(), "Job {} not exists", errorJobIds);

        // Save JobFlow
        JobFlow jobFlow = new JobFlow();
        BeanUtils.copyProperties(jobFlowVO, jobFlow);
        jobFlow.setDag(JSON.toJSONString(jobFlowVO.getDag()));
        if (jobFlowVO.getFlowId() == null) {
            jobFlowMapper.insert(jobFlow);
        } else {
            jobFlowMapper.updateByPrimaryKey(jobFlow);
        }
    }


    @Override
    public void delete(Long jobFlowId) {
        jobFlowMapper.deleteByPrimaryKey(jobFlowId);
        jobFlowInstanceMapper.deleteByFlowId(jobFlowId);
    }


    @Override
    public JobFlowVO detail(Long jobFlowId) {
        JobFlow jobFlow = jobFlowMapper.selectByPrimaryKey(jobFlowId);
        if (jobFlow == null) {
            return null;
        }
        JobFlowVO jobFlowVO = new JobFlowVO();
        BeanUtils.copyProperties(jobFlow, jobFlowVO);
        NodeEdgeDAG dag = DAGUtil.fromJSONString(jobFlow.getDag());
        jobFlowVO.setDag(dag);
        return jobFlowVO;
    }


    @Override
    public TableInfo<JobFlow> list(JobFlowVO jobFlowVO) {
        Page<JobFlow> page = jobFlowMapper.selectPageByCondition(jobFlowVO);
        return TableInfo.of(page);
    }


    @Override
    public TableInfo<JobFlowInstance> instanceList(JobFlowVO jobFlowVO) {
        Page<JobFlowInstance> page = jobFlowInstanceMapper.selectPageByCondition(jobFlowVO);
        return TableInfo.of(page);
    }


    @Override
    public JobFlowVO progress(Long flowInstanceId) {
        JobFlowInstance instance = jobFlowInstanceMapper.selectByPrimaryKey(flowInstanceId);
        if (instance == null) {
            return null;
        }
        JobFlowVO jobFlowVO = new JobFlowVO();
        BeanUtils.copyProperties(instance, jobFlowVO);
        jobFlowVO.setDag(JSON.parseObject(instance.getDag(), NodeEdgeDAG.class));
        return jobFlowVO;
    }
}