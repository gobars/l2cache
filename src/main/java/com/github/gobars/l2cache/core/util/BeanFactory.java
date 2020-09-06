package com.github.gobars.l2cache.core.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Bean 工厂类
 *
 * @author yuhao.wang3
 */
@Slf4j
public class BeanFactory {
  /** bean 容器 */
  private static final ConcurrentHashMap<Class<?>, Object> beanContainer =
      new ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  public static <T> T getBean(Class<T> aClass) {
    return (T)
        beanContainer.computeIfAbsent(
            aClass,
            aClass1 -> {
              try {
                return aClass1.newInstance();
              } catch (Exception e) {
                log.error("new instance for {} error", aClass1, e);
              }
              return null;
            });
  }
}
