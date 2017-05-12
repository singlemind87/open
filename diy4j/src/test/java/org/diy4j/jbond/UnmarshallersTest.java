package org.diy4j.jbond;

import java.util.ArrayList;
import java.util.List;

import org.diy4j.jbond.annotation.BindTag;
import org.diy4j.jbond.annotation.Pattern;
import org.diy4j.jbond.annotation.PatternSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UnmarshallersTest {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testGetTextUnmarshaller() {

    Unmarshaller<Sample> sut = Unmarshallers.getTextUnmarshaller(Sample.class);
    List<String> list = new ArrayList<>();
    list.add("A");
    list.add("Bhi");
    list.add("A");
    list.add("A");
    list.add("Bbye");
    List<Sample> actual = sut.unmarshal(list);

    for (Sample e : actual) {
      System.out.println(e);
    }
  }


  @PatternSet({
    @Pattern(index=0, regex="A"),
    @Pattern(index=1, regex="B(?<tag>.*)")
  })
  public static class Sample {

    @BindTag("tag")
    private String text;

    @BindTag("#")
    private long num;

    @Override
    public String toString() {
      return "Sample [text=" + this.text + ", num=" + this.num + "]";
    }
  }
}
