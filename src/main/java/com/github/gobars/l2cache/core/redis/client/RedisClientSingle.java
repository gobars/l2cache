package com.github.gobars.l2cache.core.redis.client;

import com.alibaba.fastjson.JSON;
import com.github.gobars.l2cache.core.listener.RedisMessageListener;
import com.github.gobars.l2cache.core.redis.serializer.JsonRedisSerializer;
import com.github.gobars.l2cache.core.redis.serializer.RedisSerializer;
import com.github.gobars.l2cache.core.redis.serializer.SerializationException;
import com.github.gobars.l2cache.core.redis.serializer.StringRedisSerializer;
import com.github.gobars.l2cache.core.util.StringUtils;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 单机版Redis客户端
 *
 * @author olafwang
 */
@Slf4j
public class RedisClientSingle implements RedisClient {
  /** 默认key序列化方式 */
  @Getter @Setter private RedisSerializer<String> keySerializer = new StringRedisSerializer();
  /** 默认value序列化方式 */
  @Getter @Setter private RedisSerializer<Object> valueSerializer = new JsonRedisSerializer();

  private final io.lettuce.core.RedisClient client;
  private final StatefulRedisConnection<byte[], byte[]> connection;
  private final StatefulRedisPubSubConnection<String, String> pubsubConnection;

  public RedisClientSingle(RedisProperties properties) {
    RedisURI redisURI =
        RedisURI.builder()
            .withHost(properties.getHost())
            .withDatabase(properties.getDatabase())
            .withPort(properties.getPort())
            .build();
    if (!StringUtils.isBlank(properties.getPassword())) {
      redisURI.setPassword(properties.getPassword());
    }

    log.info("l2cache redis配置" + JSON.toJSONString(properties));
    this.client = io.lettuce.core.RedisClient.create(redisURI);
    this.connection = client.connect(new ByteArrayCodec());
    this.pubsubConnection = client.connectPubSub();
  }

  @Override
  public Object get(String key) {
    try {
      byte[] keyBytes = getKeySerializer().serialize(key);
      byte[] valueBytes = connection.sync().get(keyBytes);
      return getValueSerializer().deserialize(valueBytes);
    } catch (SerializationException e) {
      throw e;
    } catch (Exception e) {
      throw new RedisClientException(e.getMessage(), e);
    }
  }

  @Override
  public <T> T get(String key, Class<T> t) {
    return (T) get(key);
  }

  @Override
  public String set(String key, Object value) {
    try {
      RedisCommands<byte[], byte[]> sync = connection.sync();
      return sync.set(getKeySerializer().serialize(key), getValueSerializer().serialize(value));
    } catch (SerializationException e) {
      throw e;
    } catch (Exception e) {
      throw new RedisClientException(e.getMessage(), e);
    }
  }

  @Override
  public String set(String key, Object value, long time, TimeUnit unit) {
    try {
      RedisCommands<byte[], byte[]> sync = connection.sync();
      return sync.setex(
          getKeySerializer().serialize(key),
          unit.toSeconds(time),
          getValueSerializer().serialize(value));
    } catch (SerializationException e) {
      throw e;
    } catch (Exception e) {
      throw new RedisClientException(e.getMessage(), e);
    }
  }

  @Override
  public String setNxEx(String key, Object value, long time) {
    try {
      RedisCommands<byte[], byte[]> sync = connection.sync();
      return sync.set(
          getKeySerializer().serialize(key),
          getValueSerializer().serialize(value),
          SetArgs.Builder.nx().ex(time));
    } catch (SerializationException e) {
      throw e;
    } catch (Exception e) {
      throw new RedisClientException(e.getMessage(), e);
    }
  }

  @Override
  public Long delete(String... keys) {
    if (Objects.isNull(keys) || keys.length == 0) {
      return 0L;
    }
    try {
      RedisCommands<byte[], byte[]> sync = connection.sync();

      final byte[][] bkeys = new byte[keys.length][];
      for (int i = 0; i < keys.length; i++) {
        bkeys[i] = getKeySerializer().serialize(keys[i]);
      }
      return sync.del(bkeys);
    } catch (SerializationException e) {
      throw e;
    } catch (Exception e) {
      throw new RedisClientException(e.getMessage(), e);
    }
  }

  @Override
  public Long delete(Set<String> keys) {
    return delete(keys.toArray(new String[0]));
  }

  @Override
  public Boolean hasKey(String key) {
    try {
      RedisCommands<byte[], byte[]> sync = connection.sync();
      return sync.exists(getKeySerializer().serialize(key)) > 0;
    } catch (SerializationException e) {
      throw e;
    } catch (Exception e) {
      throw new RedisClientException(e.getMessage(), e);
    }
  }

  @Override
  public Boolean expire(String key, long timeoutSecs) {
    try {
      RedisCommands<byte[], byte[]> sync = connection.sync();
      return sync.expire(getKeySerializer().serialize(key), timeoutSecs);
    } catch (SerializationException e) {
      throw e;
    } catch (Exception e) {
      throw new RedisClientException(e.getMessage(), e);
    }
  }

  @Override
  public long getExpireSecs(String key) {
    try {
      RedisCommands<byte[], byte[]> sync = connection.sync();
      return sync.ttl(getKeySerializer().serialize(key));
    } catch (SerializationException e) {
      throw e;
    } catch (Exception e) {
      throw new RedisClientException(e.getMessage(), e);
    }
  }

  @Override
  public Set<String> scan(String pattern) {
    Set<String> keys = new HashSet<>();
    try {
      RedisCommands<byte[], byte[]> sync = connection.sync();
      boolean finished;
      ScanCursor cursor = ScanCursor.INITIAL;
      do {
        KeyScanCursor<byte[]> scanCursor =
            sync.scan(cursor, ScanArgs.Builder.limit(1000).match(pattern));
        scanCursor.getKeys().forEach(key -> keys.add((String) getKeySerializer().deserialize(key)));
        finished = scanCursor.isFinished();
        cursor = ScanCursor.of(scanCursor.getCursor());
      } while (!finished);
    } catch (SerializationException e) {
      throw e;
    } catch (Exception e) {
      throw new RedisClientException(e.getMessage(), e);
    }
    return keys;
  }

  @Override
  public Object eval(String script, List<String> keys, List<String> args) {
    try {
      RedisCommands<byte[], byte[]> sync = connection.sync();
      List<byte[]> bkeys =
          keys.stream()
              .map(key -> key.getBytes(StandardCharsets.UTF_8))
              .collect(Collectors.toList());
      List<byte[]> bargs =
          args.stream()
              .map(key -> key.getBytes(StandardCharsets.UTF_8))
              .collect(Collectors.toList());
      return sync.eval(
          script,
          ScriptOutputType.INTEGER,
          bkeys.toArray(new byte[0][0]),
          bargs.toArray(new byte[0][0]));
    } catch (SerializationException e) {
      throw e;
    } catch (Exception e) {
      throw new RedisClientException(e.getMessage(), e);
    }
  }

  @Override
  public Long publish(String channel, String message) {
    try {
      return pubsubConnection.sync().publish(channel, message);
    } catch (SerializationException e) {
      throw e;
    } catch (Exception e) {
      throw new RedisClientException(e.getMessage(), e);
    }
  }

  @Override
  public void subscribe(RedisMessageListener messageListener, String... channels) {
    try {
      StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub();
      log.info("l2cache和redis创建订阅关系，订阅频道【{}】", Arrays.toString(channels));
      connection.sync().subscribe(channels);
      connection.addListener(messageListener);
    } catch (SerializationException e) {
      throw e;
    } catch (Exception e) {
      throw new RedisClientException(e.getMessage(), e);
    }
  }
}
