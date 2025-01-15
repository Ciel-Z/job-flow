package com.admin.verticle;

import com.admin.assign.Dispatch;
import com.alibaba.fastjson2.JSON;
import com.common.entity.JobInstance;
import com.common.util.AssertUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageDispatcher {

    private final Vertx vertx;

    private final List<Dispatch> dispatchList;

    private final Map<Integer, Dispatch> dispatchMap = new ConcurrentHashMap<>();


    @PostConstruct
    public void initDispatchMap() {
        // 根据 Dispatch 的 getStrategyId 方法注册到 Map 中
        for (Dispatch dispatch : dispatchList) {
            Integer strategyId = dispatch.getStrategyId();
            if (dispatchMap.containsKey(strategyId)) {
                throw new IllegalStateException("Duplicate dispatch strategy ID: " + strategyId);
            }
            dispatchMap.put(strategyId, dispatch);
        }
    }

    public Dispatcher dispatcher(String address) {
        return new Dispatcher(address);
    }


    /**
     * 提供调度和消息相关的链式封装
     */
    public class Dispatcher {
        private final String defaultAddress;
        private String currentAddress;
        private int sendTimeout;


        public Dispatcher(String defaultAddress) {
            this.defaultAddress = defaultAddress;
            this.currentAddress = defaultAddress;
            this.sendTimeout = 15000;
        }

        public Dispatcher sendTimeout(int sendTimeout) {
            this.sendTimeout = sendTimeout;
            return this;
        }

        public Dispatcher doDispatch(JobInstance instance) {
            this.currentAddress = getDispatch(instance.getDispatchStrategy()).dispatch(instance.getProcessorInfo(), instance);
            return this;
        }


        /**
         * Send a message to the address (choice one)
         *
         * @param message message body
         */
        public void send(Object message) {
            vertx.eventBus().send(defaultAddress, JSON.toJSONString(message));
        }


        /**
         * Calls a remote service using Vert.x event bus.
         *
         * @param message the message to send to the service
         * @param clazz   the class of the expected result
         * @param handler the callback function to handle the result or error
         * @param <T>     the type of the expected result
         * @throws RuntimeException if Vert.x is not initialized
         */
        public <T> void request(Object message, Class<T> clazz, BiConsumer<T, Throwable> handler) {
            DeliveryOptions options = new DeliveryOptions().setSendTimeout(this.sendTimeout);
            try {
                String body = message instanceof String && JSON.isValid((String) message) ? (String) message : JSON.toJSONString(message);
                vertx.eventBus().request(this.currentAddress, body, options, (Handler<AsyncResult<Message<String>>>) reply -> {
                    log.info("VertxFacade call reply: {}", Optional.ofNullable(reply.result()).map(Message::body).orElse(null), reply.cause());
                    T result = Optional.ofNullable(reply.result()).map(Message::body).map((response) -> parse(response, clazz)).orElse(null);
                    handler.accept(result, reply.cause());
                });
            } catch (Exception e) {
                log.error("VertxFacade call error: {}", e.getMessage());
                handler.accept(null, e);
            }
        }


        /**
         * Publish a message to the address (each listening)
         *
         * @param message 消息
         */
        public void publish(Object message) {
            vertx.eventBus().publish(this.currentAddress, JSON.toJSONString(message));
        }


        private Dispatch getDispatch(Integer dispatchStrategy) {
            AssertUtils.notEmpty(dispatchList, "No available dispatch strategy");
            return dispatchMap.get(Optional.ofNullable(dispatchStrategy).orElse(1));
        }


        private <T> T parse(String body, Class<T> clazz) {
            try {
                return JSON.parseObject(body, clazz);
            } catch (Exception e) {
                log.error("Failed to parse JSON to {}: {}", clazz.getSimpleName(), e.getMessage());
                throw new IllegalArgumentException("Invalid JSON format", e);
            }
        }
    }
}
