package com.github.gobars.l2cache.aspect;

import com.github.gobars.l2cache.aspect.annotation.*;
import com.github.gobars.l2cache.aspect.expression.CacheOperationExpressionEvaluator;
import com.github.gobars.l2cache.aspect.support.Invoker;
import com.github.gobars.l2cache.aspect.support.KeyGenerator;
import com.github.gobars.l2cache.aspect.support.SimpleKeyGenerator;
import com.github.gobars.l2cache.core.cache.Cache;
import com.github.gobars.l2cache.core.manager.CacheManager;
import com.github.gobars.l2cache.core.redis.serializer.SerializationException;
import com.github.gobars.l2cache.core.setting.C1Setting;
import com.github.gobars.l2cache.core.setting.C2Setting;
import com.github.gobars.l2cache.core.setting.L2Setting;
import com.github.gobars.l2cache.core.util.ToStringUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;

/**
 * 缓存拦截，用于注册方法信息
 *
 * @author yuhao.wang
 */
@Aspect
@Slf4j
public class L2Aspect {
  private static final String CACHE_KEY_ERROR_MESSAGE = "缓存Key %s 不能为NULL";
  private static final String CACHE_NAME_ERROR_MESSAGE = "缓存名称不能为NULL";

  /** SpEL表达式计算器 */
  private final CacheOperationExpressionEvaluator evaluator =
      new CacheOperationExpressionEvaluator();

  @Autowired private CacheManager cacheManager;

  @Autowired(required = false)
  private KeyGenerator keyGenerator = new SimpleKeyGenerator();

  @Pointcut("@annotation(com.github.gobars.l2cache.aspect.annotation.Cacheable)")
  public void cacheablePointcut() {}

  @Pointcut("@annotation(com.github.gobars.l2cache.aspect.annotation.CacheEvict)")
  public void cacheEvictPointcut() {}

  @Pointcut("@annotation(com.github.gobars.l2cache.aspect.annotation.CachePut)")
  public void cachePutPointcut() {}

  @Around("cacheablePointcut()")
  public Object cacheablePointcut(ProceedingJoinPoint jp) {
    val invoker = createInvoker(jp);

    Method m = this.getSpecificMethod(jp);
    Cacheable c = AnnotationUtils.findAnnotation(m, Cacheable.class);

    try {
      // 执行查询缓存方法
      return cache(invoker, c, m, jp.getArgs(), jp.getTarget());
    } catch (SerializationException e) {
      // 如果是序列化异常需要先删除原有缓存,在执行缓存方法
      String[] cacheNames = c.names();
      delete(cacheNames, c.key(), m, jp.getArgs(), jp.getTarget());
      try {
        return cache(invoker, c, m, jp.getArgs(), jp.getTarget());
      } catch (Exception exception) {
        // 忽略操作缓存过程中遇到的异常
        if (c.ignoreException()) {
          log.warn(e.getMessage(), e);
          return invoker.invoke();
        }
        throw e;
      }
    } catch (Exception e) {
      // 忽略操作缓存过程中遇到的异常
      if (c.ignoreException()) {
        log.warn(e.getMessage(), e);
        return invoker.invoke();
      }
      throw e;
    }
  }

  @Around("cacheEvictPointcut()")
  public Object cacheEvictPointcut(ProceedingJoinPoint joinPoint) {
    Invoker aopAllianceInvoker = createInvoker(joinPoint);
    Method method = this.getSpecificMethod(joinPoint);
    CacheEvict cacheEvict = AnnotationUtils.findAnnotation(method, CacheEvict.class);

    try {
      // 执行查询缓存方法
      return executeEvict(
          aopAllianceInvoker, cacheEvict, method, joinPoint.getArgs(), joinPoint.getTarget());
    } catch (Exception e) {
      // 忽略操作缓存过程中遇到的异常
      if (cacheEvict.ignoreException()) {
        log.warn(e.getMessage(), e);
        return aopAllianceInvoker.invoke();
      }
      throw e;
    }
  }

  @Around("cachePutPointcut()")
  public Object cachePutPointcut(ProceedingJoinPoint joinPoint) {
    Invoker invoker = createInvoker(joinPoint);

    Method method = this.getSpecificMethod(joinPoint);
    CachePut cp = AnnotationUtils.findAnnotation(method, CachePut.class);

    try {
      // 执行查询缓存方法
      return put(invoker, cp, method, joinPoint.getArgs(), joinPoint.getTarget());
    } catch (Exception e) {
      // 忽略操作缓存过程中遇到的异常
      if (cp.ignoreException()) {
        log.warn(e.getMessage(), e);
        return invoker.invoke();
      }
      throw e;
    }
  }

  /**
   * 执行Cacheable切面
   *
   * @param invoker 缓存注解的回调方法
   * @param cacheable {@link Cacheable}
   * @param method {@link Method}
   * @param args 注解方法参数
   * @param target target
   * @return {@link Object}
   */
  private Object cache(
      Invoker invoker, Cacheable cacheable, Method method, Object[] args, Object target) {

    // 解析SpEL表达式获取cacheName和key
    String[] cacheNames = cacheable.names();
    Assert.notEmpty(cacheable.names(), CACHE_NAME_ERROR_MESSAGE);
    String cacheName = cacheNames[0];
    Object key = generateKey(cacheable.key(), method, args, target);
    Assert.notNull(key, String.format(CACHE_KEY_ERROR_MESSAGE, cacheable.key()));

    // 从注解中获取缓存配置
    L1 l1 = cacheable.l1();
    L2 l2 = cacheable.l2();
    val setting1 = new C1Setting(l1.initCap(), l1.maxSize(), l1.expireSecs());
    val setting2 = new C2Setting(l2.expireSecs(), l2.preloadSecs(), l2.forceRefresh());

    L2Setting l2Setting = new L2Setting(setting1, setting2, cacheable.desc());

    // 通过cacheName和缓存配置获取Cache
    Cache cache = cacheManager.getCache(cacheName, l2Setting);

    // 通Cache获取值
    return cache.get(ToStringUtils.toString(key), invoker::invoke);
  }

  /**
   * 执行 CacheEvict 切面
   *
   * @param invoker 缓存注解的回调方法
   * @param cacheEvict {@link CacheEvict}
   * @param method {@link Method}
   * @param args 注解方法参数
   * @param target target
   * @return {@link Object}
   */
  private Object executeEvict(
      Invoker invoker, CacheEvict cacheEvict, Method method, Object[] args, Object target) {
    // 执行删除方法
    Object result = invoker.invoke();

    // 删除缓存
    // 解析SpEL表达式获取cacheName和key
    String[] cacheNames = cacheEvict.names();
    // 判断是否删除所有缓存数据
    if (cacheEvict.allEntries()) {
      // 删除所有缓存数据（清空）
      for (String cacheName : cacheNames) {
        Collection<Cache> caches = cacheManager.getCache(cacheName);
        if (CollectionUtils.isEmpty(caches)) {
          // 如果没有找到Cache就新建一个默认的
          Cache cache =
              cacheManager.getCache(
                  cacheName, new L2Setting(new C1Setting(), new C2Setting(), "默认缓存配置（清除时生成）"));
          cache.clear();
        } else {
          for (Cache cache : caches) {
            cache.clear();
          }
        }
      }
    } else {
      // 删除指定key
      delete(cacheNames, cacheEvict.key(), method, args, target);
    }

    return result;
  }

  /**
   * 删除执行缓存名称上的指定key
   *
   * @param cacheNames 缓存名称
   * @param keySpEL key的SpEL表达式
   * @param method {@link Method}
   * @param args 参数列表
   * @param target 目标类
   */
  private void delete(
      String[] cacheNames, String keySpEL, Method method, Object[] args, Object target) {
    Object key = generateKey(keySpEL, method, args, target);
    Assert.notNull(key, String.format(CACHE_KEY_ERROR_MESSAGE, keySpEL));
    for (String cacheName : cacheNames) {
      Collection<Cache> caches = cacheManager.getCache(cacheName);
      if (CollectionUtils.isEmpty(caches)) {
        // 如果没有找到Cache就新建一个默认的
        Cache cache =
            cacheManager.getCache(
                cacheName, new L2Setting(new C1Setting(), new C2Setting(), "默认缓存配置（删除时生成）"));
        cache.evict(ToStringUtils.toString(key));
      } else {
        for (Cache cache : caches) {
          cache.evict(ToStringUtils.toString(key));
        }
      }
    }
  }

  /**
   * 执行 CachePut 切面
   *
   * @param invoker 缓存注解的回调方法
   * @param cachePut {@link CachePut}
   * @param method {@link Method}
   * @param args 注解方法参数
   * @param target target
   * @return {@link Object}
   */
  private Object put(
      Invoker invoker, CachePut cachePut, Method method, Object[] args, Object target) {
    String[] cacheNames = cachePut.names();
    // 解析SpEL表达式获取 key
    Object key = generateKey(cachePut.key(), method, args, target);
    Assert.notNull(key, String.format(CACHE_KEY_ERROR_MESSAGE, cachePut.key()));

    // 从解决中获取缓存配置
    L1 l1 = cachePut.firstCache();
    L2 l2 = cachePut.secondaryCache();
    val c1Setting = new C1Setting(l1.initCap(), l1.maxSize(), l1.expireSecs());
    val c2Setting = new C2Setting(l2.expireSecs(), l2.preloadSecs(), l2.forceRefresh());

    L2Setting l2Setting = new L2Setting(c1Setting, c2Setting, cachePut.desc());

    // 指定调用方法获取缓存值
    Object result = invoker.invoke();

    for (String cacheName : cacheNames) {
      // 通过cacheName和缓存配置获取Cache
      Cache cache = cacheManager.getCache(cacheName, l2Setting);
      cache.put(ToStringUtils.toString(key), result);
    }

    return result;
  }

  private Invoker createInvoker(ProceedingJoinPoint joinPoint) {
    return () -> {
      try {
        return joinPoint.proceed();
      } catch (Throwable ex) {
        throw new Invoker.InvokeException(ex);
      }
    };
  }

  /**
   * 解析SpEL表达式，获取注解上的key属性值
   *
   * @return Object
   */
  private Object generateKey(String keySpEl, Method method, Object[] args, Object target) {
    // 获取注解上的key属性值
    Class<?> targetClass = getTargetClass(target);
    if (StringUtils.hasText(keySpEl)) {
      EvaluationContext evaluationContext =
          evaluator.createEvaluationContext(
              method, args, target, targetClass, CacheOperationExpressionEvaluator.NO_RESULT);

      AnnotatedElementKey methodCacheKey = new AnnotatedElementKey(method, targetClass);
      // 兼容传null值得情况
      Object keyValue = evaluator.key(keySpEl, methodCacheKey, evaluationContext);
      return Objects.isNull(keyValue) ? "null" : keyValue;
    }
    return this.keyGenerator.generate(target, method, args);
  }

  /**
   * 获取类信息
   *
   * @param target Object
   * @return targetClass
   */
  private Class<?> getTargetClass(Object target) {
    return AopProxyUtils.ultimateTargetClass(target);
  }

  /**
   * 获取Method
   *
   * @param pjp ProceedingJoinPoint
   * @return {@link Method}
   */
  private Method getSpecificMethod(ProceedingJoinPoint pjp) {
    MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
    Method method = methodSignature.getMethod();
    // The method may be on an interface, but we need attributes from the
    // target class. If the target class is null, the method will be
    // unchanged.
    Class<?> targetClass = AopProxyUtils.ultimateTargetClass(pjp.getTarget());
    Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
    // If we are dealing with method with generic parameters, find the
    // original method.
    return BridgeMethodResolver.findBridgedMethod(specificMethod);
  }
}
