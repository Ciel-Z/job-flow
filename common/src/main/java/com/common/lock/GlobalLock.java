package com.common.lock;

import com.common.enums.LockEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GlobalLock {

    LockEnum type() default LockEnum.LOCK;

    String key();

    /**
     * 等待时间
     */
    int waitTime() default 1000;

    /**
     * 锁持有时间
     */
    int leaseTime() default 10000;

}
