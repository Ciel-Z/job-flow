package com.common.vertx;

import com.alibaba.fastjson2.JSON;
import com.common.annotation.VerticlePath;
import com.common.entity.JobEvent;
import com.common.entity.NodeInfo;
import com.common.util.AssertUtils;
import com.common.util.PathUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public abstract class AbstractEventVerticle<T> extends AbstractVerticle{

    @Autowired
    private NodeInfo localNode;

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


    public String address() {
        VerticlePath verticlePath = getClass().getAnnotation(VerticlePath.class);
        String address = Optional.ofNullable(verticlePath).map(VerticlePath::value).orElse("");
        AssertUtils.notEmpty(address, "{} Verticle missing @VerticlePath annotation or value is empty", getClass().getName());
        return address;
    }

    public String fullAddress() {
        return PathUtil.getGlobalPath(localNode.getServerAddress(), address());
    }


    @SuppressWarnings("unchecked")
    protected Class<T> getClassType() {
        Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

        // Class
        if (type instanceof Class) {
            return (Class<T>) type;
        }

        // ParameterizedType
        if (type instanceof ParameterizedType) {
            return (Class<T>) ((ParameterizedType) type).getRawType(); // 获取原始类型 List.class
        }
        throw new IllegalArgumentException("Can't get class type");
    }
}