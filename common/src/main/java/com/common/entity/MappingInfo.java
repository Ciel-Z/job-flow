package com.common.entity;

import lombok.Data;

@Data
public class MappingInfo {

    /**
     * vertx 可调度的地址
     */
    private String address;

    /**
     * 节点标签 | 进行更细粒度的调度
     */
    private String tag;

    public static MappingInfo of(String address, String tag) {
        MappingInfo node = new MappingInfo();
        node.setAddress(address);
        node.setTag(tag);
        return node;
    }
}
