package com.github.gobars.l2cache.core.redis.serializer;

import com.github.gobars.l2cache.core.support.NestedException;

/**
 * Generic exception indicating a serialization/deserialization error.
 *
 * @author Costin Leau
 */
public class SerializationException extends NestedException {

  /**
   * Constructs a new <code>SerializationException</code> instance.
   *
   * @param msg msg
   * @param cause 原因
   */
  public SerializationException(String msg, Throwable cause) {
    super(msg, cause);
  }

  /**
   * Constructs a new <code>SerializationException</code> instance.
   *
   * @param msg msg
   */
  public SerializationException(String msg) {
    super(msg);
  }
}
