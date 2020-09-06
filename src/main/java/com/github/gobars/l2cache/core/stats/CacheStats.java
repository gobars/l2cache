package com.github.gobars.l2cache.core.stats;

import lombok.Getter;

import java.util.concurrent.atomic.LongAdder;

/**
 * 缓存统计信息实体类
 *
 * @author yuhao.wang3
 */
public final class CacheStats {
  /** 请求缓存总数 */
  @Getter private final LongAdder requestCount = new LongAdder();

  /** 请求被缓存方法总数 */
  @Getter private final LongAdder cachedRequestCount = new LongAdder();

  /** 请求被缓存方法总耗时(毫秒) */
  @Getter private final LongAdder cachedRequestTime = new LongAdder();

  /**
   * 自增请求缓存总数
   *
   * @param add 自增数量
   */
  public void addRequestCount(long add) {
    requestCount.add(add);
  }

  /**
   * 自增请求被缓存方法总数
   *
   * @param add 自增数量
   */
  public void addCachedRequestCount(long add) {
    cachedRequestCount.add(add);
  }

  /**
   * 自增请求被缓存方法总耗时(毫秒)
   *
   * @param time 自增数量
   */
  public void addCachedRequestTime(long time) {
    cachedRequestTime.add(time);
  }

  public long getAndResetRequestCount() {
    long lodValue = requestCount.longValue();
    requestCount.reset();
    return lodValue;
  }

  public long getAndResetCachedRequestCount() {
    long lodValue = cachedRequestCount.longValue();
    cachedRequestCount.reset();
    return lodValue;
  }

  public long getAndResetCachedRequestTime() {
    long lodValue = cachedRequestTime.longValue();
    cachedRequestTime.reset();
    return lodValue;
  }
}
