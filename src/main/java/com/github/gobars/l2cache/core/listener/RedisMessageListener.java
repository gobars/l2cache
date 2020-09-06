package com.github.gobars.l2cache.core.listener;

import com.alibaba.fastjson.JSON;
import com.github.gobars.l2cache.core.cache.Cache;
import com.github.gobars.l2cache.core.cache.L2Cache;
import com.github.gobars.l2cache.core.manager.CacheManager;
import io.lettuce.core.pubsub.RedisPubSubListener;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Collection;

/**
 * redis消息的订阅者
 *
 * @author yuhao.wang
 */
@Slf4j
public class RedisMessageListener implements RedisPubSubListener<String, String> {
  public static final String CHANNEL = "l2cache-channel";

  /** 缓存管理器 */
  @Setter private CacheManager cacheManager;

  @Override
  public void message(String channel, String message) {
    try {
      log.debug("redis消息订阅者接收到频道【{}】发布的消息。消息内容：{}", channel, message);
      val redisPubSubMessage = JSON.parseObject(message, RedisPubSubMessage.class);
      // 根据缓存名称获取多级缓存，可能有多个
      Collection<Cache> caches = cacheManager.getCache(redisPubSubMessage.getCacheName());
      for (Cache cache : caches) {
        // 判断缓存是否是多级缓存
        if (cache instanceof L2Cache) {
          switch (redisPubSubMessage.getMessageType()) {
            case EVICT:
              if (RedisPubSubMessage.SOURCE.equals(redisPubSubMessage.getSource())) {
                ((L2Cache) cache).getCache2().evict(redisPubSubMessage.getKey());
              }
              // 获取L1，并删除L1数据
              ((L2Cache) cache).getCache1().evict(redisPubSubMessage.getKey());
              log.info(
                  "删除L1{}数据,key={}",
                  redisPubSubMessage.getCacheName(),
                  redisPubSubMessage.getKey());
              break;

            case CLEAR:
              if (RedisPubSubMessage.SOURCE.equals(redisPubSubMessage.getSource())) {
                ((L2Cache) cache).getCache2().clear();
              }
              // 获取L1，并删除L1数据
              ((L2Cache) cache).getCache1().clear();
              log.info("清除L1{}数据", redisPubSubMessage.getCacheName());
              break;

            default:
              log.error("接收到没有定义的订阅消息频道数据");
              break;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("l2cache 清楚L1异常：{}", e.getMessage(), e);
    }
  }

  @Override
  public void message(String pattern, String channel, String message) {}

  @Override
  public void subscribed(String channel, long count) {}

  @Override
  public void psubscribed(String pattern, long count) {}

  @Override
  public void unsubscribed(String channel, long count) {}

  @Override
  public void punsubscribed(String pattern, long count) {}
}
