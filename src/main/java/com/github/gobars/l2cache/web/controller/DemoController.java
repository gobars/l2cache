package com.github.gobars.l2cache.web.controller;

import com.github.gobars.l2cache.web.service.DemoService;
import com.github.gobars.l2cache.web.utils.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {
  @Autowired DemoService demoService;

  @RequestMapping("/demo/put")
  public long put(@RequestBody Person person) {
    demoService.save(person);
    return 1L;
  }

  /**
   * 演示缓存API.
   *
   * <p>curl -X POST 'http://127.0.0.1:8080/demo/able' -H 'Content-Type: application/json' --data-raw '{"id":300579}'
   *
   * @param person
   * @return
   */
  @RequestMapping("/demo/able")
  public Person cacheable(@RequestBody Person person) {
    return demoService.findOne(person);
  }

  @RequestMapping("/demo/evit")
  public String evit(@RequestBody Person person) {
    demoService.remove(person.getId());
    return "ok";
  }
}
