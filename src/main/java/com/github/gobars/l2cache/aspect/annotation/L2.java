package com.github.gobars.l2cache.aspect.annotation;

import java.lang.annotation.*;

/**
 * L2配置项
 *
 * @author yuhao.wang
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface L2 {
  /**
   * 缓存有效时间(默认5小时).
   *
   * @return long
   */
  long expireSecs() default 5 * 60 * 60;

  /**
   * 缓存主动在失效前强制刷新缓存的时间
   *
   * <p>建议是： preloadSecs = expireSecs * 0.2
   *
   * @return long
   */
  long preloadSecs() default 60;

  /**
   * 是否强制刷新（直接执行被缓存方法），默认是false
   *
   * @return boolean
   */
  boolean forceRefresh() default false;
}
