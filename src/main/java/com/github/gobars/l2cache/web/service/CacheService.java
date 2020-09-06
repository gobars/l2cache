package com.github.gobars.l2cache.web.service;

import com.github.gobars.l2cache.core.cache.Cache;
import com.github.gobars.l2cache.core.manager.AbstractCacheManager;
import com.github.gobars.l2cache.core.setting.C1Setting;
import com.github.gobars.l2cache.core.setting.C2Setting;
import com.github.gobars.l2cache.core.setting.L2Setting;
import com.github.gobars.l2cache.core.stats.StatsService;
import com.github.gobars.l2cache.core.util.BeanFactory;
import com.github.gobars.l2cache.core.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Set;

/**
 * 操作缓存的服务
 *
 * @author yuhao.wang3
 */
@Service
public class CacheService {
  /**
   * 删除缓存
   *
   * @param cacheName 缓存名称
   * @param internalKey 内部Key，由[L1有效时间-L2有效时间-L2自动刷新时间]组成
   * @param key key，可以为NULL，如果是NULL则清空缓存
   */
  public void deleteCache(String cacheName, String internalKey, String key) {
    if (StringUtils.isBlank(cacheName) || StringUtils.isBlank(internalKey)) {
      return;
    }
    L2Setting defaultSetting =
        new L2Setting(new C1Setting(), new C2Setting(), "默认缓存配置（删除时生成）");
    Set<AbstractCacheManager> cacheManagers = AbstractCacheManager.getCacheManager();
    if (StringUtils.isBlank(key)) {
      // 清空缓存
      for (AbstractCacheManager cacheManager : cacheManagers) {
        // 删除缓存统计信息
        String redisKey = StatsService.PREFIX + cacheName + internalKey;
        BeanFactory.getBean(StatsService.class).resetCacheStat(redisKey);

        // 删除缓存
        Collection<Cache> caches = cacheManager.getCache(cacheName);
        if (CollectionUtils.isEmpty(caches)) {
          // 如果没有找到Cache就新建一个默认的
          Cache cache = cacheManager.getCache(cacheName, defaultSetting);
          cache.clear();

          // 删除统计信息
          redisKey =
              StatsService.PREFIX + cacheName + defaultSetting.getInternalKey();
          BeanFactory.getBean(StatsService.class).resetCacheStat(redisKey);
        } else {
          for (Cache cache : caches) {
            cache.clear();
          }
        }
      }

      return;
    }

    // 删除指定key
    for (AbstractCacheManager cacheManager : cacheManagers) {
      Collection<Cache> caches = cacheManager.getCache(cacheName);
      if (CollectionUtils.isEmpty(caches)) {
        // 如果没有找到Cache就新建一个默认的
        Cache cache = cacheManager.getCache(cacheName, defaultSetting);
        cache.evict(key);
      } else {
        for (Cache cache : caches) {
          cache.evict(key);
        }
      }
    }
  }
}
