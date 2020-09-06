package com.github.gobars.l2cache.aspect.config;

import com.github.gobars.l2cache.core.redis.client.RedisClient;
import com.github.gobars.l2cache.core.redis.client.RedisProperties;
import com.github.gobars.l2cache.core.redis.client.RedisClientSingle;
import com.github.gobars.l2cache.core.redis.serializer.JsonRedisSerializer;
import com.github.gobars.l2cache.core.redis.serializer.StringRedisSerializer;
import com.github.gobars.l2cache.core.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource({"classpath:application.properties"})
public class RedisConfig {

  @Value("${spring.redis.database:0}")
  private int database;

  @Value("${spring.redis.host:127.0.0.1}")
  private String host;

  @Value("${spring.redis.password:}")
  private String password;

  @Value("${spring.redis.port:6378}")
  private int port;

  @Value("${spring.redis.pool.max-idle:200}")
  private int maxIdle;

  @Value("${spring.redis.pool.min-idle:10}")
  private int minIdle;

  @Value("${spring.redis.pool.max-active:80}")
  private int maxActive;

  @Value("${spring.redis.pool.max-wait:-1}")
  private long maxWait;

  @Bean
  public RedisClient l2CacheRedisClient() {
    RedisProperties redisProperties = new RedisProperties();
    redisProperties.setDatabase(database);
    redisProperties.setHost(host);
    redisProperties.setPassword(StringUtils.isBlank(password) ? null : password);
    redisProperties.setPort(port);

    RedisClientSingle redisClient = new RedisClientSingle(redisProperties);
    redisClient.setKeySerializer(new StringRedisSerializer());
    redisClient.setValueSerializer(new JsonRedisSerializer());
    return redisClient;
  }
}
