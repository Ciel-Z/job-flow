package com.admin.mapper;

import com.admin.vo.JobFlowVO;
import com.common.entity.JobFlow;
import com.github.pagehelper.Page;

/**
* @author zuobin
* @description 针对表【t_job_flow(工作流信息表)】的数据库操作Mapper
* @createDate 2025-02-05 23:35:37
* @Entity com.common.entity.JobFlow
*/
public interface JobFlowMapper {

    int deleteByPrimaryKey(Long id);

    int insert(JobFlow record);

    int insertSelective(JobFlow record);

    JobFlow selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(JobFlow record);

    int updateByPrimaryKey(JobFlow record);

    Page<JobFlow> selectPageByCondition(JobFlowVO jobFlowVO);
}
