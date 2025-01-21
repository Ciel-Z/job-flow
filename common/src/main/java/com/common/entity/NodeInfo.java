package com.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NodeInfo {

    private String id;
    private String ip;
    private Integer port;
    private LocalDateTime timestamp; // last update time

    private NodeInfo() {
    }

    private static final NodeInfo INFO = new NodeInfo();

    public static void of(String id, String ip, Integer port) {
        INFO.id = id;
        INFO.ip = ip;
        INFO.port = port;
    }

    public static NodeInfo getNowTimeStamp() {
        INFO.timestamp = LocalDateTime.now();
        return INFO;
    }


    public static NodeInfo getInfo() {
        return INFO;
    }


    public static String getServerAddress() {
        return String.format("%s:%s", INFO.ip, INFO.port);
    }

}
