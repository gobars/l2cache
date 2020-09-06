package com.github.gobars.l2cache.core.redis.serializer;

/**
 * 序列化工具类
 *
 * @author yuhao.wang3
 */
public abstract class SerializationUtils {
  static final byte[] EMPTY_ARRAY = new byte[0];

  static boolean isEmpty(byte[] data) {
    return (data == null || data.length == 0);
  }
}
