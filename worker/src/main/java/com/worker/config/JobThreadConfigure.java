package com.worker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "job-thread-pool")
public class JobThreadConfigure {

    private Integer corePoolSize = 5;

    private Integer maxPoolSize = 15;

    private Integer queueCapacity = 100;

    private Integer keepAliveSeconds = 60;

    private String threadNamePrefix = "job-thread-";

}
