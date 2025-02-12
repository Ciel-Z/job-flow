package com.common.lock;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class GlobalLockAspect implements InitializingBean {

    private final HazelcastInstance hazelcast;

    private static final ParameterNameDiscoverer PARAM_DISCOVERER = new DefaultParameterNameDiscoverer();

    private IMap<String, Long> lockMap;

    // 检测锁续期线程池
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());


    @Override
    public void afterPropertiesSet() {
        lockMap = hazelcast.getMap("lock");
    }


    @Around("@annotation(globalLock)")
    public Object execute(ProceedingJoinPoint point, GlobalLock globalLock) throws Throwable {
        String lockKey = parseKey(point, globalLock.key());
        Long holdThreadId = lockMap.get(lockKey);
        log.debug("{} 尝试获取锁，lockKey: {}", point.getSignature().getDeclaringTypeName(), lockKey);
        // 重入
        if (holdThreadId != null && holdThreadId == Thread.currentThread().getId()) {
            return point.proceed();
        }
        // 获取锁
        boolean locked = switch (globalLock.type()) {
            case LOCK -> lockMap.tryLock(lockKey, globalLock.waitTime(), globalLock.timeUnit());
            case TRY_LOCK -> lockMap.tryLock(lockKey);
        };
        // 获取锁失败
        if (!locked) {
            log.info("{} 获取锁失败，lockKey: {}", point.getSignature().getDeclaringTypeName(), lockKey);
            return null;
        }
        // 锁信息
        lockMap.put(lockKey, Thread.currentThread().getId(), globalLock.leaseTime(), globalLock.timeUnit());
        // 监控线程用于锁续期
        long renewalInterval = globalLock.leaseTime() / 2;
        ScheduledFuture<?> renewalFuture = getScheduledFuture(globalLock, lockKey, renewalInterval);
        try {
            return point.proceed();
        } finally {
            renewalFuture.cancel(true);
            lockMap.remove(lockKey);
            lockMap.unlock(globalLock.key());
            log.debug("{} 释放锁，lockKey: {}", point.getSignature().getDeclaringTypeName(), lockKey);
        }
    }

    private ScheduledFuture<?> getScheduledFuture(GlobalLock lock, String lockKey, long renewalInterval) {
        // 仅在仍为锁持有者时更新 TTL
        return scheduler.scheduleAtFixedRate(() -> {
            try {
                // 仅在仍为锁持有者时更新 TTL
                if (lockMap.containsKey(lockKey)) {
                    lockMap.setTtl(lockKey, lock.leaseTime(), TimeUnit.MILLISECONDS);
                    log.debug("续期成功，lockKey: {}", lockKey);
                }
            } catch (Exception e) {
                log.error("续期失败，lockKey: {}", lockKey, e);
            }
        }, renewalInterval, renewalInterval, TimeUnit.MILLISECONDS);
    }


    /**
     * 解析 GlobalLock 注解上配置的 key 表达式，
     * 支持使用 SpEL 表达式，例如 "#id" 或 "#user.name"
     */
    private String parseKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        ExpressionParser parser = new SpelExpressionParser();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        EvaluationContext context = new MethodBasedEvaluationContext(joinPoint.getTarget(), methodSignature.getMethod(), joinPoint.getArgs(), PARAM_DISCOVERER);
        Expression expression = parser.parseExpression(keyExpression);
        Object value = expression.getValue(context);
        return value != null ? value.toString() : keyExpression;
    }
}
