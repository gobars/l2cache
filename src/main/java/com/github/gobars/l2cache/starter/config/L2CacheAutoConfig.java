package com.github.gobars.l2cache.starter.config;

import com.github.gobars.l2cache.aspect.L2Aspect;
import com.github.gobars.l2cache.core.manager.CacheManager;
import com.github.gobars.l2cache.core.manager.L2Manager;
import com.github.gobars.l2cache.core.redis.client.RedisClientCluster;
import com.github.gobars.l2cache.core.redis.client.RedisClient;
import com.github.gobars.l2cache.core.redis.client.RedisProperties;
import com.github.gobars.l2cache.core.redis.client.RedisClientSingle;
import com.github.gobars.l2cache.core.redis.serializer.JsonRedisSerializer;
import com.github.gobars.l2cache.core.redis.serializer.StringRedisSerializer;
import com.github.gobars.l2cache.core.util.StringUtils;
import com.github.gobars.l2cache.starter.properties.L2CacheProperties;
import com.github.gobars.l2cache.starter.properties.L2CacheRedisProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 多级缓存自动配置类
 *
 * @author xiaolyuh
 */
@Configuration
@EnableAspectJAutoProxy
@EnableConfigurationProperties({L2CacheProperties.class, L2CacheRedisProperties.class})
public class L2CacheAutoConfig {
  @Bean
  @ConditionalOnMissingBean(CacheManager.class)
  public CacheManager l2CacheManager(
      RedisClient l2CacheRedisClient, L2CacheProperties l2CacheProperties) {

    L2Manager l2Manager = new L2Manager(l2CacheRedisClient);
    // 默认开启统计功能
    l2Manager.setStats(l2CacheProperties.isStats());
    return l2Manager;
  }

  @Bean
  public L2Aspect l2Aspect() {
    return new L2Aspect();
  }

  @Bean
  public RedisClient l2CacheRedisClient(L2CacheRedisProperties l2CacheRedisProperties) {
    RedisProperties redisProperties = new RedisProperties();
    redisProperties.setDatabase(l2CacheRedisProperties.getDatabase());
    redisProperties.setHost(l2CacheRedisProperties.getHost());
    redisProperties.setCluster(l2CacheRedisProperties.getCluster());
    redisProperties.setPassword(
        StringUtils.isBlank(l2CacheRedisProperties.getPassword())
            ? null
            : l2CacheRedisProperties.getPassword());
    redisProperties.setPort(l2CacheRedisProperties.getPort());

    RedisClient redisClient = new RedisClientSingle(redisProperties);
    if (!StringUtils.isBlank(redisProperties.getCluster())) {
      redisClient = new RedisClientCluster(redisProperties);
    }

    redisClient.setKeySerializer(new StringRedisSerializer());
    redisClient.setValueSerializer(new JsonRedisSerializer());
    return redisClient;
  }
}
