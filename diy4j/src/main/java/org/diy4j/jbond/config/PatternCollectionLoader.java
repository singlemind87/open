package org.diy4j.jbond.config;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.diy4j.jbond.annotation.PatternCollection;
import org.diy4j.jbond.annotation.PatternSet;

public final class PatternCollectionLoader implements AnnotationLoader {

  private final AnnotationLoader configLoader = new PatternSetLoader();

  @Override
  public List<ExtractPattern> load(Annotation annotation) {

    final List<ExtractPattern> extractorList;
    final PatternCollection patternCollection = annotation
        .annotationType() == PatternCollection.class ? (PatternCollection) annotation : null;
    if (patternCollection != null) {
      extractorList = new ArrayList<>();

      // read config
      final PatternSet[] patternSetArr = patternCollection.value();
      for (final PatternSet ps : patternSetArr) {
        final List<ExtractPattern> tmp = this.configLoader.load(ps);
        if (tmp != null && !tmp.isEmpty()) {
          extractorList.addAll(tmp);
        }
      }
    } else {
      extractorList = Collections.emptyList();
    }

    return extractorList;
  }

}
