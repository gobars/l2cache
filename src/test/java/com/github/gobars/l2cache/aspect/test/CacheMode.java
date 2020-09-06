package com.github.gobars.l2cache.aspect.test;

/**
 * 缓存模式
 *
 * @author yuhao.wang3
 */
public enum CacheMode {
  /** 只开启L1 */
  ONLY_FIRST("只是用L1"),

  /** 只开启L2 */
  ONLY_SECOND("只是使用L2"),

  /** 同时开启L1和L2 */
  ALL("同时开启L1和L2");

  private final String label;

  CacheMode(String label) {
    this.label = label;
  }
}
