package com.github.gobars.l2cache.web.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.gobars.l2cache.core.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务
 *
 * @author yuhao.wang3
 */
@Service
public class UserService {
  private static Cache<String, Object> manualCache =
      Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).maximumSize(1000).build();

  @Value("${l2cache.web.username:admin}")
  private String userName;

  @Value("${l2cache.web.password:l2cache}")
  private String password;

  /**
   * 登录校验
   *
   * @param token 唯一标示
   */
  public boolean checkLogin(String token) {
    return !StringUtils.isBlank(token) && isLogin(token);
  }

  /**
   * 登录
   *
   * @param usernameParam 用户名
   * @param passwordParam 密码
   * @param token 唯一标示
   * @return
   */
  public boolean login(String usernameParam, String passwordParam, String token) {
    boolean ok = userName.equals(usernameParam) && password.equals(passwordParam);
    if (ok) {
      manualCache.put(token, userName);
    }

    return ok;
  }

  /**
   * 是否登录
   *
   * @param token 唯一标示
   * @return
   */
  public boolean isLogin(String token) {
    boolean ok = Objects.nonNull(manualCache.getIfPresent(token));
    if (ok) {
      refreshSession(token);
    }

    return ok;
  }

  /**
   * 退出
   *
   * @param token 唯一标示
   */
  public void loginOut(String token) {
    manualCache.invalidate(token);
  }

  /**
   * 刷新session
   *
   * @param token 唯一标示
   */
  public void refreshSession(String token) {
    manualCache.put(token, token);
  }
}
