package com.common.entity;

import lombok.Data;

@Data
public class NodeInfo {

    /**
     * vertx 可调度的地址
     */
    private String address;

    /**
     * 节点标签 | 进行更细粒度的调度
     */
    private String tag;

    public static NodeInfo of(String address, String tag) {
        NodeInfo node = new NodeInfo();
        node.setAddress(address);
        node.setTag(tag);
        return node;
    }
}
