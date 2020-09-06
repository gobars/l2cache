package com.github.gobars.l2cache.web.controller;

import com.github.gobars.l2cache.core.listener.RedisPubSubMessage;
import com.github.gobars.l2cache.core.listener.RedisPubSubMessageType;
import com.github.gobars.l2cache.core.listener.RedisPublisher;
import com.github.gobars.l2cache.core.redis.client.RedisClient;
import com.github.gobars.l2cache.core.util.StringUtils;
import com.github.gobars.l2cache.web.service.WebStatsService;
import com.github.gobars.l2cache.web.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class CacheController {
  @Autowired private WebStatsService statsService;

  /** 缓存统计列表 */
  @RequestMapping("/cache-stats/list")
  public Result list(String redisClient, String cacheName) {
    try {
      RedisClient client = RedisController.redisClientMap.get(redisClient);
      return Result.success(statsService.listCacheStats(client, cacheName));
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return Result.error("配置redis失败" + e.getMessage());
    }
  }

  /** 重置缓存统计数据 */
  @RequestMapping("/cache-stats/reset-stats")
  public Result resetStats(String redisClient) {
    try {
      RedisClient client = RedisController.redisClientMap.get(redisClient);
      statsService.resetCacheStat(client);
      return Result.success();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return Result.error("配置redis失败" + e.getMessage());
    }
  }

  /** 删除缓存统计 */
  @RequestMapping("/cache-stats/delete-cache")
  public Result deleteCache(String redisClient, String cacheName, String internalKey, String key) {
    try {
      // 清除L1需要用到redis的订阅/发布模式，否则集群中其他服服务器节点的L1数据无法删除
      RedisPubSubMessage message = new RedisPubSubMessage();
      message.setCacheName(cacheName);
      message.setSource(RedisPubSubMessage.SOURCE);
      if (StringUtils.isBlank(key)) {
        message.setMessageType(RedisPubSubMessageType.CLEAR);
      } else {
        message.setKey(key);
        message.setMessageType(RedisPubSubMessageType.EVICT);
      }

      // 发布消息
      RedisPublisher.publisher(RedisController.redisClientMap.get(redisClient), message);

      return Result.success();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return Result.error("配置redis失败" + e.getMessage());
    }
  }
}
