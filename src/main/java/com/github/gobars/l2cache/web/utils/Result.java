package com.github.gobars.l2cache.web.utils;

import lombok.Data;

/** 与前端交互对象 */
@Data
public class Result {
  private Object data;
  private Status status;
  private String code;
  private String message;

  public static Result success() {
    Result result = new Result();
    result.setCode("200");
    result.setStatus(Status.SUCCESS);
    result.setMessage(Status.SUCCESS.name());
    return result;
  }

  public static Result success(Object data) {
    Result result = new Result();
    result.setCode("200");
    result.setStatus(Status.SUCCESS);
    result.setMessage(Status.SUCCESS.name());
    result.setData(data);
    return result;
  }

  public static Result error(String msg) {
    Result result = new Result();
    result.setCode("500");
    result.setStatus(Status.ERROR);
    result.setMessage(msg);
    return result;
  }

  public enum Status {
    SUCCESS("OK"),
    ERROR("ERROR");

    private String code;

    Status(String code) {
      this.code = code;
    }

    public String code() {
      return this.code;
    }
  }
}
