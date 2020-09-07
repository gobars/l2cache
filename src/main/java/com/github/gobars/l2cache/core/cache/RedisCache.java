package com.github.gobars.l2cache.core.cache;

import com.alibaba.fastjson.JSON;
import com.github.gobars.l2cache.core.redis.client.RedisClient;
import com.github.gobars.l2cache.core.setting.C2Setting;
import com.github.gobars.l2cache.core.stats.CacheStats;
import com.github.gobars.l2cache.core.support.RedisLock;
import com.github.gobars.l2cache.core.support.ThreadAwaiter;
import com.github.gobars.l2cache.core.support.ThreadPool;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.util.CollectionUtils;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis实现的L2
 *
 * @author yuhao.wang
 */
@Slf4j
public class RedisCache extends AbstractCache {
  /** 刷新缓存等待时间，单位毫秒 */
  private static final long WAIT_TIME_MS = 500;

  /** 等待线程容器 */
  private final ThreadAwaiter container = new ThreadAwaiter();

  /** redis 客户端 */
  private final RedisClient redisClient;

  /** 缓存有效时间,秒 */
  private final long expireSecs;

  /** 缓存主动在失效前强制刷新缓存的时间 单位：秒 */
  private final long preloadSecs;

  /** 是否强制刷新（执行被缓存的方法），默认是false */
  @Getter private final boolean forceRefresh;

  /** 是否使用缓存名称作为 redis key 前缀 */
  private final boolean usePrefix;

  /**
   * @param name 缓存名称
   * @param redisClient redis客户端 redis 客户端
   * @param c2Setting L2配置{@link C2Setting}
   * @param stats 是否开启统计模式
   */
  public RedisCache(String name, RedisClient redisClient, C2Setting c2Setting, boolean stats) {
    this(
        name,
        redisClient,
        c2Setting.getExpireSecs(),
        c2Setting.getPreloadSecs(),
        c2Setting.isForceRefresh(),
        c2Setting.isUsePrefix(),
        stats);
  }

  /**
   * @param name 缓存名称
   * @param redisClient redis客户端 redis 客户端
   * @param expireSecs key的有效时间
   * @param preloadSecs 缓存主动在失效前强制刷新缓存的时间
   * @param forceRefresh 是否强制刷新（执行被缓存的方法），默认是false
   * @param usePrefix 是否使用缓存名称作为前缀
   * @param stats 是否开启统计模式
   */
  public RedisCache(
      String name,
      RedisClient redisClient,
      long expireSecs,
      long preloadSecs,
      boolean forceRefresh,
      boolean usePrefix,
      boolean stats) {
    super(stats, name);
    this.redisClient = redisClient;
    this.expireSecs = expireSecs;
    this.preloadSecs = preloadSecs;
    this.forceRefresh = forceRefresh;
    this.usePrefix = usePrefix;
  }

  @Override
  public RedisClient getNativeCache() {
    return this.redisClient;
  }

  @Override
  public Object get(String key) {
    if (isStats()) {
      getCacheStats().addRequestCount(1);
    }

    RedisCacheKey k = getRedisCacheKey(key);
    log.debug("redis缓存查询 key= {} 查询redis缓存", k.getKey());
    return redisClient.get(k.getKey());
  }

  @Override
  public <T> T get(String key, Callable<T> valueLoader) {
    if (isStats()) {
      getCacheStats().addRequestCount(1);
    }

    RedisCacheKey k = getRedisCacheKey(key);
    log.debug("redis缓存查询 key= {}", k.getKey());
    // 先获取缓存，如果有直接返回
    Object result = redisClient.get(k.getKey());
    if (result != null || redisClient.hasKey(k.getKey())) {
      refreshCache(k, valueLoader, result);
      return (T) fromStoreValue(result);
    }
    // 执行缓存方法
    return execCacheMethod(k, valueLoader);
  }

  @Override
  public void put(String key, Object value) {
    RedisCacheKey k = getRedisCacheKey(key);
    log.debug("redis缓存 key= {} put缓存，缓存值：{}", k.getKey(), JSON.toJSONString(value));
    putValue(k, value);
  }

  @Override
  public Object putIfAbsent(String key, Object value) {
    String k = getRedisCacheKey(key).getKey();
    log.debug("redis缓存 key= {} putIfAbsent缓存值：{}", k, JSON.toJSONString(value));
    Object reult = get(key);
    if (reult != null) {
      return reult;
    }

    put(key, value);
    return null;
  }

  @Override
  public void evict(String key) {
    RedisCacheKey redisCacheKey = getRedisCacheKey(key);
    log.info("清除redis缓存 key= {} ", redisCacheKey.getKey());
    redisClient.delete(redisCacheKey.getKey());
  }

  @Override
  public void clear() {
    // 必须开启了使用缓存名称作为前缀，clear才有效
    if (usePrefix) {
      log.info("清空redis缓存 ，缓存前缀为{}", getName());

      Set<String> keys = redisClient.scan(getName() + "*");
      if (!CollectionUtils.isEmpty(keys)) {
        redisClient.delete(keys);
      }
    }
  }

  /**
   * 获取 RedisCacheKey
   *
   * @param key 缓存key
   * @return RedisCacheKey
   */
  public RedisCacheKey getRedisCacheKey(String key) {
    return new RedisCacheKey(key, redisClient.getKeySerializer())
        .cacheName(getName())
        .usePrefix(usePrefix);
  }

  /** 获取锁的线程等待500ms,如果500ms都没返回，则直接释放锁放下一个请求进来，防止第一个线程异常挂掉 */
  private <T> T execCacheMethod(RedisCacheKey redisCacheKey, Callable<T> valueLoader) {
    String ck = redisCacheKey.getKey();
    val lock = new RedisLock(redisClient, ck + "_sync_lock", 10);

    while (true) {
      try {
        // 先取缓存，如果有直接返回，没有再去做拿锁操作
        Object result = redisClient.get(ck);
        if (result != null) {
          log.debug("redis缓存 key= {} 已获得锁后查询缓存命中，不需要执行被缓存的方法", ck);
          return (T) fromStoreValue(result);
        }

        // 获取分布式锁去后台查询数据
        if (lock.tryLock()) {
          T t = loadAndPutValue(redisCacheKey, valueLoader);
          log.debug("redis缓存 key= {} 获取数据完毕，唤醒等待线程", ck);
          // 唤醒线程
          container.signalAll(ck);
          return t;
        }
        // 线程等待
        log.debug("redis缓存 key= {} 未获得到锁，等待{}毫秒", ck, WAIT_TIME_MS);
        container.await(ck, WAIT_TIME_MS);
      } catch (Exception e) {
        container.signalAll(ck);
        throw new LoaderCacheValueException(ck, e);
      } finally {
        lock.unlock();
      }
    }
  }

  /** 加载并将数据放到redis缓存 */
  private <T> T loadAndPutValue(RedisCacheKey key, Callable<T> valueLoader) {
    long start = System.currentTimeMillis();
    CacheStats cacheStats = null;

    if (isStats()) {
      cacheStats = getCacheStats();
      cacheStats.addCachedRequestCount(1);
    }

    try {
      // 加载数据
      Object result = putValue(key, valueLoader.call());
      long cost = System.currentTimeMillis() - start;
      log.debug("redis缓存 key={} 执行方法, 耗时：{}, 数据:{}", key.getKey(), cost, JSON.toJSONString(result));

      if (cacheStats != null) {
        cacheStats.addCachedRequestTime(cost);
      }

      return (T) fromStoreValue(result);
    } catch (Exception e) {
      throw new LoaderCacheValueException(key.getKey(), e);
    }
  }

  private Object putValue(RedisCacheKey key, Object value) {
    Object result = toStoreValue(value);
    // redis 缓存不允许直接存NULL，如果结果返回NULL需要删除缓存
    if (result == null) {
      redisClient.delete(key.getKey());
      return result;
    }

    // 将数据放到缓存
    redisClient.set(key.getKey(), result, this.expireSecs, TimeUnit.SECONDS);
    return result;
  }

  /** 刷新缓存数据 */
  private <T> void refreshCache(
      RedisCacheKey redisCacheKey, Callable<T> valueLoader, Object result) {
    long ttl = redisClient.getExpireSecs(redisCacheKey.getKey());

    if (ttl > 0 && TimeUnit.SECONDS.toMillis(ttl) <= preloadSecs) {
      // 判断是否需要强制刷新在开启刷新线程
      if (!isForceRefresh()) {
        log.debug("redis缓存 key={} 软刷新缓存模式", redisCacheKey.getKey());
        softRefresh(redisCacheKey);
      } else {
        log.debug("redis缓存 key={} 强刷新缓存模式", redisCacheKey.getKey());
        forceRefresh(redisCacheKey, valueLoader);
      }
    }
  }

  /**
   * 软刷新，直接修改缓存时间
   *
   * @param redisCacheKey {@link RedisCacheKey}
   */
  private void softRefresh(RedisCacheKey redisCacheKey) {
    // 加一个分布式锁，只放一个请求去刷新缓存
    val l = new RedisLock(redisClient, redisCacheKey.getKey() + "_lock");
    try {
      if (l.timeoutLock()) {
        redisClient.expire(redisCacheKey.getKey(), this.expireSecs);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      l.unlock();
    }
  }

  /**
   * 硬刷新（执行被缓存的方法）
   *
   * @param redisCacheKey {@link RedisCacheKey}
   * @param valueLoader 数据加载器
   */
  private <T> void forceRefresh(RedisCacheKey redisCacheKey, Callable<T> valueLoader) {
    // 尽量少的去开启线程，因为线程池是有限的
    ThreadPool.run(
        () -> {
          // 加一个分布式锁，只放一个请求去刷新缓存
          val lock = new RedisLock(redisClient, redisCacheKey.getKey() + "_lock");
          try {
            if (lock.tryLock()) {
              // 获取锁之后再判断一下过期时间，看是否需要加载数据
              long ttl = redisClient.getExpireSecs(redisCacheKey.getKey());
              if (ttl > 0 && ttl <= preloadSecs) {
                // 加载数据并放到缓存
                loadAndPutValue(redisCacheKey, valueLoader);
              }
            }
          } catch (Exception e) {
            log.error(e.getMessage(), e);
          } finally {
            lock.unlock();
          }
        });
  }
}
