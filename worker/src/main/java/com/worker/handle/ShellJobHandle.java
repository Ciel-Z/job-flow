package com.worker.handle;

import com.common.entity.JobReport;
import com.worker.entity.JobContext;
import com.worker.entity.JobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
@Component
public class ShellJobHandle implements JobHandler {
    @Override
    public JobReport handle(JobContext context) {
        String command = context.getParams();
        StringBuffer result = new StringBuffer();
        try {
            // 使用 ProcessBuilder 执行 shell 命令
            ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);

            // 获取输出
            String line;
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            process.waitFor();
        } catch (Exception e) {
            log.error("{} {} Shell 脚本执行失败: {}", context.getJobName(), context.getInstanceId(), result, e);
            return JobReport.fail(result.toString());
        }
        log.info("{} {} Shell 脚本执行结果: {}", context.getJobName(), context.getInstanceId(), result);
        return JobReport.success(result.toString());
    }
}
