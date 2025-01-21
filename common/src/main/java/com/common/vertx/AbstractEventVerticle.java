package com.common.vertx;

import com.alibaba.fastjson2.JSON;
import com.common.annotation.VerticlePath;
import com.common.entity.JobEvent;
import com.common.entity.JobReport;
import com.common.util.AssertUtils;
import com.common.util.PathUtil;
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

    public abstract void process(JobEvent<T> jobEvent);

    /**
     * Starts the verticle and initializes necessary components.
     */
    @Override
    public void start() {
        String address = address();
        String verticlePath = fullAddress();

        Handler<Message<String>> messageHandler = message -> {
            log.info("Verticle-path:{} accept: {}", verticlePath, message.body());
            T t = JSON.parseObject(message.body(), getClassType());
            // process business in a non-blocking
            process(new JobEvent<>(t, message));
        };
        consumers.add(vertx.eventBus().consumer(address, messageHandler));
        consumers.add(vertx.eventBus().consumer(verticlePath, messageHandler));
    }


    /**
     * Stops the verticle and cleans up resources.
     */
    public void destroy() {
        consumers.forEach(MessageConsumer::unregister);
    }


    protected void send(String address, JobReport jobReport) {
        vertx.eventBus().send(address, JSON.toJSONString(jobReport));
    }

    public String address() {
        VerticlePath verticlePath = getClass().getAnnotation(VerticlePath.class);
        String address = Optional.ofNullable(verticlePath).map(VerticlePath::value).orElse("");
        AssertUtils.notEmpty(address, "{} Verticle missing @VerticlePath annotation or value is empty", getClass().getName());
        return address;
    }

    public String fullAddress() {
        return PathUtil.getGlobalPath(address());
    }


    @SuppressWarnings("unchecked")
    private Class<T> getClassType() {
        return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}