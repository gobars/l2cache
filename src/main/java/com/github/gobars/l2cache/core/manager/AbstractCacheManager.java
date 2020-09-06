package com.github.gobars.l2cache.core.manager;

import com.github.gobars.l2cache.core.cache.Cache;
import com.github.gobars.l2cache.core.listener.RedisMessageListener;
import com.github.gobars.l2cache.core.redis.client.RedisClient;
import com.github.gobars.l2cache.core.setting.L2Setting;
import com.github.gobars.l2cache.core.stats.CacheStatsInfo;
import com.github.gobars.l2cache.core.stats.StatsService;
import com.github.gobars.l2cache.core.support.ThreadPool;
import com.github.gobars.l2cache.core.util.BeanFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 公共的抽象 {@link CacheManager} 的实现.
 *
 * @author yuhao.wang3
 */
@Slf4j
public abstract class AbstractCacheManager
    implements CacheManager, InitializingBean, DisposableBean {

  /** redis pub/sub 监听器 */
  private final RedisMessageListener messageListener = new RedisMessageListener();

  /** 缓存容器 外层key是cache_name 里层key是[L1有效时间-L2有效时间-L2自动刷新时间] */
  @Getter
  private final ConcurrentMap<String, ConcurrentMap<String, Cache>> cacheContainer =
      new ConcurrentHashMap<>(16);

  /** 缓存名称容器 */
  @Getter private final Set<String> cacheNames = new LinkedHashSet<>();

  /** CacheManager 容器 */
  static Set<AbstractCacheManager> cacheManagers = new LinkedHashSet<>();

  /** 是否开启统计 */
  @Getter @Setter private boolean stats = true;

  /** redis 客户端 */
  @Getter @Setter RedisClient redisClient;

  public static Set<AbstractCacheManager> getCacheManager() {
    return cacheManagers;
  }

  @Override
  public Collection<Cache> getCache(String name) {
    ConcurrentMap<String, Cache> cacheMap = this.cacheContainer.get(name);
    if (CollectionUtils.isEmpty(cacheMap)) {
      return Collections.emptyList();
    }
    return cacheMap.values();
  }

  // Lazy cache initialization on access
  @Override
  public Cache getCache(String name, L2Setting l2Setting) {
    // 第一次获取缓存Cache，如果有直接返回,如果没有加锁往容器里里面放Cache
    ConcurrentMap<String, Cache> cacheMap = this.cacheContainer.get(name);
    if (!CollectionUtils.isEmpty(cacheMap)) {
      if (cacheMap.size() > 1) {
        log.warn("缓存名称{}存在两个不同的过期时间，请注意key唯一性，否则会出现缓存过期时间错乱的情况", name);
      }
      Cache cache = cacheMap.get(l2Setting.getInternalKey());
      if (cache != null) {
        return cache;
      }
    }

    // 第二次获取缓存Cache，加锁往容器里里面放Cache
    synchronized (this.cacheContainer) {
      cacheMap = this.cacheContainer.get(name);
      if (!CollectionUtils.isEmpty(cacheMap)) {
        // 从容器中获取缓存
        Cache cache = cacheMap.get(l2Setting.getInternalKey());
        if (cache != null) {
          return cache;
        }
      } else {
        cacheMap = new ConcurrentHashMap<>(16);
        cacheContainer.put(name, cacheMap);
        // 更新缓存名称
        updateCacheNames(name);
      }

      // 新建一个Cache对象
      Cache cache = getMissingCache(name, l2Setting);
      if (cache != null) {
        // 装饰Cache对象
        cache = decorateCache(cache);
        // 将新的Cache对象放到容器
        cacheMap.put(l2Setting.getInternalKey(), cache);
        if (cacheMap.size() > 1) {
          log.warn("缓存名称{}存在两个不同的过期时间，请注意key唯一性，否则会出现缓存过期时间错乱的情况", name);
        }
      }

      return cache;
    }
  }

  /**
   * 更新缓存名称容器
   *
   * @param name 需要添加的缓存名称
   */
  private void updateCacheNames(String name) {
    cacheNames.add(name);
  }

  /**
   * 获取Cache对象的装饰示例
   *
   * @param cache 需要添加到CacheManager的Cache实例
   * @return 装饰过后的Cache实例
   */
  protected Cache decorateCache(Cache cache) {
    return cache;
  }

  /**
   * 根据缓存名称在CacheManager中没有找到对应Cache时，通过该方法新建一个对应的Cache实例
   *
   * @param name 缓存名称
   * @param l2Setting 缓存配置
   * @return {@link Cache}
   */
  protected abstract Cache getMissingCache(String name, L2Setting l2Setting);

  @Override
  public void afterPropertiesSet() {
    messageListener.setCacheManager(this);
    // 创建监听
    redisClient.subscribe(messageListener, RedisMessageListener.CHANNEL);

    BeanFactory.getBean(StatsService.class).setCacheManager(this);
    if (isStats()) {
      // 采集缓存命中率数据
      BeanFactory.getBean(StatsService.class).scheduleSyncCacheStats();
    }
  }

  @Override
  public List<CacheStatsInfo> listCacheStats(String cacheName) {
    return BeanFactory.getBean(StatsService.class).listCacheStats(cacheName);
  }

  @Override
  public void resetCacheStat() {
    BeanFactory.getBean(StatsService.class).resetCacheStat();
  }

  @Override
  public void destroy() {
    ThreadPool.close();
    BeanFactory.getBean(StatsService.class).shutdownExecutor();
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
