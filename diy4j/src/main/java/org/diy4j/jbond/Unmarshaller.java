package org.diy4j.jbond;

import java.util.List;

public interface Unmarshaller<T> {

  List<T> unmarshal(List<String> list);

  List<T> unmarshal(String filePath);

  List<T> unmarshal(String filePath, String charsetName);

}
