package org.diy4j.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * fixedsize-array implementation of <code>List</code>.<br>
 * This capacity of list is specified with constructor.
 * If this container is filled to capacity, the oldest element is discarded and
 * new elements is added
 * when <code>add</code> method is invoked.
 */
public final class CyclicList<E> implements List<E> {

  private final int capacity;

  /** cyclic array. */
  private final E[] arr;

  /** The oldedst value's index. */
  private int head = 0;

  /** The index at which the next element would be added to the tail of the list (via add(E)). */
  private int tail = 0;

  /** The element count in arr. */
  private int size = 0;

  private int modificateCounter = 0;

  /**
   * Constructor.
   * This Constructor is equivalent to {@link #FixedArrayList(size)} where size=0 .
   */
  public CyclicList() {
    this(0);
  }

  /**
   * Constructor.
   *
   * @param size  capacity
   */
  public CyclicList(int size) {
    this.arr = allocateArray(size);
    this.capacity = size;
  }

  /**
   * Constructor.
   *
   * @param c values
   */
  public CyclicList(Collection<? extends E> c) {
    if (c == null) {
      this.arr = allocateArray(0);
      this.capacity = 0;
    } else {
      this.arr = allocateArray(c.size());
      this.capacity = c.size();
      for (final E e : c) {
        add(e);
      }
    }
  }

  private E[] allocateArray(int size) {
    @SuppressWarnings("unchecked")
    final E[] result = (E[]) new Object[size];
    return result;
  }

  /**
   * calculate position in cyclic array.
   * @param base   base index.
   * @param delta  value added to base
   * @return result
   */
  private int cyclicAdd(int base, int delta) {
    int position = (base + delta) % this.capacity;
    if (position < 0) {
      position += this.capacity;
    }
    return position;
  }

  @Override
  public final int size() {
    return this.size;
  }

  @Override
  public final boolean isEmpty() {
    return this.size == 0;
  }

  @Override
  public final boolean contains(Object o) {
    final int isize = this.size;
    if (isize == 0) {
      return false;
    }
    int position = this.head;
    for (int i = 0; i < isize; i++) {
      final E e = get(position);
      if (Objects.equals(o, e)) {
        return true;
      }
      position = cyclicAdd(position, 1);
    }
    return false;
  }

  @Override
  public final Iterator<E> iterator() {
    return new Itr();
  }

  /**
   * Copies the elements from our element array into the specified array, in order (from
   * first to last element in the list).
   * It is assumed that the array is large enough to hold all elements in the list
   * @param a array
   * @return copy of the list.
   */
  private <T> T[] copyElements(T[] a) {
    if (this.head < this.tail) {
      System.arraycopy(this.arr, this.head, a, 0, this.size);
    } else {
      final int headPortionLen = this.capacity - this.head;
      System.arraycopy(this.arr, this.head, a, 0, headPortionLen);
      System.arraycopy(this.arr, 0, a, headPortionLen, this.tail);
    }
    return a;
  }

  @Override
  public final Object[] toArray() {
    final int isize = this.size;
    final Object[] result = (Object[]) Array.newInstance(this.arr.getClass().getComponentType(),
        isize);
    if (isize == 0) {
      return result;
    }
    copyElements(result);
    return result;
  }

  @Override
  public final <T> T[] toArray(T[] a) {
    Objects.requireNonNull(a);
    final int isize = this.size;
    if (a.length < isize) {
      @SuppressWarnings("unchecked")
      final T[] tmp = (T[]) Array.newInstance(a.getClass().getComponentType(), isize);
      a = tmp;
    }
    copyElements(a);
    if (a.length > isize) {
      a[isize] = null;
    }
    return a;
  }

  @Override
  public final boolean add(E e) {
    if (this.capacity == 0) {
      return false;
    }
    this.arr[this.tail] = e;
    this.tail = cyclicAdd(this.tail, 1);
    if (this.size < this.capacity) {
      this.size++;

    } else {
      this.head = cyclicAdd(this.head, 1);
    }
    this.modificateCounter++;
    return true;
  }

  @Override
  public final void add(int index, E element) {
    // TODO implement
    this.modificateCounter++;
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean remove(Object o) {
    // TODO implement
    this.modificateCounter++;
    throw new UnsupportedOperationException();
  }

  @Override
  public final E remove(int index) {
    // TODO implement
    this.modificateCounter++;
    throw new UnsupportedOperationException();
  }

  /**
   * Remove head element.
   * @return old head element.
   */
  public final E shift() {
    if (isEmpty()) {
      return null;
    }
    final E result = this.arr[this.head];
    this.size--;
    this.head = cyclicAdd(this.head, 1);

    return result;
  }

  @Override
  public final boolean containsAll(Collection<?> c) {
    if (c == null) {
      return false;
    }
    for (final Object e : c) {
      if (!contains(e)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public final boolean addAll(Collection<? extends E> c) {
    if (c == null || c.isEmpty()) {
      return false;
    }
    @SuppressWarnings("unchecked")
    final E[] a = c
        .toArray((E[]) Array.newInstance(this.arr.getClass().getComponentType(), c.size()));

    if (a.length < this.capacity) {
      final int newTail = cyclicAdd(this.tail, a.length);
      if (this.head < this.tail) {
        if (newTail <= this.head) {
          final int portionLen = this.capacity - this.tail;
          System.arraycopy(a, 0, this.arr, this.tail, portionLen);
          System.arraycopy(a, portionLen, this.arr, 0, a.length - portionLen);
          this.tail = newTail;
          this.size = this.capacity - this.head + newTail;

        } else {
          System.arraycopy(a, 0, this.arr, this.tail, a.length);
          this.tail = newTail;
          this.size = this.capacity - newTail + 1;
        }

      } else {
        if (a.length <= (this.capacity - this.tail)) {
          System.arraycopy(a, 0, this.arr, this.tail, a.length);
          if (newTail < this.head) {
            this.size = this.capacity - this.head + newTail;
          } else {
            this.head = newTail;
            this.size = this.capacity;
          }
          this.tail = newTail;

        } else {
          final int portionLen = this.capacity - this.tail;
          System.arraycopy(a, 0, this.arr, this.tail, portionLen);
          System.arraycopy(a, portionLen, this.arr, 0, a.length - portionLen);
          this.head = newTail;
          this.tail = newTail;
          this.size = this.capacity;
        }
      }

    } else {
      System.arraycopy(a, a.length - this.capacity, this.arr, 0, this.capacity);
      this.head = 0;
      this.tail = 0;
      this.size = this.capacity;
    }

    this.modificateCounter++;

    return true;
  }

  @Override
  public final boolean addAll(int index, Collection<? extends E> c) {
    // TODO implement
    this.modificateCounter++;
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean removeAll(Collection<?> c) {
    // TODO implement
    this.modificateCounter++;
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean retainAll(Collection<?> c) {
    // TODO implement
    this.modificateCounter++;
    throw new UnsupportedOperationException();
  }

  @Override
  public final void clear() {
    Arrays.fill(this.arr, null);
    this.head = 0;
    this.tail = 0;
    this.size = 0;
    this.modificateCounter++;
  }

  private void indexCheck(int index) {
    final int isize = size();
    if (isize == 0 || index < 0 || index >= isize) {
      throw new IndexOutOfBoundsException("index=" + index + ", array size=" + isize);
    }
  }

  private int toRelativeIndex(int index) {
    int result = cyclicAdd(this.head, index);
    return result;
  }

  @Override
  public final E get(int index) {
    indexCheck(index);
    final int relativeIndex = toRelativeIndex(index);
    final E e = this.arr[relativeIndex];
    return e;
  }

  @Override
  public final E set(int index, E element) {
    indexCheck(index);
    final int relativeIndex = toRelativeIndex(index);
    final E old = this.arr[relativeIndex];
    this.arr[relativeIndex] = element;
    return old;
  }

  @Override
  public final int indexOf(Object o) {
    if (isEmpty()) {
      return -1;
    }
    final int isize = this.size;
    for (int i = 0; i < isize; i++) {
      final E e = get(i);
      if (Objects.equals(e, o)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public final int lastIndexOf(Object o) {
    if (isEmpty()) {
      return -1;
    }
    for (int i = this.size - 1; i >= 0; i--) {
      final E e = get(i);
      if (Objects.equals(e, o)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public final ListIterator<E> listIterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public final ListIterator<E> listIterator(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final List<E> subList(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return "CyclicList [capacity=" + this.capacity + ", arr=" + Arrays.toString(this.arr)
        + ", head=" + this.head + ", tail=" + this.tail + ", size=" + this.size
        + ", modificateCounter=" + this.modificateCounter + "]";
  }

  private final class Itr implements Iterator<E> {

    private final int modificatedCounterSnapShot;
    private int posi;
    private int remain;

    public Itr() {
      this.modificatedCounterSnapShot = CyclicList.this.modificateCounter;
      this.posi = CyclicList.this.head;
      this.remain = CyclicList.this.size;
    }

    @Override
    public final boolean hasNext() {
      if (this.remain <= 0) {
        return false;
      }
      return true;
    }

    @Override
    public final E next() {
      if (this.modificatedCounterSnapShot != CyclicList.this.modificateCounter) {
        throw new ConcurrentModificationException();
      }

      final E result = CyclicList.this.arr[this.posi];

      this.posi = cyclicAdd(this.posi, 1);
      this.remain--;

      return result;
    }

    @Override
    public String toString() {
      return "Itr [modificatedCounterSnapShot=" + this.modificatedCounterSnapShot + ", itrHead="
          + this.posi + ", remain=" + this.remain + "]";
    }

  }
}
