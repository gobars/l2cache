package com.github.gobars.l2cache.core.cache;

import com.alibaba.fastjson.JSON;
import com.github.gobars.l2cache.core.listener.RedisPubSubMessage;
import com.github.gobars.l2cache.core.listener.RedisPubSubMessageType;
import com.github.gobars.l2cache.core.listener.RedisPublisher;
import com.github.gobars.l2cache.core.redis.client.RedisClient;
import com.github.gobars.l2cache.core.setting.L2Setting;
import com.github.gobars.l2cache.core.stats.CacheStats;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

/**
 * 多级缓存
 *
 * @author yuhao.wang
 */
@Slf4j
public class L2Cache extends AbstractCache {
  /** redis 客户端 */
  private final RedisClient redisClient;

  /** L1 */
  @Getter private final Cache cache1;

  /** L2 */
  @Getter private final Cache cache2;

  /** 多级缓存配置 */
  @Getter private final L2Setting l2Setting;

  /** 是否使用L1， 默认true */
  private boolean useL1;

  /**
   * 创建一个多级缓存对象
   *
   * @param redisClient redis client
   * @param cache1 L1
   * @param cache2 L2
   * @param stats 是否开启统计
   * @param l2Setting 多级缓存配置
   */
  public L2Cache(
      RedisClient redisClient, Cache cache1, Cache cache2, boolean stats, L2Setting l2Setting) {
    this(redisClient, cache1, cache2, true, stats, cache2.getName(), l2Setting);
  }

  /**
   * @param redisClient redis client
   * @param cache1 L1
   * @param cache2 L2
   * @param useL1 是否使用L1，默认是
   * @param stats 是否开启统计，默认否
   * @param name 缓存名称
   * @param l2Setting 多级缓存配置
   */
  public L2Cache(
      RedisClient redisClient,
      Cache cache1,
      Cache cache2,
      boolean useL1,
      boolean stats,
      String name,
      L2Setting l2Setting) {
    super(stats, name);
    this.redisClient = redisClient;
    this.cache1 = cache1;
    this.cache2 = cache2;
    this.useL1 = useL1;
    this.l2Setting = l2Setting;
  }

  @Override
  public L2Cache getNativeCache() {
    return this;
  }

  @Override
  public Object get(String key) {
    Object result = null;
    if (useL1) {
      result = cache1.get(key);
      log.debug("查询L1。 key={},返回值是:{}", key, JSON.toJSONString(result));
    }
    if (result == null) {
      result = cache2.get(key);
      cache1.putIfAbsent(key, result);
      log.debug("查询L2,并将数据放到L1。 key={},返回值是:{}", key, JSON.toJSONString(result));
    }
    return fromStoreValue(result);
  }

  @Override
  public <T> T get(String key, Class<T> type) {
    if (useL1) {
      Object result = cache1.get(key, type);
      log.debug("查询L1。 key={},返回值是:{}", key, JSON.toJSONString(result));
      if (result != null) {
        return (T) fromStoreValue(result);
      }
    }

    T result = cache2.get(key, type);
    cache1.putIfAbsent(key, result);
    log.debug("查询L2,并将数据放到L1。 key={},返回值是:{}", key, JSON.toJSONString(result));
    return result;
  }

  @Override
  public <T> T get(String key, Callable<T> valueLoader) {
    if (useL1) {
      Object result = cache1.get(key);
      log.debug("查询L1。 key={},返回值是:{}", key, JSON.toJSONString(result));
      if (result != null) {
        return (T) fromStoreValue(result);
      }
    }
    T result = cache2.get(key, valueLoader);
    cache1.putIfAbsent(key, result);
    log.debug("查询L2,并将数据放到L1。 key={},返回值是:{}", key, JSON.toJSONString(result));
    return result;
  }

  @Override
  public void put(String key, Object value) {
    cache2.put(key, value);
    // 删除L1
    if (useL1) {
      deleteFirstCache(key);
    }
  }

  @Override
  public Object putIfAbsent(String key, Object value) {
    Object result = cache2.putIfAbsent(key, value);
    // 删除L1
    if (useL1) {
      deleteFirstCache(key);
    }
    return result;
  }

  @Override
  public void evict(String key) {
    // 删除的时候要先删除L2再删除L1，否则有并发问题
    cache2.evict(key);
    // 删除L1
    if (useL1) {
      deleteFirstCache(key);
    }
  }

  @Override
  public void clear() {
    // 删除的时候要先删除L2再删除L1，否则有并发问题
    cache2.clear();
    if (useL1) {
      // 清除L1需要用到redis的订阅/发布模式，否则集群中其他服服务器节点的L1数据无法删除
      RedisPubSubMessage message = new RedisPubSubMessage();
      message.setCacheName(getName());
      message.setMessageType(RedisPubSubMessageType.CLEAR);
      // 发布消息
      RedisPublisher.publisher(redisClient, message);
    }
  }

  private void deleteFirstCache(String key) {
    // 删除L1需要用到redis的Pub/Sub（订阅/发布）模式，否则集群中其他服服务器节点的L1数据无法删除
    RedisPubSubMessage message = new RedisPubSubMessage();
    message.setCacheName(getName());
    message.setKey(key);
    message.setMessageType(RedisPubSubMessageType.EVICT);
    // 发布消息
    RedisPublisher.publisher(redisClient, message);
  }

  @Override
  public CacheStats getCacheStats() {
    CacheStats cacheStats = new CacheStats();
    cacheStats.addRequestCount(cache1.getCacheStats().getRequestCount().longValue());
    cacheStats.addCachedRequestCount(cache2.getCacheStats().getCachedRequestCount().longValue());
    cacheStats.addCachedRequestTime(cache2.getCacheStats().getCachedRequestTime().longValue());

    cache1
        .getCacheStats()
        .addCachedRequestCount(cache2.getCacheStats().getRequestCount().longValue());

    setCacheStats(cacheStats);
    return cacheStats;
  }
}
