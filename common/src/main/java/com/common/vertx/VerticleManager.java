package com.common.vertx;

import com.common.config.VerticleMappingManager;
import com.common.config.VertxConfiguration;
import com.common.entity.ServerInfo;
import com.hazelcast.core.HazelcastInstance;
import io.vertx.core.Vertx;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnBean(VertxConfiguration.class)
public class VerticleManager {

    private final Vertx vertx;

    private final HazelcastInstance hazelcast;

    private final Set<AbstractEventVerticle<?>> verticleSet;

    private final VerticleMappingManager verticleMappingManager;


    /**
     * Registers and deploys all AbstractVerticle beans in the application context to the Vertx instance.
     */
    @PostConstruct
    private void register() {
        for (AbstractEventVerticle<?> verticle : verticleSet) {
            // Deploy the verticle
            vertx.deployVerticle(verticle);
            // Register the mapping (ensure dispatch ability)
            verticleMappingManager.register(ServerInfo.getIp(), verticle.fullAddress());
        }
        log.info("VerticleManager Verticles deployed");
    }

    /**
     * Closes the Vertx instance when the application context is destroyed.
     */
    @PreDestroy
    public void destroy() {
        log.info("Destroying verticles");
        // Undeploy all verticles
        verticleSet.forEach(AbstractEventVerticle::destroy);
        // Shutdown the Hazelcast instance
        hazelcast.shutdown();
        // Close the Vertx instance
        vertx.close();
    }
}