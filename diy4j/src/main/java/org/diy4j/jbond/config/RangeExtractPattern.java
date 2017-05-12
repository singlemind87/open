package org.diy4j.jbond.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RangeExtractPattern {

  private final Pattern startRegex;
  private final String[] startTagArr;
  private final Pattern[] requiredRegex;
  private final String[] requiredTagArr;
  private final Pattern[] optionalRegex;
  private final String[] optionalTagArr;
  private final Pattern endRegex;
  private final String[] endTagArr;
  private final Pattern[] excludeRegex;
  /** count of required regex pattern. */
  private final int requiredRegexCount;

  private List<String[]> tagValPairList = null;
  private HashSet<String> matchedRequiredRegex = null;
  private State matchingState = State.NO_MATCH;
  /** count of accepted elements. */
  private int count = 0;

  private RangeExtractPattern(Pattern startRegex,
      Pattern[] required,
      Pattern[] optional,
      Pattern[] exclude,
      Pattern end,
      String[] startTagArr,
      String[] requiredTagArr,
      String[] optionalTagArr,
      String[] endTagArr,
      int requiredRegexCount) {
    this.startRegex = startRegex;
    this.requiredRegex = required;
    this.optionalRegex = optional;
    this.excludeRegex = exclude;
    this.endRegex = end;
    this.startTagArr = startTagArr;
    this.requiredTagArr = requiredTagArr;
    this.optionalTagArr = optionalTagArr;
    this.endTagArr = endTagArr;
    this.requiredRegexCount = requiredRegexCount;
  }

  public RangeExtractPattern(String start,
      String[] required,
      String[] optional,
      String[] exclude,
      String end) {

    Objects.requireNonNull(start);
    Objects.requireNonNull(end);

    // ---------------------------------------------------------------------
    // create pattern instance.
    // ---------------------------------------------------------------------
    this.startRegex = Pattern.compile(start);
    this.endRegex = Pattern.compile(end);
    // instance for required pattern
    List<Pattern> tmpPatternList = new ArrayList<>();
    HashSet<String> requiredRegexText = new HashSet<>();
    if (required != null) {
      for (final String pattern : required) {
        if (pattern != null) {
          tmpPatternList.add(Pattern.compile(pattern));
          requiredRegexText.add(pattern.toString());
        }
      }
    }
    this.requiredRegexCount = requiredRegexText.size();
    requiredRegexText = null;
    this.requiredRegex = tmpPatternList.toArray(new Pattern[tmpPatternList.size()]);
    tmpPatternList.clear();

    // instance for optional pattern
    if (optional != null) {
      for (final String pattern : optional) {
        if (pattern != null) {
          tmpPatternList.add(Pattern.compile(pattern));
        }
      }
    }
    this.optionalRegex = tmpPatternList.toArray(new Pattern[tmpPatternList.size()]);
    tmpPatternList.clear();

    // instance for excluding pattern
    if (exclude != null) {
      for (final String pattern : exclude) {
        if (pattern != null) {
          tmpPatternList.add(Pattern.compile(pattern));
        }
      }
    }
    this.excludeRegex = tmpPatternList.toArray(new Pattern[tmpPatternList.size()]);
    tmpPatternList = null;

    // ---------------------------------------------------------------------
    // collect capturing tag
    // ---------------------------------------------------------------------
    // tags in start pattern
    this.startTagArr = ConfigUtils.extractLabel4Capture(start);
    Arrays.sort(this.startTagArr);

    // tags in required pattern
    final HashSet<String> tmpTagSet = new HashSet<>();
    if (required != null) {
      for (final String regex : required) {
        final String[] tmpTagArr = ConfigUtils.extractLabel4Capture(regex);
        if (tmpTagArr != null) {
          for (final String tag : tmpTagArr) {
            if (tag != null) {
              tmpTagSet.add(tag);
            }
          }
        }
      }
    }
    if (!tmpTagSet.isEmpty()) {
      this.requiredTagArr = tmpTagSet.toArray((new String[tmpTagSet.size()]));
      Arrays.sort(this.requiredTagArr);
    } else {
      this.requiredTagArr = null;
    }
    tmpTagSet.clear();

    // tags in optional pattern
    if (optional != null) {
      for (final String regex : optional) {
        final String[] tmpTagArr = ConfigUtils.extractLabel4Capture(regex);
        if (tmpTagArr != null) {
          for (final String tag : tmpTagArr) {
            if (tag != null) {
              tmpTagSet.add(tag);
            }
          }
        }
      }
    }
    if (!tmpTagSet.isEmpty()) {
      this.optionalTagArr = tmpTagSet.toArray(new String[tmpTagSet.size()]);
      Arrays.sort(this.optionalTagArr);
    } else {
      this.optionalTagArr = null;
    }

    // tags in end pattern
    this.endTagArr = ConfigUtils.extractLabel4Capture(end);
    Arrays.sort(this.endTagArr);
  }

  public boolean containsTag(String tag) {
    if (this.startTagArr != null && Arrays.binarySearch(this.startTagArr, tag) >= 0) {
      return true;
    } else if (this.requiredTagArr != null && Arrays.binarySearch(this.requiredTagArr, tag) >= 0) {
      return true;
    } else if (this.optionalTagArr != null && Arrays.binarySearch(this.optionalTagArr, tag) >= 0) {
      return true;
    } else if (this.endTagArr != null && Arrays.binarySearch(this.endTagArr, tag) >= 0) {
      return true;
    } else {
      return false;
    }
  }

  public ExtractInfo importAndGetTagValPairIfTarget(String element) {

    final ExtractInfo result;
    Matcher m = null;
    boolean firstMatch = false;

    if (this.matchingState == State.NO_MATCH) {
      // check whether the element is start element or not. if the element is start element, collect tags.
      m = this.startRegex.matcher(element);
      if (m.matches()) {
        this.tagValPairList = new ArrayList<>();
        extractAndCollect(m, element, this.startTagArr, this.tagValPairList);
        this.matchingState = State.START_FOUND;
        firstMatch = true;
      } else {
        return null;
      }
    }

    if (isExclusionPattern(element)) {
      resetState();
      return null;
    }

    this.count++;

    collectIfRequiredMatch(element);

    collectIfOptionalMatch(element);

    if (firstMatch) {
      result = null;

    } else {

      m = this.endRegex.matcher(element);
      if (m.matches()) {
        extractAndCollect(m, element, this.endTagArr, this.tagValPairList);
        if (this.requiredRegexCount == 0 || (this.matchedRequiredRegex != null
            && this.matchedRequiredRegex.size() >= this.requiredRegexCount)) {
          result = new ExtractInfo(this.count, this.tagValPairList);
        } else {
          result = null;
        }

        resetState();

        // in case end edge is also new start edge, check whether or not the element is start edge.
        m = this.startRegex.matcher(element);
        if (m.matches()) {
          this.tagValPairList = new ArrayList<>();
          extractAndCollect(m, element, this.startTagArr, this.tagValPairList);
          this.matchingState = State.START_FOUND;
          this.count++;

          collectIfRequiredMatch(element);

          collectIfOptionalMatch(element);
        }

      } else {
        result = null;
      }
    }

    return result;
  }

  private boolean isExclusionPattern(String element) {
    if (element == null) {
      return false;
    }
    if (this.excludeRegex == null) {
      return false;
    }
    for (final Pattern pattern : this.excludeRegex) {
      final Matcher m = pattern.matcher(element);
      if (m.matches()) {
        return true;
      }
    }

    return false;
  }

  private void resetState() {
    this.tagValPairList = null;
    this.matchingState = State.NO_MATCH;
    this.matchedRequiredRegex = null;
    this.count = 0;
  }

  private void collectIfOptionalMatch(String element) {
    if (this.optionalRegex != null) {
      for (final Pattern pattern : this.optionalRegex) {
        final Matcher m = pattern.matcher(element);
        if (m.matches()) {
          extractAndCollect(m, element, this.optionalTagArr, this.tagValPairList);
        }
      }
    }
  }

  private void collectIfRequiredMatch(String element) {
    if (this.requiredRegex != null) {
      for (final Pattern pattern : this.requiredRegex) {
        final Matcher m = pattern.matcher(element);
        if (m.matches()) {
          extractAndCollect(m, element, this.requiredTagArr, this.tagValPairList);
          if (this.matchedRequiredRegex == null) {
            this.matchedRequiredRegex = new HashSet<>();
          }
          this.matchedRequiredRegex.add(pattern.toString());
        }
      }
    }
  }

  private void extractAndCollect(Matcher m, String target, String[] keyTagArr,
      List<String[]> resultStore) {
    Objects.requireNonNull(target);

    if (keyTagArr == null || keyTagArr.length == 0) {
      return;
    }

    // collect value bound to tag
    for (final String tag : keyTagArr) {
      if (m.pattern().pattern().contains(tag)) {
        final String text = m.group(tag);
        if (tag != null && text != null) {
          final String[] pair = { tag, text };
          if (resultStore == null) {
            resultStore = new ArrayList<>();
          }
          resultStore.add(pair);
        }
      }
    }
  }

  public RangeExtractPattern createClone() {
    return new RangeExtractPattern(this.startRegex,
        this.requiredRegex,
        this.excludeRegex,
        this.excludeRegex,
        this.endRegex,
        this.startTagArr,
        this.requiredTagArr,
        this.optionalTagArr,
        this.endTagArr,
        this.requiredRegexCount);
  }

  private static enum State {
    NO_MATCH, START_FOUND
  }

  public static class ExtractInfo {
    private final int count;
    private final List<String[]> tagValPairList;

    private ExtractInfo(int count, List<String[]> tagValPairs) {
      this.count = count;
      this.tagValPairList = tagValPairs;
    }

    public int getWidth() {
      return this.count;
    }

    public List<String[]> getTagValPairList() {
      return this.tagValPairList;
    }
  }
}
