package com.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Data
@Configuration
@ConfigurationProperties(prefix = "vertx-cluster")
public class VertxConfiguration {

    private String clusterName;

    private Integer port;

    private String serviceName;

    private List<String> memberList;

}