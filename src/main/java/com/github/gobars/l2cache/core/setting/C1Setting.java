package com.github.gobars.l2cache.core.setting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * L1配置项
 *
 * @author yuhao.wang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class C1Setting {

  /** 缓存初始Size */
  private int initCap = 10;

  /** 缓存最大Size */
  private int maxSize = 500;

  /** 缓存有效时间 */
  private int expireSecs = 0;
}
