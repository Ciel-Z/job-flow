package com.common.config;

import com.common.entity.NodeInfo;
import com.hazelcast.cluster.Member;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.HazelcastInstance;
import io.vertx.core.Vertx;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Order(0)
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnBean(VertxConfiguration.class)
public class VertxConfig {

    private final VertxConfiguration configuration;


    @Bean
    public HazelcastClusterManager hazelcastClusterManager() {
        Config config = new Config();
        // 集群名称
        config.setClusterName(configuration.getClusterName());

        // 端口
        NetworkConfig network = config.getNetworkConfig();

        // 集群成员
        JoinConfig join = network.getJoin();
        join.getMulticastConfig().setEnabled(false);

        // Kubernetes 环境 or 非 Kubernetes 环境
        String kubernetesNamespace = getKubernetesNamespace();
        if (!kubernetesNamespace.isEmpty()) {
            log.info("Hazelcast Running in Kubernetes, namespace: {}", kubernetesNamespace);
            join.getTcpIpConfig().setEnabled(false);
            join.getKubernetesConfig().setEnabled(true);
            join.getKubernetesConfig().setProperty("namespace", kubernetesNamespace);
            join.getKubernetesConfig().setProperty("service-name", configuration.getServiceName());
        } else {
            log.info("Hazelcast Running in Non-Kubernetes");
            network.setPort(configuration.getPort());
            join.getTcpIpConfig().setEnabled(true);
            join.getTcpIpConfig().setMembers(configuration.getMemberList());
        }
        return new HazelcastClusterManager(config);
    }


    @Bean
    public Vertx vertx(HazelcastClusterManager clusterManager) throws ExecutionException, InterruptedException {
        // 部署Vertx实例
        CompletableFuture<Vertx> future = new CompletableFuture<>();
        Vertx.builder().withClusterManager(clusterManager).buildClustered(res -> {
            if (res.succeeded()) {
                future.complete(res.result());
            }
        });
        return future.get();
    }


    @Bean
    @DependsOn("vertx")
    public HazelcastInstance hazelcast(HazelcastClusterManager clusterManager) {
        return clusterManager.getHazelcastInstance();
    }


    @Bean
    @DependsOn("hazelcast")
    public NodeInfo localNode(HazelcastInstance hazelcast) {
        Member localMember = hazelcast.getCluster().getLocalMember();
        String nodeId = localMember.getUuid().toString();
        String host = localMember.getAddress().getHost();
        int port = localMember.getAddress().getPort();
        NodeInfo nodeInfo = NodeInfo.of(nodeId, host, port);
        log.info("Vertx instance deployed successfully local IP: {}:{}", host, port);
        return nodeInfo;
    }


    private String getKubernetesNamespace() {
        try {
            String namespaceFilePath = "/var/run/secrets/kubernetes.io/serviceaccount/namespace";
            return new String(Files.readAllBytes(Paths.get(namespaceFilePath))).trim();
        } catch (Exception ignored) {
        }
        return "";
    }
}
