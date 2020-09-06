package com.github.gobars.l2cache.core.redis.client;

import com.github.gobars.l2cache.core.listener.RedisMessageListener;
import com.github.gobars.l2cache.core.redis.serializer.RedisSerializer;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface RedisClient {

  /**
   * 通过key获取储存在redis中的value
   *
   * @param key key
   * @return 成功返回value 失败返回null
   */
  Object get(String key);

  /**
   * @param key
   * @return
   */
  /**
   * 通过key获取储存在redis中的value,自动转对象
   *
   * @param key key
   * @param t 返回值类型对应的Class对象
   * @param <T> 返回值类型
   * @return 成功返回value 失败返回null
   * @author manddoxli
   */
  <T> T get(String key, Class<T> t);

  /**
   * 向redis存入key和value,并释放连接资源
   *
   * <p>如果key已经存在 则覆盖
   *
   * @param key key
   * @param value value
   * @return 成功 返回OK 失败返回 0
   */
  String set(String key, Object value);

  /**
   * 向redis存入key和value,并释放连接资源
   *
   * <p>如果key已经存在 则覆盖
   *
   * @param key key
   * @param value value
   * @return 成功 返回OK 失败返回 0
   */
  String set(String key, Object value, long time, TimeUnit unit);

  /**
   * Set the string value as value of the key. The string can't be longer than 1073741824 bytes (1
   * GB).
   *
   * @param key key
   * @param value value
   * @param time expire time in the units of <code>expx</code>
   * @return Status code reply
   */
  String setNxEx(final String key, final Object value, final long time);

  /**
   * 删除指定的key,也可以传入一个包含key的数组
   *
   * @param keys 一个key 也可以使 string 数组
   * @return 返回删除成功的个数
   */
  Long delete(String... keys);

  /**
   * 删除一批key
   *
   * @param keys key的Set集合
   * @return 返回删除成功的个数
   */
  Long delete(Set<String> keys);

  /**
   * 判断key是否存在
   *
   * @param key key
   * @return true OR false
   */
  Boolean hasKey(String key);

  /**
   * 为给定 key 设置生存时间，当 key 过期时(生存时间为 0 )，它会被自动删除。
   *
   * @param key key
   * @param timeoutSecs 过期时间
   * @return 成功返回1 如果存在 和 发生异常 返回 0
   */
  Boolean expire(String key, long timeoutSecs);

  /**
   * 以秒为单位，返回给定 key 的剩余生存时间
   *
   * @param key key
   * @return 当 key 不存在时或没有设置剩余生存时间时，返回 -1 。否则，以秒为单位，返回 key 的剩余生存时间。 发生异常 返回 0
   */
  long getExpireSecs(String key);

  /**
   * 查询符合条件的key
   *
   * @param pattern 表达式
   * @return 返回符合条件的key
   */
  Set<String> scan(String pattern);

  /**
   * 执行Lua脚本
   *
   * @param script Lua 脚本
   * @param keys 参数
   * @param args 参数值
   * @return 返回结果
   */
  Object eval(String script, List<String> keys, List<String> args);

  /**
   * 发送消息
   *
   * @param channel 发送消息的频道
   * @param message 消息内容
   * @return Long
   */
  Long publish(String channel, String message);

  /**
   * 绑定监听器
   *
   * @param messageListener 消息监听器
   * @param channel 信道
   */
  void subscribe(RedisMessageListener messageListener, String... channel);

  /** @return the key {@link RedisSerializer}. */
  RedisSerializer<String> getKeySerializer();

  /** @return the value {@link RedisSerializer}. */
  RedisSerializer<Object> getValueSerializer();

  /**
   * 设置key的序列化方式
   *
   * @param keySerializer {@link RedisSerializer}
   */
  void setKeySerializer(RedisSerializer<String> keySerializer);

  /**
   * 设置value的序列化方式
   *
   * @param valueSerializer {@link RedisSerializer}
   */
  void setValueSerializer(RedisSerializer<Object> valueSerializer);
}
