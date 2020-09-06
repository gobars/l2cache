package com.github.gobars.l2cache.core.cache;

import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.gobars.l2cache.core.setting.C1Setting;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 基于Caffeine实现的L1
 *
 * @author yuhao.wang
 */
@Slf4j
public class CaffeineCache extends AbstractCache {
  /** 缓存对象 */
  private final Cache<Object, Object> cache;

  /**
   * 使用name和{@link C1Setting}创建一个 {@link CaffeineCache} 实例
   *
   * @param name 缓存名称
   * @param c1Setting L1配置 {@link C1Setting}
   * @param stats 是否开启统计模式
   */
  public CaffeineCache(String name, C1Setting c1Setting, boolean stats) {
    super(stats, name);
    this.cache = getCache(c1Setting);
  }

  @Override
  public Cache<Object, Object> getNativeCache() {
    return this.cache;
  }

  @Override
  public Object get(String key) {
    log.debug("caffeine缓存 key={} 获取缓存", key);

    if (isStats()) {
      getCacheStats().addRequestCount(1);
    }

    if (this.cache instanceof LoadingCache) {
      return ((LoadingCache<Object, Object>) this.cache).get(key);
    }
    return cache.getIfPresent(key);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(String key, Callable<T> valueLoader) {
    log.debug("caffeine缓存 key={} 获取缓存， 如果没有命中就走库加载缓存", key);

    if (isStats()) {
      getCacheStats().addRequestCount(1);
    }

    Object result = this.cache.get(key, k -> loaderValue(key, valueLoader));
    return (T) fromStoreValue(result);
  }

  @Override
  public void put(String key, Object value) {
    log.debug("caffeine缓存 key={} put缓存，缓存值：{}", key, JSON.toJSONString(value));
    this.cache.put(key, toStoreValue(value));
  }

  @Override
  public Object putIfAbsent(String key, Object value) {
    log.debug("caffeine缓存 key={} putIfAbsent 缓存，缓存值：{}", key, JSON.toJSONString(value));
    Object result = this.cache.get(key, k -> toStoreValue(value));
    return fromStoreValue(result);
  }

  @Override
  public void evict(String key) {
    log.debug("caffeine缓存 key={} 清除缓存", key);
    this.cache.invalidate(key);
  }

  @Override
  public void clear() {
    log.debug("caffeine缓存 清空缓存");
    this.cache.invalidateAll();
  }

  /** 加载数据 */
  private <T> Object loaderValue(Object key, Callable<T> valueLoader) {
    long start = 0L;
    if (isStats()) {
      start = System.currentTimeMillis();
      getCacheStats().addCachedRequestCount(1);
    }

    try {
      T t = valueLoader.call();

      if (isStats()) {
        getCacheStats().addCachedRequestTime(System.currentTimeMillis() - start);
      }

      log.debug("caffeine缓存 key={} 从库加载缓存 value={}", key, JSON.toJSONString(t));

      return toStoreValue(t);
    } catch (Exception e) {
      throw new LoaderCacheValueException(key, e);
    }
  }

  /**
   * 根据配置获取本地缓存对象
   *
   * @param c1Setting L1配置
   * @return {@link Cache}
   */
  private static Cache<Object, Object> getCache(C1Setting c1Setting) {
    // 根据配置创建Caffeine builder
    Caffeine<Object, Object> builder = Caffeine.newBuilder();
    builder.initialCapacity(c1Setting.getInitCap());
    builder.maximumSize(c1Setting.getMaxSize());
    builder.expireAfterWrite(c1Setting.getExpireSecs(), TimeUnit.SECONDS);
    // 根据Caffeine builder创建 Cache 对象
    return builder.build();
  }
}
