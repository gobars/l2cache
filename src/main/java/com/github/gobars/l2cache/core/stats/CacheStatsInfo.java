package com.github.gobars.l2cache.core.stats;

import com.github.gobars.l2cache.core.setting.L2Setting;
import lombok.Data;

import java.util.Objects;

/**
 * 缓存命中率统计实体类
 *
 * @author yuhao.wang3
 */
@Data
public class CacheStatsInfo {
  /** 缓存名称 */
  private String cacheName;

  /** 内部Key，由[L1有效时间-L2有效时间-L2自动刷新时间]组成 */
  private String internalKey;

  /** 描述,数据监控页面使用 */
  private String desc;

  /** 总请求总数 */
  private long requestCount;

  /** 总未命中总数 */
  private long missCount;

  /** 命中率 */
  private double hitRate;

  /** L1命中总数 */
  private long l1RequestCount;

  /** L1未命中总数 */
  private long l1MissCount;

  /** L2命中总数 */
  private long l2RequestCount;

  /** L2未命中总数 */
  private long l2MissCount;

  /** 总的请求时间 */
  private long totalLoadTime;

  /** 缓存配置 */
  private L2Setting l2Setting;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CacheStatsInfo that = (CacheStatsInfo) o;

    return Objects.equals(cacheName, that.cacheName)
        && Objects.equals(internalKey, that.internalKey);
  }

  @Override
  public int hashCode() {
    int result = cacheName != null ? cacheName.hashCode() : 0;
    result = 31 * result + (internalKey != null ? internalKey.hashCode() : 0);
    return result;
  }

  /** 清空统计信息 */
  public void clear() {
    this.setRequestCount(0);
    this.setMissCount(0);
    this.setTotalLoadTime(0);
    this.setHitRate(0);
    this.setL1RequestCount(0);
    this.setL1MissCount(0);
    this.setL2RequestCount(0);
    this.setL2MissCount(0);
  }
}
