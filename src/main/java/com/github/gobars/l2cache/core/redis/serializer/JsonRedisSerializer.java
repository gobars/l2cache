package com.github.gobars.l2cache.core.redis.serializer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonRedisSerializer implements RedisSerializer<Object> {
  static {
    // https://github.com/alibaba/fastjson/wiki/enable_autotype
    ParserConfig.getGlobalInstance().addAccept("com.github.gobars.l2cache.");
    // ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
  }

  @Override
  public byte[] serialize(java.lang.Object t) throws SerializationException {

    try {
      return JSON.toJSONBytes(t, SerializerFeature.WriteClassName);
    } catch (Exception e) {
      log.error("JSON序列化异常", e);
      throw new SerializationException(
          String.format("JSON序列化异常: %s, 【%s】", e.getMessage(), JSON.toJSONString(t)), e);
    }
  }

  @Override
  public Object deserialize(byte[] bytes) throws SerializationException {
    if (SerializationUtils.isEmpty(bytes)) {
      return null;
    }

    try {
      return JSON.parse(bytes);
    } catch (Exception e) {
      log.error("JSON反序列化异常", e);
      throw new SerializationException(
          String.format("JSON反序列化异常: %s, 【%s】", e.getMessage(), new String(bytes)), e);
    }
  }
}
