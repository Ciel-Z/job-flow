package com.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * Vertx configuration properties.
 */
@Order(0)
@Data
@Configuration
@ConfigurationProperties(prefix = "vertx-cluster")
public class VertxConfiguration {

    private String clusterName;

    private String tag = "default";

    private long heartbeatInterval = 30;

    private long timeout = 60;

    private String serviceName;

    private Integer port;

    private List<String> memberList;

}