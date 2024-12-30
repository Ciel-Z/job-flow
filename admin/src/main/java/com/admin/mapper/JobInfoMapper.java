package com.admin.mapper;

import com.admin.vo.JobRequestVO;
import com.common.entity.JobInfo;
import com.github.pagehelper.Page;

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
}
