package com.common.entity;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 上报任务日志
 * @description 任务日志集合
 */
@Data
public class JobLogReport {

    /**
     * Worker地址
     */
    private String workerAddress;
    
    private List<JobLog> logs;
    
    public List<JobLog> getLogs(){
        if (logs == null) {
            return Collections.emptyList();
        }
        return logs;
    }
}
