package com.github.gobars.l2cache.core.listener;

import lombok.Data;

import java.io.Serializable;

/**
 * redis pub/sub 消息
 *
 * @author yuhao.wang3
 */
@Data
public class RedisPubSubMessage implements Serializable {
  public static final String SOURCE = "web-manage";

  /** 缓存名称 */
  private String cacheName;

  /** 缓存key */
  private String key;

  /** 消息类型 */
  private RedisPubSubMessageType messageType;

  /** 消息来源 */
  private String source;
}
