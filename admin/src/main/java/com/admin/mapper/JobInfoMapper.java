package com.admin.mapper;

import com.admin.vo.JobRequestVO;
import com.common.entity.JobInfo;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * @author zuobin
 * @description 针对表【t_job_info(任务信息表)】的数据库操作Mapper
 * @createDate 2024-12-24 16:16:39
 * @Entity io.github.azure.common.entity.JobInfo
 */
public interface JobInfoMapper {

    int deleteByPrimaryKey(Long id);

    int insert(JobInfo record);

    int insertSelective(JobInfo record);

    JobInfo selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(JobInfo record);

    int updateByPrimaryKey(JobInfo record);

    Page<JobInfo> selectPage(JobRequestVO requestVO);

    List<Long> selectOverdueJob(@Param("timestamp") Long timestamp);

    void updateJobServerIp(@Param("jobIds") List<Long> jobIds, @Param("serverIp") String serverIp);

    /**
     * 查询指定服务器上的任务将执行的任务
     *
     * @param serverAddress 服务器地址
     * @param timestamp     时间戳
     * @return List<JobInfo>
     */
    List<JobInfo> selectByServerIpAndLessTimestamp(@Param("serverAddress") String serverAddress, @Param("timestamp") Long timestamp);

    @Update("update t_job_info set next_trigger_time = #{nextTriggerTime} where job_id = #{jobId}")
    void updateJobNextTriggerTime(@Param("jobId") Long jobId, @Param("nextTriggerTime") Long nextTriggerTime);
}
