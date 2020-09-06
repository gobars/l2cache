package com.github.gobars.l2cache.core.listener;

import com.alibaba.fastjson.JSON;
import com.github.gobars.l2cache.core.redis.client.RedisClient;
import lombok.extern.slf4j.Slf4j;

/**
 * redis消息的发布者
 *
 * @author yuhao.wang
 */
@Slf4j
public class RedisPublisher {
  /**
   * 发布消息到频道（Channel）
   *
   * @param redisClient redis客户端
   * @param message 消息内容
   */
  public static void publisher(RedisClient redisClient, RedisPubSubMessage message) {
    redisClient.publish(RedisMessageListener.CHANNEL, JSON.toJSONString(message));
    log.debug("redis消息发布者向频道【{}】发布了【{}】消息", RedisMessageListener.CHANNEL, message.toString());
  }
}
