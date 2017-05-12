package org.diy4j.jbond;

import java.util.Objects;

public final class Unmarshallers {

  private Unmarshallers() {
  }

  /**
   * get unmarshaller to use annoatted class for config.
   * @param clazz config
   * @return unmarshaller
   */
  public static <T> Unmarshaller<T> getTextUnmarshaller(Class<T> clazz) {

    Objects.requireNonNull(clazz);

    final Unmarshaller<T> u = UnmarshallerFactory4AnnotatedClass.getInstance()
        .getUnmarshaller(clazz);

    return u;
  }

}
