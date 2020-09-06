package com.github.gobars.l2cache.core.cache;

import com.alibaba.fastjson.JSON;
import com.github.gobars.l2cache.core.stats.CacheStats;
import com.github.gobars.l2cache.core.support.NullValue;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.util.concurrent.Callable;

/**
 * Cache 接口的抽象实现类，对公共的方法做了一写实现，如是否允许存NULL值
 *
 * <p>如果允许为NULL值，则需要在内部将NULL替换成{@link NullValue#INSTANCE} 对象 *
 *
 * @author yuhao.wang3
 */
public abstract class AbstractCache implements Cache {
  /** 缓存名称 */
  @Getter private final String name;
  /** 是否开启统计功能 */
  @Getter private final boolean stats;
  /** 缓存统计类 */
  @Getter @Setter private CacheStats cacheStats = new CacheStats();

  /**
   * 通过构造方法设置缓存配置
   *
   * @param stats 是否开启监控统计
   * @param name 缓存名称
   */
  protected AbstractCache(boolean stats, String name) {
    Assert.notNull(name, "缓存名称不能为NULL");
    this.stats = stats;
    this.name = name;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(String key, Class<T> type) {
    return (T) fromStoreValue(get(key));
  }

  /**
   * Convert the given value from the internal store to a user value returned from the get method
   * (adapting {@code null}).
   *
   * @param storeValue the store value
   * @return the value to return to the user
   */
  protected Object fromStoreValue(Object storeValue) {
    return storeValue instanceof NullValue ? null : storeValue;
  }

  /**
   * Convert the given user value, as passed into the put method, to a value in the internal store
   * (adapting {@code null}).
   *
   * @param userValue the given user value
   * @return the value to store
   */
  protected Object toStoreValue(Object userValue) {
    return userValue == null ? NullValue.INSTANCE : userValue;
  }

  /** {@link #get(String, Callable)} 方法加载缓存值的包装异常 */
  public static class LoaderCacheValueException extends RuntimeException {
    @Getter private final Object key;

    public LoaderCacheValueException(Object key, Throwable ex) {
      super(String.format("加载key为 %s 的缓存数据,执行被缓存方法异常", JSON.toJSONString(key)), ex);
      this.key = key;
    }
  }
}
