package org.diy4j.jbond.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.diy4j.jbond.BindConfigException;

public class BindConfig<T> {

  private static final String DELIMITER = " | ";

  private final Class<T> bindClass;

  private Map<String, Field> taggedFieldMap;
  private List<ExtractPattern> extractorList;
  private RangeExtractPattern rangeExtractor;
  private List<String> violationMsgList;

  public BindConfig(Class<T> clazz) {
    this.bindClass = clazz;
  }

  public void registerExtractConfig(ExtractPattern config) {
    if (config == null) {
      return;
    }
    if (this.extractorList == null) {
      this.extractorList = new ArrayList<>();
    }
    if (!this.extractorList.contains(config)) {
      this.extractorList.add(config);
    }
    if (config.containsTag(Tag.COUNTER)) {
      if (this.violationMsgList == null) {
        this.violationMsgList = new ArrayList<>();
      }
      this.violationMsgList
          .add(Tag.COUNTER + "is reserved tag. you cannot use it as label in regex.");
    }
  }

  public void registerExtractConfig(RangeExtractPattern config) {
    if (config == null) {
      return;
    }
    this.rangeExtractor = config;
    if (config.containsTag(Tag.COUNTER)) {
      if (this.violationMsgList == null) {
        this.violationMsgList = new ArrayList<>();
      }
      this.violationMsgList
          .add(Tag.COUNTER + "is reserved tag. you cannot use it as label in regex.");
    }

  }

  public void registerBindConfig(String tag, Field f) {
    if (tag == null || f == null) {
      return;
    }

    if (this.taggedFieldMap == null) {
      this.taggedFieldMap = new HashMap<>();
    }

    if (this.taggedFieldMap.containsKey(tag)) {
      final Field oldF = this.taggedFieldMap.get(tag);
      if (this.violationMsgList == null) {
        this.violationMsgList = new ArrayList<>();
      }
      this.violationMsgList
          .add(String.format("Tag[%s] bound to Field[%s] of Class[%s] is used for Field[%s].", tag,
              f.getName(), this.bindClass, oldF.getName()));
    }

    this.taggedFieldMap.put(tag, f);
  }

  public void test() throws BindConfigException {
    List<String> unusedTagList = null;
    for (final String tag : this.taggedFieldMap.keySet()) {
      boolean found = false;
      if (this.extractorList != null) {
        for (final ExtractPattern ex : this.extractorList) {
          found |= ex.containsTag(tag);
          if (found) {
            break;
          }
        }
      }
      if (!found && this.rangeExtractor != null) {
        found |= this.rangeExtractor.containsTag(tag);
      }
      if (!found) {
        if (!tag.equals(Tag.COUNTER)) {
          if (unusedTagList == null) {
            unusedTagList = new ArrayList<>();
          }
          unusedTagList.add(tag);
        }
      }
    }
    final StringBuilder sb = new StringBuilder();
    if (this.violationMsgList != null) {
      for (final String msg : this.violationMsgList) {
        sb.append(msg).append(DELIMITER);
      }
    }
    if (unusedTagList != null) {
      sb.append("tag is not used in regex{");
      for (final String tag : unusedTagList) {
        sb.append(tag).append(" ");
      }
      sb.append("}");
    }
    if (sb.length() > 0) {
      throw new BindConfigException(sb.toString());
    }
  }

  public Class<T> getBindClass() {
    return this.bindClass;
  }

  public Map<String, Field> getTaggedFieldMap() {
    return this.taggedFieldMap;
  }

  public List<ExtractPattern> getExtractorList() {
    return this.extractorList;
  }

  public RangeExtractPattern getRangeExtractor() {
    return this.rangeExtractor;
  }

  public static interface Tag {
    String COUNTER = "#";
  }

}
