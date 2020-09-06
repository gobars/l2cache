package com.github.gobars.l2cache.aspect.config;

import com.github.gobars.l2cache.aspect.L2Aspect;
import com.github.gobars.l2cache.aspect.test.TestService;
import com.github.gobars.l2cache.core.manager.CacheManager;
import com.github.gobars.l2cache.core.manager.L2Manager;
import com.github.gobars.l2cache.core.redis.client.RedisClient;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

@Configuration
@Import({RedisConfig.class})
@EnableAspectJAutoProxy
public class CacheConfig {

  @Bean
  public CacheManager l2CacheManager(RedisClient l2CacheRedisClient) {
    val cm = new L2Manager(l2CacheRedisClient);
    cm.setStats(true);
    return cm;
  }

  @Bean
  public L2Aspect l2Aspect() {
    return new L2Aspect();
  }

  @Bean
  public TestService testService() {
    return new TestService();
  }
}
