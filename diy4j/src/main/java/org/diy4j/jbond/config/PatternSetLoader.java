package org.diy4j.jbond.config;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.diy4j.jbond.annotation.Pattern;
import org.diy4j.jbond.annotation.PatternSet;

public final class PatternSetLoader implements AnnotationLoader {

  private final PatternLoader patternLoader = new PatternLoader();

  @Override
  public List<ExtractPattern> load(Annotation annotation) {

    if (annotation == null) {
      return Collections.emptyList();
    }

    final PatternSet patternSet = annotation.annotationType() == PatternSet.class
        ? (PatternSet) annotation : null;
    if (patternSet == null) {
      return Collections.emptyList();
    }

    // read config
    final List<PatternConfig> cfgList = new ArrayList<>(5);
    final Pattern[] patternArr = patternSet.value();
    for (final Pattern p : patternArr) {
      if (p != null) {
        final PatternConfig cfg = this.patternLoader.convert(p);
        if (cfg != null) {
          cfgList.add(cfg);
        }
      }
    }
    final List<ExtractPattern> extractorList = new ArrayList<>();
    if (!cfgList.isEmpty()) {
      final PatternConfig[] arr = cfgList.toArray(new PatternConfig[cfgList.size()]);
      extractorList.add(new ExtractPattern(arr));
    }

    return extractorList;
  }

}
