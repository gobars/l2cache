package com.github.gobars.l2cache.starter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** @author yuhao.wang3 */
@ConfigurationProperties("l2cache")
public class L2CacheProperties {

  /** 是否开启缓存统计 */
  private boolean stats = true;

  /** 命名空间，必须唯一般使用服务名 */
  private String namespace;

  public boolean isStats() {
    return stats;
  }

  public void setStats(boolean stats) {
    this.stats = stats;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }
}
