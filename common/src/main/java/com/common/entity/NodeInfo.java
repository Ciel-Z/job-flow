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

    public static NodeInfo of(String id, String ip, Integer port) {
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.id = id;
        nodeInfo.ip = ip;
        nodeInfo.port = port;
        return nodeInfo;
    }

    public NodeInfo updateTimeStamp() {
        this.timestamp = LocalDateTime.now();
        return this;
    }

    public  String getServerAddress() {
        return String.format("%s:%s", ip, port);
    }

}
