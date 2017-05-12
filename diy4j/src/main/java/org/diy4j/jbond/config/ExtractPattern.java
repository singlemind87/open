package org.diy4j.jbond.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExtractPattern {

  private final PatternConfig[] patternConfigArr;
  private final int minIndex;
  private final int maxIndex;

  public ExtractPattern(PatternConfig... configArr) {
    this.patternConfigArr = configArr;
    int min = Integer.MAX_VALUE;
    int max = 0;
    for (final PatternConfig cfg : this.patternConfigArr) {
      if (cfg != null) {
        final int tmpIndex = cfg.index();
        if (tmpIndex > max) {
          max = tmpIndex;
        }
        if (tmpIndex < min) {
          min = tmpIndex;
        }
      }
    }

    this.minIndex = min;
    this.maxIndex = max;
  }

  public int getBeginIndex() {
    return this.minIndex;
  }

  public int getEndIndex() {
    return this.maxIndex;
  }

  public int windowWidth() {
    if (this.patternConfigArr == null) {
      return 0;
    }
    return this.maxIndex + 1;
  }

  public boolean containsTag(String tag) {
    for (final PatternConfig cfg : this.patternConfigArr) {
      if (cfg.contains(tag)) {
        return true;
      }
    }
    return false;
  }

  public List<String[]> toTagValPair(List<String> buffer) {

    if (buffer == null || this.patternConfigArr == null || this.patternConfigArr.length == 0) {
      return null;
    }

    List<String[]> tagValPairList = null;

    // And Matching
    for (final PatternConfig cfg : this.patternConfigArr) {
      // get captured value
      final String[][] tagValPair = cfg.extract(buffer);
      if (tagValPair != null) {
        if (tagValPairList == null) {
          tagValPairList = new ArrayList<>(2);
        }
        for (final String[] entry : tagValPair) {
          tagValPairList.add(entry);
        }
      } else {
        return null;
      }
    }

    return tagValPairList;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(this.patternConfigArr);
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
    final ExtractPattern other = (ExtractPattern) obj;
    if (!Arrays.equals(this.patternConfigArr, other.patternConfigArr)) {
      return false;
    }
    return true;
  }
}
