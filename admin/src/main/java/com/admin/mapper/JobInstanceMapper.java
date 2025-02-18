package com.admin.mapper;

import com.admin.entity.TableInfo;
import com.admin.vo.JobInstanceVO;
import com.common.entity.JobInstance;
import com.common.entity.JobReport;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author zuobin
 * @description 针对表【t_job_instance(任务实例表)】的数据库操作Mapper
 * @createDate 2024-12-24 16:16:46
 * @Entity io.github.azure.common.entity.JobInstance
 */
public interface JobInstanceMapper {

    int deleteByPrimaryKey(Long id);

    int insert(JobInstance record);

    int insertSelective(JobInstance record);

    JobInstance selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(JobInstance record);

    int updateByPrimaryKey(JobInstance record);

    TableInfo<JobInstance> selectPageByCondition(JobInstanceVO requestVO);

    @Delete("delete from t_job_instance where job_id = #{jobId}")
    void deleteByJobId(Long jobId);

    /**
     * 根据任务事件更新任务状态, 任务为非终止状态时更新
     *
     * @param jobReport
     */
    void updateByEvent(JobReport jobReport);

    /**
     * 查询超时的实例
     *
     * @param timestamp 时间戳
     */
    List<JobInstance> selectRunningJobTimeout(@Param("timestamp") LocalDateTime timestamp);

    /**
     * 更新超时实例
     *
     * @param ids 实例ID
     */
    void updateTimeoutByIds(@Param("ids") List<Long> ids);

    /**
     * 更新工作流实例下任务实例被终止
     *
     * @param flowInstanceId 工作流实例ID
     */
    void updateStopByFlowInstanceId(@Param("flowInstanceId") Long flowInstanceId);
}