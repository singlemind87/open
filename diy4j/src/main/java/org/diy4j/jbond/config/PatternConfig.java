package org.diy4j.jbond.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.diy4j.jbond.BindConfigException;

public class PatternConfig {

  private final int index;
  private final java.util.regex.Pattern pattern;
  private final String[] tagArr;

  public PatternConfig(int index, String pattern, String... tagArr) {
    if (index < 0) {
      throw new BindConfigException("");
    }
    Objects.requireNonNull(pattern);

    this.index = index;
    this.pattern = Pattern.compile(pattern);
    final Set<String> tagSet = new HashSet<>();
    for (final String e : tagArr) {
      tagSet.add(e);
    }
    this.tagArr = tagSet.toArray(new String[tagSet.size()]);
    if (this.tagArr.length != 0) {
      Arrays.sort(this.tagArr);
    }
  }

  protected boolean contains(String tag) {
    if (tag == null) {
      return false;
    }
    if (Arrays.binarySearch(this.tagArr, tag) >= 0) {
      return true;
    } else {
      return false;
    }
  }

  public int index() {
    return this.index;
  }

  public String[][] extract(List<String> buff) {
    if (buff == null) {
      return null;
    }

    if ((this.index + 1) > buff.size()) {
      return null;
    }

    final String target = buff.get(this.index);
    if (target == null) {
      return null;
    }
    final Matcher m = this.pattern.matcher(target);
    if (m.matches()) {
      String[][] result = new String[this.tagArr.length][];
      int matchIndex = -1;
      for (final String tag : this.tagArr) {
        final String text = m.group(tag);
        if (tag != null && text != null) {
          matchIndex++;
          final String[] pair = { tag, text };
          result[matchIndex] = pair;
        }
      }
      final int matchArrSize = matchIndex + 1;
      if (matchArrSize < result.length) {
        result = Arrays.copyOf(result, matchArrSize);
      }
      return result;
    } else {
      return null;
    }
  }

  @Override
  public String toString() {
    return this.index + "," + this.pattern + "," + Arrays.toString(this.tagArr) + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + this.index;
    result = prime * result
        + ((this.pattern.pattern() == null) ? 0 : this.pattern.pattern().hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final PatternConfig other = (PatternConfig) obj;
    if (this.index != other.index) {
      return false;
    }
    if (this.pattern.pattern() == null) {
      if (other.pattern.pattern() != null) {
        return false;
      }
    } else if (!this.pattern.pattern().equals(other.pattern.pattern())) {
      return false;
    }
    return true;
  }

}
