package com.admin.mapper;

import com.common.entity.JobLog;
import com.common.entity.JobLogReport;
import org.apache.ibatis.annotations.Param;

/**
* @author zuobin
* @description 针对表【t_job_log(任务日志表)】的数据库操作Mapper
* @createDate 2025-02-02 14:46:00
* @Entity com.common.entity.JobLog
*/
public interface JobLogMapper {

    int deleteByPrimaryKey(Long id);

    int insert(JobLog record);

    int insertSelective(JobLog record);

    JobLog selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(JobLog record);

    int updateByPrimaryKey(JobLog record);

    void batchInsert(@Param("jobLogReport") JobLogReport jobLogReport);
}
