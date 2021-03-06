package org.diy4j.jbond.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RangePattern {
  String start();

  String[] required() default {};

  String[] optional() default {};

  String[] excluded() default {};

  String end();
}
