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
public class PythonJobHandle implements JobHandler {

    @Override
    public JobReport handle(JobContext context) {
        String command = context.getParams();
        StringBuffer result = new StringBuffer();
        try {
            // 设置 Python 脚本路径
            ProcessBuilder processBuilder = new ProcessBuilder("python", "-c", command);
            Process process = processBuilder.start();

            // 获取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            process.waitFor();
        } catch (Exception e) {
            log.error("{} {} Python 脚本执行失败: {}", context.getJobName(), context.getInstanceId(), result, e);
            return JobReport.fail(result.toString());
        }
        log.info("{} {} Python 脚本执行结果: {}", context.getJobName(), context.getInstanceId(), result);
        return JobReport.success(result.toString());
    }
}
