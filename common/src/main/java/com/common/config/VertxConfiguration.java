package com.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Vertx configuration properties.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "vertx-cluster")
public class VertxConfiguration {

    private String clusterName;

    private String tag = "default";

    private String serviceName;

    private Integer port;

    private List<String> memberList;

}