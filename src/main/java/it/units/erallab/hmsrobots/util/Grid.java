/*
 * Copyright (C) 2020 Eric Medvet <eric.medvet@gmail.com> (as Eric Medvet <eric.medvet@gmail.com>)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Grid<T> implements Iterable<Grid.Entry<T>>, Serializable {

  private final static char FULL_CELL_CHAR = '█';
  private final static char EMPTY_CELL_CHAR = '░';

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

  @JsonProperty("items")
  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
  private final List<T> ts;
  @JsonProperty
  private final int w;
  @JsonProperty
  private final int h;

  @JsonCreator
  public Grid(
      @JsonProperty("w") int w,
      @JsonProperty("h") int h,
      @JsonProperty("items") List<T> ts
  ) {
    this.w = w;
    this.h = h;
    this.ts = new ArrayList<>(w * h);
    for (int i = 0; i < w * h; i++) {
      if ((ts != null) && (i < ts.size())) {
        this.ts.add(ts.get(i));
      } else {
        this.ts.add(null);
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

  public Stream<Entry<T>> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  public Collection<T> values() {
    return Collections.unmodifiableList(ts);
  }

  public long count(Predicate<T> predicate) {
    return values().stream().filter(predicate).count();
  }

  public List<List<T>> rows() {
    List<List<T>> rows = new ArrayList<>();
    for (int y = 0; y < h; y++) {
      List<T> row = new ArrayList<>();
      for (int x = 0; x < w; x++) {
        row.add(get(x, y));
      }
      rows.add(row);
    }
    return rows;
  }

  public List<List<T>> columns() {
    List<List<T>> columns = new ArrayList<>();
    for (int x = 0; x < w; x++) {
      List<T> column = new ArrayList<>();
      for (int y = 0; y < h; y++) {
        column.add(get(x, y));
      }
      columns.add(column);
    }
    return columns;
  }

  public boolean[][] toArray(Predicate<T> p) {
    boolean[][] b = new boolean[w][h];
    for (Grid.Entry<T> entry : this) {
      b[entry.getX()][entry.getY()] = p.test(entry.getValue());
    }
    return b;
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
    return toString(grid, p, "\n");
  }

  public static <K> String toString(Grid<K> grid, Predicate<K> p, String separator) {
    return toString(grid, (Function<K, Character>) k -> p.test(k) ? FULL_CELL_CHAR : EMPTY_CELL_CHAR, separator);
  }

  public static <K> String toString(Grid<K> grid, Function<K, Character> function) {
    return toString(grid, function, "\n");
  }

  public static <K> String toString(Grid<K> grid, Function<K, Character> function, String separator) {
    StringBuilder sb = new StringBuilder();
    for (int y = 0; y < grid.getH(); y++) {
      for (int x = 0; x < grid.getW(); x++) {
        sb.append(function.apply(grid.get(x, y)));
      }
      if (y < grid.getH() - 1) {
        sb.append(separator);
      }
    }
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Grid<?> grid = (Grid<?>) o;
    return w == grid.w && h == grid.h && ts.equals(grid.ts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ts, w, h);
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
