package com.common.lock;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class GlobalLockAspect {

    //  TODO 待完成 1.切面加锁 2.锁续期 3.锁自动释放
    private final HazelcastInstance hazelcast;

    private IMap<String, LocalDateTime> map = hazelcast.<String, LocalDateTime>getMap("lock");


}
