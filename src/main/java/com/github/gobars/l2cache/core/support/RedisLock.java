package com.github.gobars.l2cache.core.support;

import com.github.gobars.l2cache.core.redis.client.RedisClient;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.*;

/**
 * Redis分布式锁 使用 SET resource-name anystring NX EX max-lock-time 实现
 *
 * <p>该方案在 Redis 官方 SET 命令页有详细介绍。 http://doc.redisfans.com/string/set.html
 *
 * <p>在介绍该分布式锁设计之前，我们先来看一下在从 Redis 2.6.12 开始 SET 提供的新特性， 命令 SET key value [EX seconds] [PX
 * milliseconds] [NX|XX]，其中：
 *
 * <p>EX seconds — 以秒为单位设置 key 的过期时间； PX milliseconds — 以毫秒为单位设置 key 的过期时间； NX — 将key 的值设为value
 * ，当且仅当key 不存在，等效于 SETNX。 XX — 将key 的值设为value ，当且仅当key 存在，等效于 SETEX。
 *
 * <p>命令 SET resource-name anystring NX EX max-lock-time 是一种在 Redis 中实现锁的简单方法。
 *
 * <p>客户端执行以上的命令：
 *
 * <p>如果服务器返回 OK ，那么这个客户端获得锁。 如果服务器返回 NIL ，那么客户端获取锁失败，可以在稍后再重试。
 *
 * @author yuhao.wangwang
 * @version 1.0
 * @since 2017年11月3日 上午10:21:27
 */
@Slf4j
public class RedisLock {
  private final RedisClient client;
  /** 锁标志对应的key */
  private final String lockKey;
  /** 锁对应的值 */
  private String lockValue;

  /** 锁的有效时间 */
  @Setter private int expireSecs = 60;
  /** 请求锁的超时时间 */
  @Setter private long timeOutNs = 100;
  /** 锁标记 */
  private volatile boolean locked = false;

  private final Random random = new Random();

  /**
   * 使用默认的锁过期时间和请求锁的超时时间
   *
   * @param client redis客户端
   * @param lockKey 锁的key（Redis的Key）
   */
  public RedisLock(RedisClient client, String lockKey) {
    this.client = client;
    this.lockKey = "lock:" + lockKey;
  }

  /**
   * 使用默认的请求锁的超时时间，指定锁的过期时间
   *
   * @param client redis客户端
   * @param lockKey 锁的key（Redis的Key）
   * @param expireSecs 锁的过期时间(单位：秒)
   */
  public RedisLock(RedisClient client, String lockKey, int expireSecs) {
    this(client, lockKey);
    this.expireSecs = expireSecs;
  }

  /**
   * 锁的过期时间和请求锁的超时时间都是用指定的值
   *
   * @param client redis客户端
   * @param lockKey 锁的key（Redis的Key）
   * @param expireSecs 锁的过期时间(单位：秒)
   * @param timeOutMs 请求锁的超时时间(单位：毫秒)
   */
  public RedisLock(RedisClient client, String lockKey, int expireSecs, long timeOutMs) {
    this(client, lockKey, expireSecs);
    this.timeOutNs = timeOutMs * 1000000;
  }

  /**
   * 尝试获取锁 超时返回
   *
   * @return boolean
   */
  public boolean timeoutLock() {
    // 生成随机key
    this.lockValue = UUID.randomUUID().toString();
    // 系统当前时间，纳秒
    long nowTime = System.nanoTime();
    while (System.nanoTime() - nowTime < timeOutNs) {
      if (setNxEx(lockKey, lockValue, expireSecs)) {
        locked = true;
        return true;
      }

      // 每次请求等待一段时间
      sleep(10, 50000);
    }

    return locked;
  }

  /**
   * 尝试获取锁 立即返回
   *
   * @return 是否成功获得锁
   */
  public boolean tryLock() {
    this.lockValue = UUID.randomUUID().toString();
    // 不存在则添加 且设置过期时间
    locked = setNxEx(lockKey, lockValue, expireSecs);
    return locked;
  }

  /**
   * 解锁
   *
   * <p>可以通过以下修改，让这个锁实现更健壮：
   *
   * <p>不使用固定的字符串作为键的值，而是设置一个不可猜测（non-guessable）的长随机字符串，作为口令串（token）。 不使用 DEL 命令来释放锁，而是发送一个 Lua
   * 脚本，这个脚本只在客户端传入的值和键的口令串相匹配时，才对键进行删除。 这两个改动可以防止持有过期锁的客户端误删现有锁的情况出现。
   *
   * @return 是否删除锁定成功
   */
  public boolean unlock() {
    // 只有加锁成功且锁有效才释放锁
    if (!locked) {
      return true;
    }

    // 解锁的lua脚本
    val lua =
        "if redis.call(\"get\",KEYS[1])==ARGV[1] then "
            + "return redis.call(\"del\",KEYS[1]) "
            + "else return 0 end";

    try {
      Long result = (Long) client.eval(lua, newList(lockKey), newList(lockValue));
      if (result == 0) {
        log.debug("Redis解锁失败！key:{}, 解锁时间：{}", lockKey, System.currentTimeMillis());
      }

      // 0 表示当前锁已经被别人持有了
      locked = result == 0;
      return result == 1;
    } catch (Throwable e) {
      log.warn("Redis不支持EVAL命令，使用降级方式解锁：{}", e.getMessage());
      if (lockValue.equals(this.get(lockKey, String.class))) {
        client.delete(lockKey);
        return true;
      }

      return false;
    }
  }

  /**
   * setNxEx of redis.
   *
   * <p>see: https://redis.io/commands/set
   *
   * <p>命令 SET resource-name anystring NX EX max-lock-time 是一种在 Redis 中实现锁的简单方法。
   *
   * <p>客户端执行以上的命令：
   *
   * <p>如果服务器返回 OK ，那么这个客户端获得锁。
   *
   * <p>如果服务器返回 NIL ，那么客户端获取锁失败，可以在稍后再重试。
   *
   * @param key 锁的Key
   * @param value 锁的值
   * @param seconds 过去时间（秒）
   * @return 是否加锁成功
   */
  private boolean setNxEx(final String key, final String value, final long seconds) {
    boolean ok = "OK".equals(client.setNxEx(key, value, seconds));
    if (ok) {
      log.debug("加锁成功。 key:{}, 当前时间：{}", key, System.currentTimeMillis());
    }

    return ok;
  }

  /**
   * 获取redis里面的值
   *
   * @param key key
   * @param aClass class
   * @return T
   */
  private <T> T get(final String key, Class<T> aClass) {
    return (T) client.get(key);
  }

  /**
   * sleep millis + rand(nanos).
   *
   * @param millis 毫秒
   * @param nanos 纳秒
   */
  private void sleep(long millis, int nanos) {
    try {
      Thread.sleep(millis, random.nextInt(nanos));
    } catch (Exception e) {
      log.debug("获得锁休眠被中断：", e);
    }
  }

  public static <T> List<T> newList(T... values) {
    ArrayList<T> l = new ArrayList<>(values.length);
    Collections.addAll(l, values);

    return l;
  }
}
