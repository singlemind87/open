package org.diy4j.jbond.config;

import java.lang.annotation.Annotation;
import java.util.List;

public interface AnnotationLoader {
  List<ExtractPattern> load(Annotation annotation);
}
