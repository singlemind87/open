package org.diy4j.jbond;

public class BindException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public BindException(String message) {
    super(message);
  }

  public BindException(Throwable cause) {
    super(cause);
  }
}
