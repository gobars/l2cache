package com.github.gobars.l2cache.aspect.support;

import lombok.Getter;

/**
 * 抽象的调用缓存操作方法
 *
 * <p>不提供传输已检查异常的方法，但提供了一个特殊异常，该异常应该用于包装底层调用引发的任何异常。 调用者应特别处理此异常类型。
 *
 * @author yuhao.wang3
 */
public interface Invoker {

  /**
   * 调用此实例定义的缓存操作.
   *
   * <p>Wraps any exception that is thrown during the invocation in a {@link InvokeException}.
   *
   * @return the result of the operation
   * @throws InvokeException if an error occurred while invoking the operation
   */
  Object invoke() throws InvokeException;

  /** Wrap any exception thrown while invoking {@link #invoke()}. */
  class InvokeException extends RuntimeException {
    @Getter private final Throwable original;

    public InvokeException(Throwable original) {
      super(original.getMessage(), original);
      this.original = original;
    }
  }
}
