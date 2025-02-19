package com.worker.handle;

import com.common.entity.JobReport;
import com.common.util.AssertUtils;
import com.worker.entity.JobContext;
import com.worker.entity.JobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SqlJobHandle implements JobHandler {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Override
    public JobReport handle(JobContext context) {
        AssertUtils.isNotNull(jdbcTemplate, "JdbcTemplate 未注入");

        String sqlCommand = context.getParams();
        // 将 sqlCommand 按照 ; 分割成多条 SQL 语句
        String[] sqlStatements = sqlCommand.split(";");
        StringBuffer result = new StringBuffer();

        try {
            // 执行每一条 SQL 语句
            for (String sql : sqlStatements) {
                // 使用 update 执行修改操作
                if (!sql.trim().isEmpty()) {
                    jdbcTemplate.update(sql.trim());
                    result.append("Executed: ").append(sql.trim()).append("\n");
                }
            }
        } catch (Exception e) {
            log.error("{} {} SQL 执行失败: {}", context.getJobName(), context.getInstanceId(), result, e);
            return JobReport.fail(result.toString());
        }

        log.info("{} {} SQL 执行结果: {}", context.getJobName(), context.getInstanceId(), result);
        return JobReport.success(result.toString());
    }
}