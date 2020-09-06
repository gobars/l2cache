package com.github.gobars.l2cache.web.controller;

import com.github.gobars.l2cache.core.redis.client.RedisClientCluster;
import com.github.gobars.l2cache.core.redis.client.RedisClient;
import com.github.gobars.l2cache.core.redis.client.RedisProperties;
import com.github.gobars.l2cache.core.redis.client.RedisClientSingle;
import com.github.gobars.l2cache.core.redis.serializer.JsonRedisSerializer;
import com.github.gobars.l2cache.core.redis.serializer.StringRedisSerializer;
import com.github.gobars.l2cache.core.util.StringUtils;
import com.github.gobars.l2cache.web.utils.Result;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class RedisController {
  public static final Map<String, RedisClient> redisClientMap = new ConcurrentHashMap<>();

  @RequestMapping("/redis/redis-config")
  @ResponseBody
  public Result login(String address, String password, Integer port, Integer database) {
    try {
      RedisProperties redisProperties = new RedisProperties();
      if (address.contains(":")) {
        redisProperties.setCluster(address);
      } else {
        redisProperties.setHost(address);
      }
      redisProperties.setPassword(StringUtils.isBlank(password) ? null : password);
      redisProperties.setPort(port);
      redisProperties.setDatabase(database);

      String key = address + ":" + port + ":" + database;
      redisClientMap.put(key, getRedisClient(redisProperties));

      RedisClient redisClient = redisClientMap.get(key);
      redisClient.get("test");
      return Result.success();
    } catch (Exception e) {
      return Result.error("配置redis失败" + e.getMessage());
    }
  }

  @RequestMapping("/redis/redis-list")
  @ResponseBody
  public Result redisList() {
    List<Map<String, String>> list = new ArrayList<>();
    try {
      for (String key : redisClientMap.keySet()) {
        Map<String, String> map = new HashMap<>();
        map.put("address", key);
        list.add(map);
      }
      return Result.success(list);
    } catch (Exception e) {
      return Result.error("配置redis失败" + e.getMessage());
    }
  }

  private RedisClient getRedisClient(RedisProperties redisProperties) {
    RedisClient c =
        StringUtils.isBlank(redisProperties.getCluster())
            ? new RedisClientSingle(redisProperties)
            : new RedisClientCluster(redisProperties);

    c.setKeySerializer(new StringRedisSerializer());
    c.setValueSerializer(new JsonRedisSerializer());
    return c;
  }
}
