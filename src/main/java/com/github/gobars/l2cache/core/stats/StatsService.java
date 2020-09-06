package com.github.gobars.l2cache.core.stats;

import com.alibaba.fastjson.JSON;
import com.github.gobars.l2cache.core.cache.Cache;
import com.github.gobars.l2cache.core.cache.L2Cache;
import com.github.gobars.l2cache.core.manager.AbstractCacheManager;
import com.github.gobars.l2cache.core.redis.client.RedisClient;
import com.github.gobars.l2cache.core.setting.L2Setting;
import com.github.gobars.l2cache.core.support.RedisLock;
import com.github.gobars.l2cache.core.util.StringUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 统计服务
 *
 * @author yuhao.wang3
 */
@Slf4j
public class StatsService {
  /** 缓存统计数据前缀 */
  public static final String PREFIX = "l2cache:stats:";

  /** 定时任务线程池 */
  private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

  /** {@link AbstractCacheManager } */
  @Setter private AbstractCacheManager cacheManager;

  /**
   * 获取缓存统计list
   *
   * @param cacheName 缓存名称
   * @return List&lt;CacheStatsInfo&gt;
   */
  public List<CacheStatsInfo> listCacheStats(String cacheName) {
    Set<String> l2CacheKeys = cacheManager.getRedisClient().scan(PREFIX + "*");
    if (CollectionUtils.isEmpty(l2CacheKeys)) {
      return Collections.emptyList();
    }

    // 遍历找出对应统计数据
    List<CacheStatsInfo> statsList = new ArrayList<>();
    for (String key : l2CacheKeys) {
      if (!StringUtils.isBlank(cacheName) && !key.startsWith(PREFIX + cacheName)) {
        continue;
      }

      CacheStatsInfo cacheStats = (CacheStatsInfo) cacheManager.getRedisClient().get(key);
      if (!Objects.isNull(cacheStats)) {
        statsList.add(cacheStats);
      }
    }

    return statsList.stream()
        .sorted(Comparator.comparing(CacheStatsInfo::getHitRate))
        .collect(Collectors.toList());
  }

  /** 同步缓存统计list */
  public void scheduleSyncCacheStats() {
    // 清空统计数据
    resetCacheStat();
    //  初始时间间隔是1分
    executor.scheduleWithFixedDelay(this::syncCacheStats, 1, 1, TimeUnit.MINUTES);
  }

  /** 关闭线程池 */
  public void shutdownExecutor() {
    executor.shutdown();
  }

  /** 重置缓存统计数据 */
  public void resetCacheStat() {
    for (String key : cacheManager.getRedisClient().scan(PREFIX + "*")) {
      resetCacheStat(key);
    }
  }

  /**
   * 重置缓存统计数据
   *
   * @param redisKey redisKey
   */
  public void resetCacheStat(String redisKey) {
    RedisClient client = cacheManager.getRedisClient();
    try {
      CacheStatsInfo cacheStats = (CacheStatsInfo) client.get(redisKey);
      if (Objects.nonNull(cacheStats)) {
        cacheStats.clear();
        // 将缓存统计数据写到redis
        client.set(redisKey, cacheStats, 24, TimeUnit.HOURS);
      }
    } catch (Exception e) {
      client.delete(redisKey);
    }
  }

  public void syncCacheStats() {
    RedisClient redisClient = cacheManager.getRedisClient();

    log.debug("执行缓存统计数据采集定时任务");
    for (AbstractCacheManager cm : AbstractCacheManager.getCacheManager()) {
      for (String cacheName : cm.getCacheNames()) {
        for (Cache cache : cm.getCache(cacheName)) {
          syncCacheStats(redisClient, cacheName, (L2Cache) cache);
        }
      }
    }
  }

  private void syncCacheStats(RedisClient redisClient, String cacheName, L2Cache l2Cache) {
    L2Setting l2Setting = l2Cache.getL2Setting();
    // 加锁并增量缓存统计数据，缓存key=固定前缀 +缓存名称加 + 内部Key
    String redisKey = PREFIX + cacheName + l2Setting.getInternalKey();
    RedisLock lock = new RedisLock(redisClient, redisKey, 60, 5000);
    try {
      if (lock.timeoutLock()) {
        updateStat(redisClient, cacheName, l2Cache, redisKey);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      lock.unlock();
    }
  }

  private void updateStat(
      RedisClient redisClient, String cacheName, L2Cache l2Cache, String redisKey) {
    CacheStatsInfo old = (CacheStatsInfo) redisClient.get(redisKey);
    if (Objects.isNull(old)) {
      old = new CacheStatsInfo();
    }

    L2Setting l2Setting = l2Cache.getL2Setting();

    // 设置缓存唯一标示
    old.setCacheName(cacheName);
    old.setInternalKey(l2Setting.getInternalKey());
    old.setDesc(l2Setting.getDesc());
    // 设置缓存配置信息
    old.setL2Setting(l2Setting);

    // 设置缓存统计数据
    CacheStats stats = l2Cache.getCacheStats();
    CacheStats cache1Stats = l2Cache.getCache1().getCacheStats();
    CacheStats cache2Stats = l2Cache.getCache2().getCacheStats();

    // 清空加载缓存时间
    cache1Stats.getAndResetCachedRequestTime();
    cache2Stats.getAndResetCachedRequestTime();

    old.setRequestCount(old.getRequestCount() + stats.getAndResetRequestCount());
    old.setMissCount(old.getMissCount() + stats.getAndResetCachedRequestCount());
    old.setTotalLoadTime(old.getTotalLoadTime() + stats.getAndResetCachedRequestTime());
    old.setHitRate(
        (old.getRequestCount() - old.getMissCount()) / (double) old.getRequestCount() * 100);

    old.setL1RequestCount(old.getL1RequestCount() + cache1Stats.getAndResetRequestCount());
    old.setL1MissCount(old.getL1MissCount() + cache1Stats.getAndResetCachedRequestCount());

    old.setL2RequestCount(old.getL2RequestCount() + cache2Stats.getAndResetRequestCount());
    old.setL2MissCount(old.getL2MissCount() + cache2Stats.getAndResetCachedRequestCount());

    // 将缓存统计数据写到redis
    redisClient.set(redisKey, old, 24, TimeUnit.HOURS);

    log.info("L2Cache统计信息：{}", JSON.toJSONString(old));
  }
}
