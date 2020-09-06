package com.github.gobars.l2cache.core.setting;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 多级缓存配置项
 *
 * @author yuhao.wang
 */
@NoArgsConstructor
@Data
public class L2Setting {
  /** 内部Key，由[L1有效时间-L2有效时间-L2自动刷新时间]组成 */
  private String internalKey;

  /** 描述，数据监控页面使用 */
  private String desc;

  /** L1配置 */
  private C1Setting c1Setting;

  /** L2配置 */
  private C2Setting c2Setting;

  public L2Setting(C1Setting c1Setting, C2Setting c2Setting, String desc) {
    this.c1Setting = c1Setting;
    this.c2Setting = c2Setting;
    this.desc = desc;
    internalKey();
  }

  private void internalKey() {
    // L1有效时间-L2有效时间-L2自动刷新时间
    StringBuilder sb = new StringBuilder();
    if (c1Setting != null) {
      sb.append(c1Setting.getExpireSecs());
    }
    sb.append("-");
    if (c2Setting != null) {
      sb.append(c2Setting.getExpireSecs());
      sb.append("-");
      sb.append(c2Setting.getPreloadSecs());
    }
    internalKey = sb.toString();
  }
}
