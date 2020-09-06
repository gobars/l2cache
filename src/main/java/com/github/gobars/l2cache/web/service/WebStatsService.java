package com.github.gobars.l2cache.web.service;

import com.github.gobars.l2cache.core.redis.client.RedisClient;
import com.github.gobars.l2cache.core.stats.CacheStatsInfo;
import com.github.gobars.l2cache.core.stats.StatsService;
import com.github.gobars.l2cache.core.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计服务
 *
 * @author yuhao.wang3
 */
@Service
@Slf4j
public class WebStatsService {
  /**
   * 获取缓存统计list
   *
   * @param redisClient redisClient
   * @param cacheNameParam 缓存名称
   * @return List&lt;CacheStatsInfo&gt;
   */
  public List<CacheStatsInfo> listCacheStats(RedisClient redisClient, String cacheNameParam) {
    Set<String> l2CacheKeys = redisClient.scan(StatsService.PREFIX + "*");
    if (CollectionUtils.isEmpty(l2CacheKeys)) {
      return Collections.emptyList();
    }
    // 遍历找出对应统计数据
    List<CacheStatsInfo> statsList = new ArrayList<>();
    for (String key : l2CacheKeys) {
      if (!StringUtils.isBlank(cacheNameParam)
          && !key.startsWith(StatsService.PREFIX + cacheNameParam)) {
        continue;
      }

      try {
        val cacheStats = (CacheStatsInfo) redisClient.get(key);
        if (!Objects.isNull(cacheStats)) {
          statsList.add(cacheStats);
        }
      } catch (Exception e) {
      }
    }

    return statsList.stream()
        .sorted(Comparator.comparing(CacheStatsInfo::getHitRate))
        .collect(Collectors.toList());
  }

  /** 重置缓存统计数据 */
  public void resetCacheStat(RedisClient redisClient) {
    val l2CacheKeys = redisClient.scan(StatsService.PREFIX + "*");

    for (String key : l2CacheKeys) {
      resetCacheStat(redisClient, key);
    }
  }

  /**
   * 重置缓存统计数据
   *
   * @param redisKey redisKey
   */
  public void resetCacheStat(RedisClient redisClient, String redisKey) {
    try {
      val cacheStats = (CacheStatsInfo) redisClient.get(redisKey);
      if (Objects.nonNull(cacheStats)) {
        cacheStats.clear();
        redisClient.set(redisKey, cacheStats);
      }
    } catch (Exception e) {
      redisClient.delete(redisKey);
    }
  }
}
