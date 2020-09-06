package com.github.gobars.l2cache.core.manager;

import com.github.gobars.l2cache.core.cache.Cache;
import com.github.gobars.l2cache.core.cache.CaffeineCache;
import com.github.gobars.l2cache.core.cache.L2Cache;
import com.github.gobars.l2cache.core.cache.RedisCache;
import com.github.gobars.l2cache.core.redis.client.RedisClient;
import com.github.gobars.l2cache.core.setting.L2Setting;
import lombok.val;

public class L2Manager extends AbstractCacheManager {
  public L2Manager(RedisClient redisClient) {
    this.redisClient = redisClient;
    cacheManagers.add(this);
  }

  @Override
  protected Cache getMissingCache(String name, L2Setting l2Setting) {
    val l1 = new CaffeineCache(name, l2Setting.getC1Setting(), isStats());
    val l2 = new RedisCache(name, redisClient, l2Setting.getC2Setting(), isStats());
    return new L2Cache(redisClient, l1, l2, super.isStats(), l2Setting);
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
