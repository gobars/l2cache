package com.github.gobars.l2cache.aspect.annotation;

import java.lang.annotation.*;

/**
 * L1配置项.
 *
 * @author yuhao.wang
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface L1 {
  /**
   * 缓存初始Size
   *
   * @return int
   */
  int initCap() default 10;

  /**
   * 缓存最大Size
   *
   * @return int
   */
  int maxSize() default 5000;

  /**
   * 缓存有效时间(秒)
   *
   * @return int
   */
  int expireSecs() default 5 * 60;
}
