package org.diy4j.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

public class TextFileSequencialTracer implements Iterable<String> {

  private final BufferedReader reader;

  /**
   * Constructor.
   * @param reader  caller must close reader.
   * */
  public TextFileSequencialTracer(Reader reader) {
    if (reader instanceof BufferedReader) {
      this.reader = (BufferedReader) reader;
    } else {
      this.reader = new BufferedReader(reader);
    }
  }

  @Override
  public Iterator<String> iterator() {
    return new Itr();
  }

  private class Itr implements Iterator<String> {

    private String nextLine = "";

    @Override
    public boolean hasNext() {
      try {
        this.nextLine = TextFileSequencialTracer.this.reader.readLine();
        return this.nextLine != null;
      } catch (final IOException e) {
        e.printStackTrace();
      }
      return false;
    }

    @Override
    public String next() {
      return this.nextLine;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }
}
