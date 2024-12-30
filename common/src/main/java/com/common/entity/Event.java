package com.common.entity;

import io.vertx.core.eventbus.Message;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Event<T> {

    private T body;

    private Message<String> message;

}
