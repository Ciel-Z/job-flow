package com.common.config;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;


@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@ConditionalOnBean(VertxConfiguration.class)
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

    }

    // TODO ws
//
//    private final WebSocketJobHandler webSocketJobHandler;
//
//    @Override
//    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//        registry.addHandler(webSocketJobHandler, JobConstant.JOB_EVENT_WS_ADDRESS).setAllowedOrigins("*");
//        log.info("WebSocketConfig registerWebSocketHandlers");
//    }

}
