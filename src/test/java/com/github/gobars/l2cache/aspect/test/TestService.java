package com.github.gobars.l2cache.aspect.test;

import com.github.gobars.l2cache.aspect.annotation.*;
import com.github.gobars.l2cache.aspect.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
public class TestService {
  @Cacheable(
      value = "user:info",
      key = "#userId",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public User getUserById(long userId) {
    log.debug("测试正常配置的缓存方法，参数是基本类型");
    User user = new User();
    user.setUserId(userId);
    user.setAge(31);
    user.setLastName(new String[] {"w", "y", "h"});
    return user;
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public User getUserNoKey(long userId, String[] lastName) {
    log.debug("测试没有配置key的缓存方法，参数是基本类型和数组的缓存缓存方法");
    User user = new User();
    user.setUserId(userId);
    user.setAge(31);
    user.setLastName(lastName);
    return user;
  }

  @Cacheable(
      value = "user:info",
      l1 = @L1(expireSecs = 4),
      ignoreException = false,
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public User getUserObjectPram(User user) {
    log.debug("测试没有配置key的缓存方法，参数是复杂对象");
    return user;
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public User getUser(User user, int age) {
    log.debug("测试没有配置key的缓存方法，参数是复杂对象和基本量类型");
    user.setAge(age);
    return user;
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public User getUserNoParam() {
    log.debug("测试没有配置key的缓存方法，没有参数");
    User user = new User();
    user.setUserId(223);
    user.setAge(31);
    user.setLastName(new String[] {"w", "y", "h"});
    return user;
  }

  @Cacheable(
      value = "user:info",
      key = "#user.userId",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public User getNullObjectPram(User user) {
    log.debug("测试参数是NULL对象，不忽略异常");
    return user;
  }

  @Cacheable(
      value = "user:info",
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public User getNullObjectPramIgnoreException(User user) {
    log.debug("测试参数是NULL对象，忽略异常");
    return user;
  }

  @Cacheable(
      value = "user:info",
      key = "#userId",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public User getNullUser(Long userId) {
    log.debug("缓存方法返回NULL");
    return null;
  }

  @Cacheable(
      value = "user:info",
      key = "#userId",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 100, preloadSecs = 70, forceRefresh = true))
  public User getNullUserAllowNullValueTrueMagnification(Long userId) {
    log.debug("缓存方法返回NULL");
    return null;
  }

  @Cacheable(
      value = "user:info",
      key = "#userId",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 7, forceRefresh = true))
  public User getNullUserAllowNullValueFalse(Long userId) {
    log.debug("缓存方法返回NULL");
    return null;
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public String getString(long userId) {
    log.debug("缓存方法返回字符串");
    return "User";
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public int getInt(long userId) {
    log.debug("缓存方法返回基本类型");
    return 111;
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public Long getLong(long userId) {
    log.debug("缓存方法返回包装类型");
    return 1111L;
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public double getDouble(long userId) {
    log.debug("缓存方法返回包装类型");
    return 111.2;
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public float getFloat(long userId) {
    log.debug("缓存方法返回包装类型");
    return 11.311F;
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public BigDecimal getBigDecimal(long userId) {
    log.debug("缓存方法返回包装类型");
    return new BigDecimal(33.33);
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public Date getDate(long userId) {
    log.debug("缓存方法返回Date类型");
    return new Date();
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public CacheMode getEnum(long userId) {
    log.debug("缓存方法返回枚举");
    return CacheMode.ONLY_FIRST;
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public long[] getArray(long userId) {
    log.debug("缓存方法返回数组");
    return new long[] {111, 222, 333};
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public User[] getObjectArray(long userId) {
    log.debug("缓存方法返回数组");
    User user = new User();
    return new User[] {user, user, user};
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public List<String> getList(long userId) {
    log.debug("调用方法获取数组");
    List<String> list = new ArrayList<>();
    list.add("111");
    list.add("112");
    list.add("113");

    return list;
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public LinkedList<String> getLinkedList(long userId) {
    log.debug("调用方法获取数组");
    LinkedList<String> list = new LinkedList<>();
    list.add("111");
    list.add("112");
    list.add("113");

    return list;
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public List<User> getListObject(long userId) {
    log.debug("调用方法获取数组");
    List<User> list = new ArrayList<>();
    User user = new User();
    list.add(user);
    list.add(user);
    list.add(user);

    return list;
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public Set<String> getSet(long userId) {
    log.debug("调用方法获取数组");
    Set<String> set = new HashSet<>();
    set.add("111");
    set.add("112");
    set.add("113");
    return set;
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public Set<User> getSetObject(long userId) {
    log.debug("调用方法获取数组");
    Set<User> set = new HashSet<>();
    User user = new User();
    set.add(user);
    return set;
  }

  @Cacheable(
      value = "user:info",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public List<User> getException(long userId) {
    log.debug("缓存测试方法");

    throw new RuntimeException("缓存测试方法");
  }

  @CachePut(
      value = "user:info",
      key = "#userId",
      ignoreException = false,
      firstCache = @L1(expireSecs = 4),
      secondaryCache = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public User putUser(long userId) {
    return new User();
  }

  @CachePut(
      value = "user:info",
      firstCache = @L1(expireSecs = 4),
      ignoreException = false,
      secondaryCache = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public User putUserNoParam() {
    User user = new User();
    return user;
  }

  @CachePut(
      value = "user:info:118",
      key = "#userId",
      ignoreException = false,
      firstCache = @L1(expireSecs = 4),
      secondaryCache = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public User putNullUser1118(long userId) {

    return null;
  }

  @CachePut(
      value = "user:info",
      ignoreException = false,
      firstCache = @L1(expireSecs = 40),
      secondaryCache = @L2(expireSecs = 100, preloadSecs = 30, forceRefresh = true))
  public User putUserNoKey(long userId, String[] lastName, User user) {
    return user;
  }

  @Cacheable(
      value = "user:info:118",
      key = "#userId",
      ignoreException = false,
      l1 = @L1(expireSecs = 4),
      l2 = @L2(expireSecs = 10, preloadSecs = 3, forceRefresh = true))
  public User getUserById118(long userId) {
    log.debug("1.1.8版本测试正常配置的缓存方法，参数是基本类型");
    User user = new User();
    user.setUserId(userId);
    user.setAge(31);
    user.setLastName(new String[] {"w", "y", "h"});
    return user;
  }

  @CachePut(
      value = "user:info",
      key = "#userId",
      ignoreException = false,
      firstCache = @L1(expireSecs = 4),
      secondaryCache = @L2(expireSecs = 40, preloadSecs = 30, forceRefresh = true))
  public User putNullUserAllowNullValueTrueMagnification(long userId) {

    return null;
  }

  @CachePut(
      value = "user:info",
      key = "#userId",
      ignoreException = false,
      firstCache = @L1(expireSecs = 4),
      secondaryCache = @L2(expireSecs = 10, preloadSecs = 7, forceRefresh = true))
  public User putNullUserAllowNullValueFalse(long userId) {

    return null;
  }

  @CachePut(
      value = "user:info",
      key = "#userId",
      ignoreException = false,
      firstCache = @L1(expireSecs = 4),
      secondaryCache = @L2(expireSecs = 100, preloadSecs = 3, forceRefresh = true))
  public User putUserById(long userId) {
    User user = new User();
    user.setUserId(userId);
    user.setAge(311);
    user.setLastName(new String[] {"w", "y", "h"});

    return user;
  }

  @CacheEvict(value = "user:info", key = "#userId", ignoreException = false)
  public void evictUser(long userId) {}

  @CacheEvict(value = "user:info", ignoreException = false)
  public void evictUserNoKey(long userId, String[] lastName, User user) {}

  @CacheEvict(value = "user:info", allEntries = true, ignoreException = false)
  public void evictAllUser() {}
}
