package com.common.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ServerInfo {

    private static ConfigHolder configHolder = new ConfigHolder();

    public static void instance(String id, String ip, Integer port) {
        configHolder.id = id;
        configHolder.ip = ip;
        configHolder.port = port;
    }

    public static String getId() {
        return configHolder.id;
    }

    public static String getIp() {
        return configHolder.ip;
    }

    public static Integer getPort() {
        return configHolder.port;
    }

    public static String getServerAddress() {
        return String.format("%s:%s",  configHolder.ip, configHolder.port);
    }


    @NoArgsConstructor
    private static class ConfigHolder {
        private volatile String id;
        private volatile String ip;
        private volatile Integer port;
    }
}
