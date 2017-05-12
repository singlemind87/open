package org.diy4j.jbond.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ConfigUtils {

  /** regex pattern of label for capturing. */
  private static final Pattern TAG_PATTERN = Pattern.compile("\\(?<([^>]+)>");

  private ConfigUtils() {
  }

  public static String[] extractLabel4Capture(String regex) {
    if (regex == null) {
      return null;
    }

    final Matcher m = TAG_PATTERN.matcher(regex);

    final List<String> tagList = new ArrayList<>(5);
    while (m.find()) {
      // tag found
      if (!tagList.contains(m.group(1))) {
        tagList.add(m.group(1));
      }
    }

    final String[] tagArr = tagList.toArray(new String[tagList.size()]);

    return tagArr;
  }
}
