package com.common.util;

import com.common.entity.NodeInfo;

public class PathUtil {


    /**
     * 获取全局路径 | 统一生成可调度地址的入口
     */
    public static String getGlobalPath(String path) {
        return String.format("%s@%s", NodeInfo.getServerAddress(), path);
    }


    /**
     * 获取全局路径 | 统一生成可调度地址的入口
     */
    public static String getGlobalPath(String address, String path) {
        return String.format("%s@%s", address, path);
    }
}
