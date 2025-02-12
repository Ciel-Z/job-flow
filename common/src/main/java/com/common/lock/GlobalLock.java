package com.common.lock;

import com.common.enums.LockEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁, 基于 Hazelcast; 通过 AOP 方式实现;
 * @see <code>com.common.lock.GlobalLockAspect</code>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GlobalLock {

    LockEnum type() default LockEnum.LOCK;

    String key();

    /**
     * 等待时间
     */
    long waitTime() default 1000;

    /**
     * 锁持有时间
     */
    long leaseTime() default 10000;


    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

}
