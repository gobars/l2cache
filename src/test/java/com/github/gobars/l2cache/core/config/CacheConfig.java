package com.github.gobars.l2cache.core.config;

import com.github.gobars.l2cache.core.manager.CacheManager;
import com.github.gobars.l2cache.core.manager.L2Manager;
import com.github.gobars.l2cache.core.redis.client.RedisClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({RedisConfig.class})
public class CacheConfig {
  @Bean
  public CacheManager l2CacheManager(RedisClient l2CacheRedisClient) {
    L2Manager l2Manager = new L2Manager(l2CacheRedisClient);
    // 开启统计功能
    l2Manager.setStats(true);
    return l2Manager;
  }
}
