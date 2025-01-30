package com.common.util;

public class PathUtil {

    public static final String PATH_SEPARATOR = "@";

    /**
     * 获取全局路径 | 统一生成可调度地址的入口
     */
    public static String getGlobalPath(String address, String path) {
        return String.format("%s%s%s", address, PATH_SEPARATOR, path);
    }
}
