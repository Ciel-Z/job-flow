package com.admin.mapper;

import com.common.entity.JobInstance;
import com.common.entity.JobReport;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

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

    @Delete("delete from t_job_instance where job_id = #{jobId}")
    void deleteByJobId(Long jobId);

    void updateByEvent(JobReport jobReport);

    /**
     * 更新任务执行超时 | 心跳超时终止
     *
     * @param timestamp 时间戳
     */
    void updateRunningJobTimeOut(@Param("timestamp") LocalDateTime timestamp);
}