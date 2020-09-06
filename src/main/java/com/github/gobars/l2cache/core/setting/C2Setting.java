package com.github.gobars.l2cache.core.setting;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * L2配置项
 *
 * @author yuhao.wang
 */
@NoArgsConstructor
@Data
public class C2Setting {
  /** 缓存有效时间 */
  private long expireSecs = 0;

  /** 缓存主动在失效前强制刷新缓存的时间 */
  private long preloadSecs = 0;

  /** 是否强制刷新（走数据库），默认是false */
  private boolean forceRefresh = false;

  /** 是否使用缓存名称作为 redis key 前缀 */
  private boolean usePrefix = true;

  /**
   * 非空值和null值之间的时间倍率，默认是1。allowNullValue=true才有效
   *
   * <p>如配置缓存的有效时间是200秒，倍率这设置成10， 那么当缓存value为null时，缓存的有效时间将是20秒，非空时为200秒
   */
  int magnification = 1;

  /**
   * @param expireSecs 缓存有效时间
   * @param preloadSecs 缓存刷新时间
   * @param forceRefresh 是否强制刷新
   * @param magnification 非空值和null值之间的时间倍率
   */
  public C2Setting(long expireSecs, long preloadSecs, boolean forceRefresh, int magnification) {
    this.expireSecs = expireSecs;
    this.preloadSecs = preloadSecs;
    this.forceRefresh = forceRefresh;
    this.magnification = magnification;
  }
}
