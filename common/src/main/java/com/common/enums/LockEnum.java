package com.common.enums;

public enum LockEnum {
    /**
     * 等待获取直至超出等待时间, 适用于所有任务必须有序完成
     */
    LOCK,
    /**
     * 立即返回, 适用任务有一个完成即可
     */
    TRY_LOCK;
}
