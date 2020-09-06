package com.github.gobars.l2cache.core.redis.client;

import lombok.Data;

@Data
public class RedisProperties {
  Integer database = 0;
  /**
   * redis://[password@]host[:port][,host2[:port2]]
   *
   * <p>redis://password@localhost:7379,localhost2:7379
   */
  String cluster = "";

  String host = "localhost";
  Integer port = 6379;
  String password = null;
}
