package com.common.vertx;

import com.alibaba.fastjson2.JSON;
import com.common.annotation.VerticlePath;
import com.common.entity.Event;
import com.common.entity.ServerInfo;
import com.common.util.AssertUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public abstract class AbstractEventVerticle<T> extends AbstractVerticle {

    private final List<MessageConsumer<String>> consumers = new ArrayList<>();

    public abstract void process(Event<T> event);

    /**
     * Starts the verticle and initializes necessary components.
     *
     * @throws Exception if an error occurs during the start process
     */
    @Override
    public void start() throws Exception {
        String signature = signature();
        AssertUtils.notEmpty(signature, "Verticle path not be empty");
        String verticlePath = String.format("%s:%s", ServerInfo.getServerAddress(), signature);

        Handler<Message<String>> messageHandler = message -> {
            log.info("Verticle-path:{} accept: {}", verticlePath, message.body());
            T t = JSON.parseObject(message.body(), getClassType());
            // process business in a non-blocking
            process(new Event<>(t, message));
        };

        consumers.add(vertx.eventBus().consumer(signature, messageHandler));
        consumers.add(vertx.eventBus().consumer(verticlePath, messageHandler));
    }


    /**
     * Stops the verticle and cleans up resources.
     */
    public void destroy() {
        consumers.forEach(MessageConsumer::unregister);
    }


    @SuppressWarnings("unchecked")
    protected Class<T> getClassType() {
        return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    protected String signature() {
        VerticlePath verticlePath = getClass().getAnnotation(VerticlePath.class);
        return Optional.ofNullable(verticlePath).map(VerticlePath::value).orElse("");
    }

}