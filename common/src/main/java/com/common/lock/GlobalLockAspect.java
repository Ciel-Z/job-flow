package com.common.lock;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
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

    private static final DefaultParameterNameDiscoverer PARAM_DISCOVERER = new DefaultParameterNameDiscoverer();

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    // 检测锁续期线程池
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());

    private IMap<String, Long> lockMap;


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
            log.info("{} 重入锁，lockKey: {}", point.getSignature().getDeclaringTypeName(), lockKey);
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
            lockMap.unlock(lockKey);  // 释放锁
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
    private static String parseKey(ProceedingJoinPoint joinPoint, String spEl) {
        if (!spEl.startsWith("#")) {
            return spEl;
        }
        Method method = parseMethod(joinPoint);
        Object[] arguments = joinPoint.getArgs();
        String[] params = PARAM_DISCOVERER.getParameterNames(method);

        EvaluationContext context = new StandardEvaluationContext();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                context.setVariable(params[i], arguments[i]);
            }
        }
        try {
            Expression expression = PARSER.parseExpression(spEl);
            return expression.getValue(context, String.class);
        } catch (Exception e) {
            log.error("解析 {} SpEL 失败", spEl, e);
            throw e;
        }
    }


    public static Method parseMethod(ProceedingJoinPoint joinPoint) {
        Signature pointSignature = joinPoint.getSignature();
        if (!(pointSignature instanceof MethodSignature)) {
            throw new IllegalArgumentException("this annotation should be used on a method!");
        }
        MethodSignature signature = (MethodSignature) pointSignature;
        Method method = signature.getMethod();
        if (method.getDeclaringClass().isInterface()) {
            try {
                method = joinPoint.getTarget().getClass().getDeclaredMethod(pointSignature.getName(), method.getParameterTypes());
            } catch (SecurityException | NoSuchMethodException e) {
                log.error("解析方法失败", e);
            }
        }
        return method;
    }
}



