package com.admin.mapper;

import com.admin.vo.JobFlowVO;
import com.common.entity.JobFlowInstance;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * @author zuobin
 * @description 针对表【t_job_flow_instance(工作流实例表)】的数据库操作Mapper
 * @createDate 2025-02-05 23:35:41
 * @Entity com.common.entity.JobFlowInstance
 */
public interface JobFlowInstanceMapper {

    int deleteByPrimaryKey(Long id);

    int insert(JobFlowInstance record);

    int insertSelective(JobFlowInstance record);

    JobFlowInstance selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(JobFlowInstance record);

    int updateByPrimaryKey(JobFlowInstance record);

    void updateVersionById(JobFlowInstance record);

    @Delete("delete from t_job_flow_instance where flow_id = #{jobFlowId}")
    void deleteByFlowId(Long jobFlowId);

    List<JobFlowInstance> selectByIds(@Param("flowIds") Set<Long> flowIds);

    /**
     * 分页查询
     */
    Page<JobFlowInstance> selectPageByCondition(JobFlowVO jobFlowVO);
}

