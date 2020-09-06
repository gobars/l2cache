package com.github.gobars.l2cache.demo;

import com.github.gobars.l2cache.web.service.DemoService;
import com.github.gobars.l2cache.web.utils.Person;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DemoServiceTest {
  @Autowired private DemoService demoService;

  @Test
  public void testSave() {
    Person p = new Person(1, "name1", 12, "address1");
    demoService.save(p);

    Person person = demoService.findOne(p);
    Assert.assertEquals(person.getId(), 1);
  }

  @Test
  public void testRemove() {
    Person p = new Person(5, "name1", 12, "address1");
    demoService.save(p);

    demoService.remove(5L);
    Person person = demoService.findOne(p);
    Assert.assertEquals(person.getId(), 2);
  }

  @Test
  public void testRemoveAll() throws InterruptedException {
    Person p = new Person(6, "name1", 12, "address1");
    demoService.save(p);

    Person person = demoService.findOne(p);
    Assert.assertEquals(person.getId(), 6);
    demoService.removeAll();

    Thread.sleep(1000);
    person = demoService.findOne(p);
    Assert.assertEquals(person.getId(), 2);
  }

  @Test
  public void testFindOne() {
    Person p = new Person(2);
    demoService.findOne(p);
    Person person = demoService.findOne(p);
    Assert.assertEquals(person.getName(), "name2");
  }
}
