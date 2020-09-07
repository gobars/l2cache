package com.github.gobars.l2cache.web.service;

import com.github.gobars.l2cache.aspect.annotation.*;
import com.github.gobars.l2cache.web.utils.Person;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DemoService {
  @CachePut(value = "cache-prefix:people", key = "#person.id", desc = "用户信息缓存")
  public Person save(Person person) {
    log.info("为id、key为:{}数据做了缓存", person.getId());
    return null;
  }

  @CacheEvict(value = "cache-prefix:people", key = "#id") // 2
  public void remove(Long id) {
    log.info("删除了id、key为{}的数据缓存", id);
    // 这里不做实际删除操作
  }

  @CacheEvict(value = "cache-prefix:people", allEntries = true) // 2
  public void removeAll() {
    log.info("删除了所有缓存的数据缓存");
    // 这里不做实际删除操作
  }

  @SneakyThrows
  @Cacheable(value = "cache-prefix:people", key = "#person.id", desc = "用户信息缓存", l1 = @L1, l2 = @L2)
  public Person findOne(Person person) {
    Person p = new Person(person.getId(), "name2", 12, "address2");
    log.info("为id、key为:{}数据做了缓存", p.getId());
    Thread.sleep(850);
    return p;
  }
}
