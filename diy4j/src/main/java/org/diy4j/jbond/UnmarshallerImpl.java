package org.diy4j.jbond;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.diy4j.jbond.config.BindConfig;
import org.diy4j.jbond.config.BindConfig.Tag;
import org.diy4j.jbond.config.ExtractPattern;
import org.diy4j.jbond.config.RangeExtractPattern;
import org.diy4j.jbond.config.RangeExtractPattern.ExtractInfo;
import org.diy4j.util.CyclicList;
import org.diy4j.util.TextFileSequencialTracer;

final class UnmarshallerImpl<T> implements Unmarshaller<T> {


  private final Map<String, Field> taggedFieldMap;
  private final Field countField;
  private final ExtractPattern[] extractorArr;
  private final RangeExtractPattern templateRangeExtractor;
  private final Class<T> clazz;
  private final int maxScanSize;

  public UnmarshallerImpl(BindConfig<T> config) throws BindConfigException {

    Objects.requireNonNull(config);

    final Class<T> clazz = config.getBindClass();
    final Map<String, Field> taggedFieldMap = config.getTaggedFieldMap();
    final List<ExtractPattern> extractorList = config.getExtractorList();
    final RangeExtractPattern rangeExtractPattern = config.getRangeExtractor();

    Objects.requireNonNull(clazz);
    Objects.requireNonNull(taggedFieldMap);

    if (extractorList == null) {
      Objects.requireNonNull(rangeExtractPattern);
    }
    if (rangeExtractPattern == null) {
      Objects.requireNonNull(extractorList);
      if (extractorList.isEmpty()) {
        throw new BindConfigException("absent instance.");
      }
    }

    config.test();

    int maxLineCount = 0;
    if (extractorList != null) {
      for (final ExtractPattern ex : extractorList) {
        if (ex != null) {
          final int lineCount = ex.windowWidth();
          if (lineCount > maxLineCount) {
            maxLineCount = lineCount;
          }
        }
      }
    }
    this.maxScanSize = maxLineCount;

    this.clazz = clazz;
    if (extractorList != null && !extractorList.isEmpty()) {
      this.extractorArr = extractorList.toArray(new ExtractPattern[extractorList.size()]);
    } else {
      this.extractorArr = null;
    }
    this.templateRangeExtractor = rangeExtractPattern;
    this.taggedFieldMap = new HashMap<>(taggedFieldMap);
    if (taggedFieldMap.containsKey(Tag.COUNTER)) {
      this.countField = taggedFieldMap.get(Tag.COUNTER);
    } else {
      this.countField = null;
    }

  }

  private List<T> unmarshalImpl(Iterable<String> iterable) {

    if (iterable == null) {
      return Collections.emptyList();
    }

    final List<T> result = new ArrayList<>();

    final CyclicList<String> buffer;
    if (this.extractorArr != null) {
      buffer = new CyclicList<>(this.maxScanSize);
      for (int i = 0; i < this.maxScanSize; i++) {
        buffer.add(null);
      }
    } else {
      buffer = null;
    }

    final RangeExtractPattern rangeExtractor;
    if (this.templateRangeExtractor != null) {
      // create new local instance to maintain threadsafe.
      rangeExtractor = this.templateRangeExtractor.createClone();
    } else {
      rangeExtractor = null;
    }

    try {
      long blockCount = 1;

      for (final String line : iterable) {
        if (buffer != null) {
          buffer.add(line);

          for (final ExtractPattern extractor : this.extractorArr) {
            final T entity = newIfTarget(extractor, buffer, blockCount);
            if (entity != null) {
              result.add(entity);
            }
          }
        }
        if (rangeExtractor != null) {
          final T entity = newIfTarget(rangeExtractor, line, blockCount);
          if (entity != null) {
            result.add(entity);
          }
        }
        blockCount++;
      }

      if (buffer != null) {
        while (buffer.size() > 0) {
          buffer.shift();

          for (final ExtractPattern extractor : this.extractorArr) {
            final T entity = newIfTarget(extractor, buffer, blockCount);
            if (entity != null) {
              result.add(entity);
            }
          }
          blockCount++;
        }
      }
    } catch (IllegalAccessException | InstantiationException e) {
      throw new BindException(e);
    }

    return result;
  }

  @Override
  public List<T> unmarshal(List<String> list) {

    if (list == null) {
      return Collections.emptyList();
    }

    final List<T> result = unmarshalImpl(list);
    return result;
  }

  @Override
  public List<T> unmarshal(String filePath) {

    if (filePath == null) {
      return Collections.emptyList();
    }
    final List<T> result = unmarshal(filePath, Charset.defaultCharset().toString());

    return result;
  }

  @Override
  public List<T> unmarshal(String filePath, String charsetName) {

    if (filePath == null) {
      return Collections.emptyList();
    }

    String charset = charsetName;
    if (charset == null) {
      charset = Charset.defaultCharset().toString();
    }

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(new FileInputStream(filePath), charset))) {
      final TextFileSequencialTracer fileContentTracer = new TextFileSequencialTracer(reader);

      final List<T> result = unmarshalImpl(fileContentTracer);

      return result;

    } catch (final IOException e) {
      throw new BindException(e);
    }
  }

  private T newIfTarget(ExtractPattern extractor, CyclicList<String> buffer, long blockCount)
      throws IllegalAccessException, InstantiationException {

    if (extractor == null || buffer == null) {
      return null;
    }

    final List<String[]> tagValPairList = extractor.toTagValPair(buffer);

    final T result;
    if (tagValPairList != null) {
      final long hitBlockIndex = blockCount - extractor.getEndIndex() + extractor.getBeginIndex();
      result = createInstance(hitBlockIndex, tagValPairList);

    } else {
      result = null;
    }

    return result;
  }

  private T newIfTarget(RangeExtractPattern extractor, String target, long blockCount)
      throws IllegalAccessException, InstantiationException {

    if (extractor == null) {
      return null;
    }

    final ExtractInfo extractInfo = extractor.importAndGetTagValPairIfTarget(target);

    final T result;
    if (extractInfo != null && extractInfo.getTagValPairList() != null) {
      final long hitBlockIndex = blockCount - extractInfo.getWidth() + 1;
      result = createInstance(hitBlockIndex, extractInfo.getTagValPairList());
    } else {
      result = null;
    }

    return result;
  }

  private T createInstance(long hitBlockIndex, List<String[]> tagValPairList)
      throws BindException, IllegalAccessException, InstantiationException {

    final T result = this.clazz.newInstance();

    if (this.countField != null) {
      if (long.class.equals(this.countField.getType())) {
        this.countField.setLong(result, hitBlockIndex);
      } else {
        bind(result, this.countField, String.valueOf(hitBlockIndex));
      }
    }

    for (final String[] entry : tagValPairList) {
      if (entry.length < 1) {
        continue;
      }
      final String tag = entry[0];
      final String val = entry[1];

      final Field f = this.taggedFieldMap.get(tag);

      // bind
      bind(result, f, val);
    }

    return result;
  }

  private boolean bind(T entity, Field f, String value)
      throws BindException, IllegalAccessException {

    if (entity == null || f == null || value == null) {
      return false;
    }

    final Class<?> fClass = f.getType();

    try {
      if (fClass == List.class) {

        final ParameterizedType listType = (ParameterizedType) f.getGenericType();
        final Class<?> elementType = (Class<?>) listType.getActualTypeArguments()[0];
        final Method method = fClass.getDeclaredMethod("add", Object.class);
        List<?> listMember = (List<?>) f.get(entity);
        if (listMember == null) {
          listMember = new ArrayList<>();
          f.set(entity, listMember);
        }
        final Object convertedValue = parse(elementType, value);
        method.invoke(listMember, convertedValue);

      } else {
        final Object convertedValue = parse(fClass, value);
        if (convertedValue != null) {
          f.set(entity, convertedValue);
        }
      }
    } catch (final NumberFormatException e1) {
      throw new BindException("Fail binding to field.");
    } catch (NoSuchMethodException | SecurityException | InvocationTargetException e2) {
      throw new BindException(e2);
    }

    return true;
  }

  private Object parse(Class<?> fclass, String value) throws NumberFormatException {

    if (fclass == null || value == null) {
      throw new IllegalArgumentException();
    }

    final Object result;

    if (fclass == String.class) {
      result = value;

    } else if (fclass == Character.class || (!value.isEmpty() && fclass == char.class)) {
      result = Character.valueOf(value.charAt(0));

    } else if (fclass == Boolean.class || (!value.isEmpty() && fclass == boolean.class)) {
      result = Boolean.valueOf(value);

    } else {
      final String trimValue = value.trim();
      if (!trimValue.isEmpty()) {
        try {
          if (fclass == Byte.class || fclass == byte.class) {
            result = Byte.valueOf(trimValue);

          } else if (fclass == Short.class || fclass == short.class) {
            result = Short.valueOf(trimValue);

          } else if (fclass == Integer.class || fclass == int.class) {
            result = Integer.valueOf(trimValue);

          } else if (fclass == Long.class || fclass == long.class) {
            result = Long.valueOf(trimValue);

          } else if (fclass == Float.class || fclass == float.class) {
            result = Float.valueOf(trimValue);

          } else if (fclass == Double.class || fclass == double.class) {
            result = Double.valueOf(trimValue);

          } else {
            throw new BindException("Unsupported Type. " + fclass.getName());
          }
        } catch (final SecurityException e) {
          throw new BindException(e);
        }
      } else {
        result = null;
      }
    }

    return result;
  }
}
