package com.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public enum LogLevel {

    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3),
    OFF(4);
    private final int level;

    private static final Map<Integer, String> levelMap = Map.of(0, "DEBUG", 1, "INFO", 2, "WARN", 3, "ERROR", 4, "OFF");
    public String of(int level) {
        return levelMap.get(level);
    }
}
