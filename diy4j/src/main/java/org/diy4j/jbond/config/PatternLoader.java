package org.diy4j.jbond.config;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.diy4j.jbond.annotation.Pattern;

public final class PatternLoader implements AnnotationLoader {

  @Override
  public List<ExtractPattern> load(Annotation annotation) {

    if (annotation == null) {
      return Collections.emptyList();
    }

    final Pattern annPattern = (annotation.annotationType() == Pattern.class) ? (Pattern) annotation
        : null;

    final PatternConfig config = convert(annPattern);

    final List<ExtractPattern> result = new ArrayList<>();
    if (config != null) {
      result.add(new ExtractPattern(config));
    }

    return result;
  }

  PatternConfig convert(Pattern p) {
    if (p == null) {
      return null;
    }

    final String[] tagArr = ConfigUtils.extractLabel4Capture(p.regex());
    final PatternConfig cfg = new PatternConfig(p.index(), p.regex(), tagArr);

    return cfg;
  }
}
