package com.github.gobars.l2cache.starter.properties;

import com.github.gobars.l2cache.core.util.StringUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "l2cache.redis")
public class L2CacheRedisProperties {
  Integer database = 0;
  String cluster = "";
  String host = "localhost";
  Integer port = 6379;
  String password = null;

  public String getPassword() {
    return StringUtils.isBlank(password) ? null : password;
  }
}
