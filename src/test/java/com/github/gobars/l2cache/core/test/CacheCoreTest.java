package com.github.gobars.l2cache.core.test;

import com.github.gobars.l2cache.core.cache.Cache;
import com.github.gobars.l2cache.core.cache.L2Cache;
import com.github.gobars.l2cache.core.cache.RedisCache;
import com.github.gobars.l2cache.core.cache.RedisCacheKey;
import com.github.gobars.l2cache.core.config.CacheConfig;
import com.github.gobars.l2cache.core.manager.CacheManager;
import com.github.gobars.l2cache.core.redis.client.RedisClient;
import com.github.gobars.l2cache.core.setting.C1Setting;
import com.github.gobars.l2cache.core.setting.C2Setting;
import com.github.gobars.l2cache.core.setting.L2Setting;
import com.github.gobars.l2cache.core.stats.CacheStats;
import com.github.gobars.l2cache.core.support.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collection;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {CacheConfig.class})
@Slf4j
public class CacheCoreTest {
  @Autowired private CacheManager cacheManager;
  @Autowired private RedisClient redisClient;

  private L2Setting l2Setting1;
  private L2Setting l2Setting4;
  private L2Setting l2Setting5;

  @Before
  public void testGetCache() {
    // 测试 CacheManager getCache方法
    C1Setting c1Setting0 = new C1Setting(10, 1000, 4);
    C2Setting l1CacheSetting1 = new C2Setting(10, 4, true);
    l2Setting1 = new L2Setting(c1Setting0, l1CacheSetting1, "");

    // L2可以缓存null,时间倍率是1
    C1Setting c1Setting2 = new C1Setting(10, 1000, 5);
    C2Setting c2Setting2 = new C2Setting(3000, 14, true);
    L2Setting l2L2Setting2 = new L2Setting(c1Setting2, c2Setting2, "");

    // L2可以缓存null,时间倍率是10
    C1Setting c1Setting4 = new C1Setting(10, 1000, 5);
    C2Setting c2Setting4 = new C2Setting(100, 70, true);
    l2Setting4 = new L2Setting(c1Setting4, c2Setting4, "");

    // L2不可以缓存null
    C1Setting c1Setting5 = new C1Setting(10, 1000, 5);
    C2Setting c2Setting5 = new C2Setting(10, 7, false);
    l2Setting5 = new L2Setting(c1Setting5, c2Setting5, "");

    String cacheName = "cache:name";
    Cache cache1 = cacheManager.getCache(cacheName, l2Setting1);
    Cache cache2 = cacheManager.getCache(cacheName, l2Setting1);
    Assert.assertEquals(cache1, cache2);

    Cache cache3 = cacheManager.getCache(cacheName, l2L2Setting2);
    Collection<Cache> caches = cacheManager.getCache(cacheName);
    Assert.assertEquals(2, caches.size());
    Assert.assertNotEquals(cache1, cache3);
  }

  @Test
  public void testCacheexpireSecs() {
    // 测试 缓存过期时间
    String cacheName = "cache:name";
    String cacheKey1 = "cache:key1";
    L2Cache cache1 = (L2Cache) cacheManager.getCache(cacheName, l2Setting1);
    cache1.get(cacheKey1, () -> initCache(String.class));
    // 测试L1值及过期时间
    String str1 = cache1.getCache1().get(cacheKey1, String.class);
    String st2 = cache1.getCache1().get(cacheKey1, () -> initCache(String.class));
    log.debug("========================:{}", str1);
    Assert.assertEquals(str1, st2);
    Assert.assertEquals(str1, initCache(String.class));
    sleep(5);
    Assert.assertNull(cache1.getCache1().get(cacheKey1, String.class));
    // 看日志是不是走了L2
    cache1.get(cacheKey1, () -> initCache(String.class));

    // 测试L2
    str1 = cache1.getCache2().get(cacheKey1, String.class);
    st2 = cache1.getCache2().get(cacheKey1, () -> initCache(String.class));
    Assert.assertEquals(st2, str1);
    Assert.assertEquals(str1, initCache(String.class));
    sleep(5);
    // 看日志是不是走了自动刷新
    RedisCacheKey redisCacheKey = ((RedisCache) cache1.getCache2()).getRedisCacheKey(cacheKey1);
    cache1.get(cacheKey1, () -> initCache(String.class));
    sleep(6);
    Long ttl = redisClient.getExpireSecs(redisCacheKey.getKey());
    log.debug("========================ttl 1:{}", ttl);
    Assert.assertNotNull(cache1.getCache2().get(cacheKey1));
    sleep(5);
    ttl = redisClient.getExpireSecs(redisCacheKey.getKey());
    log.debug("========================ttl 2:{}", ttl);
    Assert.assertNull(cache1.getCache2().get(cacheKey1));
  }

  @Test
  public void testGetCacheNullUserAllowNullValueTrue() {
    log.info("测试L2允许为NULL，NULL值时间倍率是10");
    // 测试 缓存过期时间
    String cacheName = "cache:name:118_1";
    String cacheKey1 = "cache:key1:118_1";
    L2Cache cache1 = (L2Cache) cacheManager.getCache(cacheName, l2Setting4);
    cache1.get(cacheKey1, this::initNullCache);
    // 测试L1值不能缓存NULL
    String str1 = cache1.getCache1().get(cacheKey1, String.class);
    com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
        (com.github.benmanes.caffeine.cache.Cache<Object, Object>)
            cache1.getCache1().getNativeCache();
    Assert.assertNull(str1);
    Assert.assertEquals(0, nativeCache.asMap().size());

    // 测试L2可以存NULL值，NULL值时间倍率是10
    String st2 = cache1.getCache2().get(cacheKey1, String.class);
    RedisCacheKey redisCacheKey = ((RedisCache) cache1.getCache2()).getRedisCacheKey(cacheKey1);
    Long ttl = redisClient.getExpireSecs(redisCacheKey.getKey());
    Assert.assertTrue(redisClient.hasKey(redisCacheKey.getKey()));
    Assert.assertNull(st2);
    Assert.assertTrue(ttl <= 10);
    sleep(5);
    st2 = cache1.getCache2().get(cacheKey1, String.class);
    Assert.assertNull(st2);
    cache1.getCache2().get(cacheKey1, this::initNullCache);
    sleep(1);
    ttl = redisClient.getExpireSecs(redisCacheKey.getKey());
    Assert.assertTrue(ttl <= 10 && ttl > 5);

    st2 = cache1.get(cacheKey1, String.class);
    Assert.assertNull(st2);
  }

  @Test
  public void testGetCacheNullUserAllowNullValueFalse() {
    log.info("测试L2不允许为NULL");
    // 测试 缓存过期时间
    String cacheName = "cache:name:118_2";
    String cacheKey1 = "cache:key1:118_2";
    L2Cache cache1 = (L2Cache) cacheManager.getCache(cacheName, l2Setting5);
    cache1.get(cacheKey1, this::initNullCache);
    // 测试L1值不能缓存NULL
    String str1 = cache1.getCache1().get(cacheKey1, String.class);
    com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
        (com.github.benmanes.caffeine.cache.Cache<Object, Object>)
            cache1.getCache1().getNativeCache();
    Assert.assertNull(str1);
    Assert.assertEquals(0, nativeCache.asMap().size());

    // 测试L2不可以存NULL值，NULL值时间倍率是10
    String st2 = cache1.getCache2().get(cacheKey1, String.class);
    RedisCacheKey redisCacheKey = ((RedisCache) cache1.getCache2()).getRedisCacheKey(cacheKey1);
    Assert.assertFalse(redisClient.hasKey(redisCacheKey.getKey()));
    Assert.assertNull(st2);
  }

  @Test
  public void testGetType() throws Exception {
    // 测试 缓存过期时间
    String cacheName = "cache:name";
    String cacheKey1 = "cache:key:22";
    L2Cache cache1 = (L2Cache) cacheManager.getCache(cacheName, l2Setting1);
    cache1.get(cacheKey1, () -> null);
    String str1 = cache1.get(cacheKey1, String.class);
    Assert.assertNull(str1);
    sleep(11);
    cache1.get(cacheKey1, () -> initCache(String.class));

    str1 = cache1.get(cacheKey1, String.class);
    Assert.assertEquals(str1, initCache(String.class));
  }

  @Test
  public void testCacheEvict() throws Exception {
    // 测试 缓存过期时间
    String cacheName = "cache:name";
    String cacheKey1 = "cache:key2";
    String cacheKey2 = "cache:key3";
    L2Cache cache1 = (L2Cache) cacheManager.getCache(cacheName, l2Setting1);
    cache1.get(cacheKey1, () -> initCache(String.class));
    cache1.get(cacheKey2, () -> initCache(String.class));
    // 测试删除方法
    cache1.evict(cacheKey1);
    Thread.sleep(500);
    String str1 = cache1.get(cacheKey1, String.class);
    String str2 = cache1.get(cacheKey2, String.class);
    Assert.assertNull(str1);
    Assert.assertNotNull(str2);
    // 测试删除方法
    cache1.evict(cacheKey1);
    Thread.sleep(500);
    str1 = cache1.get(cacheKey1, () -> initCache(String.class));
    str2 = cache1.get(cacheKey2, String.class);
    Assert.assertNotNull(str1);
    Assert.assertNotNull(str2);
  }

  @Test
  public void testCacheClear() throws Exception {
    // 测试 缓存过期时间
    String cacheName = "cache:name";
    String cacheKey1 = "cache:key4";
    String cacheKey2 = "cache:key5";
    L2Cache cache = (L2Cache) cacheManager.getCache(cacheName, l2Setting1);
    cache.get(cacheKey1, () -> initCache(String.class));
    cache.get(cacheKey2, () -> initCache(String.class));
    // 测试清除方法
    cache.clear();
    Thread.sleep(500);
    String str1 = cache.get(cacheKey1, String.class);
    String str2 = cache.get(cacheKey2, String.class);
    Assert.assertNull(str1);
    Assert.assertNull(str2);
    // 测试清除方法
    cache.clear();
    Thread.sleep(500);
    str1 = cache.get(cacheKey1, () -> initCache(String.class));
    str2 = cache.get(cacheKey2, () -> initCache(String.class));
    Assert.assertNotNull(str1);
    Assert.assertNotNull(str2);
  }

  @Test
  public void testCachePut() throws Exception {
    // 测试 缓存过期时间
    String cacheName = "cache:name";
    String cacheKey1 = "cache:key6";
    L2Cache cache = (L2Cache) cacheManager.getCache(cacheName, l2Setting1);
    String str1 = cache.get(cacheKey1, String.class);
    Assert.assertNull(str1);

    cache.put(cacheKey1, "test1");
    str1 = cache.get(cacheKey1, String.class);
    Assert.assertEquals(str1, "test1");

    cache.put(cacheKey1, "test2");
    Thread.sleep(2000);
    Object value = cache.getCache1().get(cacheKey1);
    Assert.assertNull(value);
    str1 = cache.get(cacheKey1, String.class);
    Assert.assertEquals(str1, "test2");
  }

  @Test
  public void testPutCacheNullUserAllowNullValueTrue() {
    log.info("测试PutL2允许为NULL，NULL值时间倍率是10");
    // 测试 缓存过期时间
    String cacheName = "cache:name:118_3";
    String cacheKey1 = "cache:key1:118_3";
    L2Cache cache1 = (L2Cache) cacheManager.getCache(cacheName, l2Setting4);
    cache1.put(cacheKey1, initNullCache());
    // 测试L1值不能缓存NULL
    String str1 = cache1.getCache1().get(cacheKey1, String.class);
    com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
        (com.github.benmanes.caffeine.cache.Cache<Object, Object>)
            cache1.getCache1().getNativeCache();
    Assert.assertNull(str1);
    Assert.assertEquals(0, nativeCache.asMap().size());

    // 测试L2可以存NULL值，NULL值时间倍率是10
    String st2 = cache1.getCache2().get(cacheKey1, String.class);
    RedisCacheKey redisCacheKey = ((RedisCache) cache1.getCache2()).getRedisCacheKey(cacheKey1);
    Long ttl = redisClient.getExpireSecs(redisCacheKey.getKey());
    Assert.assertTrue(redisClient.hasKey(redisCacheKey.getKey()));
    Assert.assertNull(st2);
    Assert.assertTrue(ttl <= 10);
    sleep(5);
    st2 = cache1.getCache2().get(cacheKey1, String.class);
    Assert.assertNull(st2);
    cache1.getCache2().get(cacheKey1, this::initNullCache);
    sleep(1);
    ttl = redisClient.getExpireSecs(redisCacheKey.getKey());
    Assert.assertTrue(ttl <= 10 && ttl > 5);

    st2 = cache1.get(cacheKey1, String.class);
    Assert.assertNull(st2);
  }

  @Test
  public void testCacheNullUserAllowNullValueFalse() {
    log.info("测试PutL2不允许为NULL");
    // 测试 缓存过期时间
    String cacheName = "cache:name:118_4";
    String cacheKey1 = "cache:key1:118_4";
    L2Cache cache1 = (L2Cache) cacheManager.getCache(cacheName, l2Setting5);
    cache1.put(cacheKey1, initNullCache());
    // 测试L1值不能缓存NULL
    String str1 = cache1.getCache1().get(cacheKey1, String.class);
    com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
        (com.github.benmanes.caffeine.cache.Cache<Object, Object>)
            cache1.getCache1().getNativeCache();
    Assert.assertNull(str1);
    Assert.assertEquals(0, nativeCache.asMap().size());

    // 测试L2不可以存NULL值，NULL值时间倍率是10
    String st2 = cache1.getCache2().get(cacheKey1, String.class);
    RedisCacheKey redisCacheKey = ((RedisCache) cache1.getCache2()).getRedisCacheKey(cacheKey1);
    Assert.assertFalse(redisClient.hasKey(redisCacheKey.getKey()));
    Assert.assertNull(st2);
  }

  @Test
  public void testCachePutIfAbsent() throws Exception {
    // 测试 缓存过期时间
    String cacheName = "cache:name";
    String cacheKey1 = "cache:key7";
    L2Cache cache = (L2Cache) cacheManager.getCache(cacheName, l2Setting1);
    cache.putIfAbsent(cacheKey1, "test1");
    Thread.sleep(2000);
    Object value = cache.getCache1().get(cacheKey1);
    Assert.assertNull(value);
    String str1 = cache.get(cacheKey1, String.class);
    Assert.assertEquals(str1, "test1");

    cache.putIfAbsent(cacheKey1, "test2");
    str1 = cache.get(cacheKey1, String.class);
    Assert.assertEquals(str1, "test1");
  }

  /** 测试统计 */
  @Test
  public void testStats() {
    // 测试 缓存过期时间
    String cacheName = "cache:name:1";
    String cacheKey1 = "cache:key:123";
    L2Cache cache1 = (L2Cache) cacheManager.getCache(cacheName, l2Setting1);
    cache1.get(cacheKey1, () -> initCache(String.class));
    cache1.get(cacheKey1, () -> initCache(String.class));
    sleep(5);
    cache1.get(cacheKey1, () -> initCache(String.class));

    sleep(11);
    cache1.get(cacheKey1, () -> initCache(String.class));

    CacheStats cacheStats = cache1.getCacheStats();
    CacheStats cacheStats2 = cache1.getCacheStats();
    Assert.assertEquals(
        cacheStats.getRequestCount().longValue(), cacheStats2.getRequestCount().longValue());
    Assert.assertEquals(
        cacheStats.getCachedRequestCount().longValue(),
        cacheStats2.getCachedRequestCount().longValue());
    Assert.assertEquals(
        cacheStats.getCachedRequestTime().longValue(),
        cacheStats2.getCachedRequestTime().longValue());

    log.debug("缓请求数：{}", cacheStats.getRequestCount());
    log.debug("被缓存方法请求数：{}", cacheStats.getCachedRequestCount());
    log.debug("被缓存方法请求总耗时：{}", cacheStats.getCachedRequestTime());

    Assert.assertEquals(cacheStats.getRequestCount().longValue(), 4);
    Assert.assertEquals(cacheStats.getCachedRequestCount().longValue(), 2);
    Assert.assertTrue(cacheStats.getCachedRequestTime().longValue() >= 0);
  }

  /** 测试锁 */
  @Test
  public void testLock() {
    RedisLock lock = new RedisLock(redisClient, "test:123");
    lock.tryLock();
    lock.unlock();
  }

  private <T> T initCache(Class<T> t) {
    log.debug("加载缓存");
    return (T) "test";
  }

  private <T> T initNullCache() {
    log.debug("加载缓存,空值");
    return null;
  }

  private void sleep(int time) {
    try {
      Thread.sleep(time * 1000);
    } catch (InterruptedException e) {
      log.error(e.getMessage(), e);
    }
  }
}
