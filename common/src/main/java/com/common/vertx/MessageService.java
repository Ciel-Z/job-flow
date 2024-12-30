package com.common.vertx;

import com.alibaba.fastjson2.JSON;
import com.common.assign.Dispatch;
import com.common.entity.JobInstance;
import com.common.util.AssertUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageService {

    private final Vertx vertx;

    private final List<Dispatch> assignmentList;

    private Dispatch getDispatch(Integer dispatchStrategy) {
        AssertUtils.notEmpty(assignmentList, "No available dispatch strategy");
        if (dispatchStrategy == null || dispatchStrategy < 0 || dispatchStrategy >= assignmentList.size()) {
            dispatchStrategy = 0;
        }
        return assignmentList.get(dispatchStrategy);
    }

    private String doDispatch(String signature, JobInstance instance) {
        return getDispatch(instance.getDispatchStrategy()).dispatch(signature, instance);
    }


    /**
     * Send a message to the specified signature (choice one)
     *
     * @param signature Verticle bus address
     * @param instance  job instance
     */
    public void send(String signature, JobInstance instance) {
        signature = doDispatch(signature, instance);
        this.send(signature, (Object) instance);
    }


    /**
     * Send a message to the specified signature (choice one)
     *
     * @param signature Verticle bus address
     * @param message   message body
     */
    public void send(String signature, Object message) {
        if (vertx == null) {
            throw new RuntimeException("Vertx is not initialized");
        }
        vertx.eventBus().send(signature, JSON.toJSONString(message));
    }


    /**
     * Calls a remote service using Vert.x event bus.
     *
     * @param signature the address of the service on the event bus
     * @param instance  job instance
     * @param clazz     the class of the expected result
     * @param consumer  the callback function to handle the result or error
     * @param <T>       the type of the expected result
     * @throws RuntimeException if Vert.x is not initialized
     */
    public <T> void call(String signature, JobInstance instance, Class<T> clazz, BiConsumer<T, Throwable> consumer) {
        signature = doDispatch(signature, instance);
        call(signature, (Object) instance, clazz, consumer);
    }


    /**
     * Calls a remote service using Vert.x event bus.
     *
     * @param signature the address of the service on the event bus
     * @param message   the message to send to the service
     * @param clazz     the class of the expected result
     * @param consumer  the callback function to handle the result or error
     * @param <T>       the type of the expected result
     * @throws RuntimeException if Vert.x is not initialized
     */
    public <T> void call(String signature, Object message, Class<T> clazz, BiConsumer<T, Throwable> consumer) {
        if (vertx == null) {
            throw new RuntimeException("Vertx is not initialized");
        }
        DeliveryOptions options = new DeliveryOptions().setSendTimeout(15000);
        vertx.eventBus().request(signature, JSON.toJSONString(message), options, (Handler<AsyncResult<Message<String>>>) reply -> {
            log.info("VertxFacade call reply: {}", Optional.ofNullable(reply.result()).map(Message::body).orElse(null));
            T result = null;
            try {
                result = JSON.parseObject(reply.result().body(), clazz);
            } catch (Exception ignored) {
            }
            consumer.accept(result, reply.cause());
        });
    }


    /**
     * Publish a message to the specified address (each listening)
     *
     * @param address 地址
     * @param message 消息
     */
    public void publish(String address, Object message) {
        if (vertx == null) {
            throw new RuntimeException("Vertx is not initialized");
        }
        vertx.eventBus().publish(address, JSON.toJSONString(message));
    }
}
