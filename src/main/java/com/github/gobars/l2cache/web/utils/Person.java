package com.github.gobars.l2cache.web.utils;

import lombok.Data;

@Data
public class Person {
  private long id;
  private String name;
  private Integer age;
  private String address;

  public Person() {}

  public Person(long id) {
    this.id = id;
  }

  public Person(long id, String name, Integer age, String address) {
    this.id = id;
    this.name = name;
    this.age = age;
    this.address = address;
  }

  public Person(String name, Integer age) {
    this.name = name;
    this.age = age;
  }
}
