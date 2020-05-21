/*
 * Copyright (C) 2020 Eric Medvet <eric.medvet@gmail.com> (as eric)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.util;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Grid<T> implements Iterable<Grid.Entry<T>>, Serializable {

  public static final class Entry<K> implements Serializable {

    private final int x;
    private final int y;
    private final K value;

    public Entry(int x, int y, K value) {
      this.x = x;
      this.y = y;
      this.value = value;
    }

    public int getX() {
      return x;
    }

    public int getY() {
      return y;
    }

    public K getValue() {
      return value;
    }

    @Override
    public int hashCode() {
      int hash = 7;
      hash = 53 * hash + this.x;
      hash = 53 * hash + this.y;
      hash = 53 * hash + Objects.hashCode(this.value);
      return hash;
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
      final Entry<?> other = (Entry<?>) obj;
      if (this.x != other.x) {
        return false;
      }
      if (this.y != other.y) {
        return false;
      }
      return Objects.equals(this.value, other.value);
    }

  }

  private static final class GridIterator<K> implements Iterator<Entry<K>> {

    private int c = 0;
    private final Grid<K> grid;

    public GridIterator(Grid<K> grid) {
      this.grid = grid;
    }

    @Override
    public boolean hasNext() {
      return c < grid.w * grid.h;
    }

    @Override
    public Entry<K> next() {
      int y = Math.floorDiv(c, grid.w);
      int x = c % grid.w;
      c = c + 1;
      return new Entry<>(x, y, grid.get(x, y));
    }

  }

  private final List<T> ts;
  private final int w;
  private final int h;

  public Grid(int w, int h, T[] t) {
    this.w = w;
    this.h = h;
    ts = new ArrayList<>(w * h);
    for (int i = 0; i < w * h; i++) {
      if ((t != null) && (i < t.length)) {
        ts.add(t[i]);
      } else {
        ts.add(null);
      }
    }
  }

  public T get(int x, int y) {
    if ((x < 0) || (x >= w)) {
      return null;
    }
    if ((y < 0) || (y >= h)) {
      return null;
    }
    return ts.get((y * w) + x);
  }

  public void set(int x, int y, T t) {
    ts.set((y * w) + x, t);
  }

  public int getW() {
    return w;
  }

  public int getH() {
    return h;
  }

  public static <S, T> Grid<T> create(Grid<S> source, Function<S, T> transformerFunction) {
    Grid<T> target = Grid.create(source);
    for (Grid.Entry<S> entry : source) {
      target.set(entry.getX(), entry.getY(), transformerFunction.apply(entry.getValue()));
    }
    return target;
  }

  public static <K> Grid<K> create(int w, int h, K k) {
    return create(w, h, (x, y) -> k);
  }

  public static <K> Grid<K> create(int w, int h, BiFunction<Integer, Integer, K> fillerFunction) {
    Grid<K> grid = new Grid<>(w, h, null);
    for (int x = 0; x < grid.getW(); x++) {
      for (int y = 0; y < grid.getH(); y++) {
        grid.set(x, y, fillerFunction.apply(x, y));
      }
    }
    return grid;
  }

  public static <K> Grid<K> create(int w, int h) {
    return create(w, h, (K) null);
  }

  public static <K> Grid<K> create(Grid<?> other) {
    return create(other.getW(), other.getH());
  }

  public static <K> Grid<K> copy(Grid<K> other) {
    Grid<K> grid = Grid.create(other);
    for (int x = 0; x < grid.w; x++) {
      for (int y = 0; y < grid.h; y++) {
        grid.set(x, y, other.get(x, y));
      }
    }
    return grid;
  }

  @Override
  public Iterator<Entry<T>> iterator() {
    return new GridIterator<>(this);
  }

  public Collection<T> values() {
    return Collections.unmodifiableList(ts);
  }

  public long count(Predicate<T> predicate) {
    return values().stream().filter(predicate).count();
  }

  public static <K> String toString(Grid<K> grid, String format) {
    StringBuilder sb = new StringBuilder();
    for (int y = 0; y < grid.getH(); y++) {
      for (int x = 0; x < grid.getW(); x++) {
        sb.append(String.format(format, grid.get(x, y)));
      }
      if (y < grid.getH() - 1) {
        sb.append(String.format("%n"));
      }
    }
    return sb.toString();
  }

  public static <K> String toString(Grid<K> grid, Predicate<K> p) {
    StringBuilder sb = new StringBuilder();
    for (int y = 0; y < grid.getH(); y++) {
      for (int x = 0; x < grid.getW(); x++) {
        sb.append(p.test(grid.get(x, y)) ? "+" : "-");
      }
      if (y < grid.getH() - 1) {
        sb.append(String.format("%n"));
      }
    }
    return sb.toString();
  }

  @Override
  public int hashCode() {
    int hash = 5;
    return hash;
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
    final Grid<?> other = (Grid<?>) obj;
    if (this.w != other.w) {
      return false;
    }
    if (this.h != other.h) {
      return false;
    }
    return Objects.equals(this.ts, other.ts);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        sb.append(String.format("(%d,%d)=%s", x, y, get(x, y)));
        if ((x < (w - 1)) && (y < (h - 1))) {
          sb.append(", ");
        }
      }
    }
    sb.append("]");
    return sb.toString();
  }
}
