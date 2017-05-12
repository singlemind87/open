package org.diy4j.jbond;

import java.lang.annotation.Annotation;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import org.diy4j.jbond.annotation.BindTag;
import org.diy4j.jbond.annotation.Pattern;
import org.diy4j.jbond.annotation.PatternCollection;
import org.diy4j.jbond.annotation.PatternSet;
import org.diy4j.jbond.annotation.RangePattern;
import org.diy4j.jbond.config.AnnotationLoader;
import org.diy4j.jbond.config.BindConfig;
import org.diy4j.jbond.config.ExtractPattern;
import org.diy4j.jbond.config.PatternCollectionLoader;
import org.diy4j.jbond.config.PatternLoader;
import org.diy4j.jbond.config.PatternSetLoader;
import org.diy4j.jbond.config.RangeExtractPattern;

final class UnmarshallerFactory4AnnotatedClass {

  private static final Logger LOG = Logger
      .getLogger(UnmarshallerFactory4AnnotatedClass.class.getName());
  private static final Cache cache = new Cache();
  private static final UnmarshallerFactory4AnnotatedClass ME
                                        = new UnmarshallerFactory4AnnotatedClass();

  private final Map<Class<?>, AnnotationLoader> loaderMap;

  private UnmarshallerFactory4AnnotatedClass() {
    this.loaderMap = new HashMap<>();
    this.loaderMap.put(PatternCollection.class, new PatternCollectionLoader());
    this.loaderMap.put(PatternSet.class, new PatternSetLoader());
    this.loaderMap.put(Pattern.class, new PatternLoader());
  }

  public static final UnmarshallerFactory4AnnotatedClass getInstance() {
    return ME;
  }

  public <T> Unmarshaller<T> getUnmarshaller(Class<T> clazz) {

    Objects.requireNonNull(clazz);

    UnmarshallerImpl<T> obj = cache.get(clazz);
    if (obj != null) {
      LOG.config("Return cached value. Key class=" + clazz);
      return obj;
    }

    final BindConfig<T> config = new BindConfig<>(clazz);

    // class section annotation
    final Annotation[] topLevelAnnoArray = clazz.getDeclaredAnnotations();
    RangeExtractPattern rangeExtractPattern = null;
    for (final Annotation anno : topLevelAnnoArray) {
      if (this.loaderMap.containsKey(anno.annotationType())) {
        final AnnotationLoader tmpLoader = this.loaderMap.get(anno.annotationType());
        for (final ExtractPattern ex : tmpLoader.load(anno)) {
          if (ex != null) {
            config.registerExtractConfig(ex);
          }
        }
      }
      if (RangePattern.class.equals(anno.annotationType())) {
        final RangePattern rp = (RangePattern) anno;
        rangeExtractPattern = new RangeExtractPattern(rp.start(), rp.required(), rp.optional(),
            rp.excluded(), rp.end());
      }
      config.registerExtractConfig(rangeExtractPattern);
    }

    // field section annotation
    final Field[] fieldArr = clazz.getDeclaredFields();
    for (final Field f : fieldArr) {
      final BindTag annBindTag = f.getAnnotation(BindTag.class);
      if (annBindTag != null) {
        f.setAccessible(true);
        ;
        config.registerBindConfig(annBindTag.value(), f);
      }
    }

    obj = new UnmarshallerImpl<>(config);
    cache.putIfAbsent(clazz, obj);

    return obj;
  }

  private static class Cache {

    private static final Map<Class<?>, SoftReference<UnmarshallerImpl<?>>> cache = new HashMap<>();

    public <E> void putIfAbsent(Class<E> clazz, UnmarshallerImpl<E> value) {
      if (clazz == null || value == null) {
        return;
      }

      synchronized (clazz) {
        if (cache.containsKey(clazz)) {
          final SoftReference<UnmarshallerImpl<?>> oldRef = cache.get(clazz);
          final UnmarshallerImpl<?> oldValue = oldRef.get();
          if (oldValue != null) {
            return;
          }
        }
        cache.put(clazz, new SoftReference<UnmarshallerImpl<?>>(value));
      }

    }

    public <E> UnmarshallerImpl<E> get(Class<E> clazz) {
      if (clazz == null) {
        return null;
      }

      synchronized (clazz) {
        final SoftReference<UnmarshallerImpl<?>> oldRef = cache.get(clazz);
        if (oldRef == null) {
          return null;
        }
        @SuppressWarnings("unchecked")
        final UnmarshallerImpl<E> oldValue = (UnmarshallerImpl<E>) oldRef.get();
        return oldValue;
      }
    }
  }
}
