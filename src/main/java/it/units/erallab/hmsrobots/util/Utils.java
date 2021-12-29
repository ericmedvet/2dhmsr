/*
 * Copyright (C) 2021 Eric Medvet <eric.medvet@gmail.com> (as Eric Medvet <eric.medvet@gmail.com>)
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

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Utils {

  private Utils() {
  }

  public static <K> Grid<K> cropGrid(Grid<K> inGrid, Predicate<K> p) {
    //find bounds
    int minX = inGrid.getW();
    int maxX = 0;
    int minY = inGrid.getH();
    int maxY = 0;
    for (Grid.Entry<K> entry : inGrid) {
      if (p.test(entry.value())) {
        minX = Math.min(minX, entry.key().x());
        maxX = Math.max(maxX, entry.key().x());
        minY = Math.min(minY, entry.key().y());
        maxY = Math.max(maxY, entry.key().y());
      }
    }
    //build new grid
    Grid<K> outGrid = Grid.create(maxX - minX + 1, maxY - minY + 1);
    //fill
    for (int x = 0; x < outGrid.getW(); x++) {
      for (int y = 0; y < outGrid.getH(); y++) {
        outGrid.set(x, y, inGrid.get(x + minX, y + minY));
      }
    }
    return outGrid;
  }

  public static <K> Grid<K> gridConnected(Grid<K> kGrid, Comparator<K> comparator, int n) {
    record KeyPair(Grid.Key first, Grid.Key second) {}
    Comparator<Grid.Entry<K>> entryComparator = (e1, e2) -> comparator.compare(e1.value(), e2.value());
    Predicate<KeyPair> adjacencyPredicate = p -> (Math.abs(p.first()
        .x() - p.second()
        .x()) <= 1 && p.first().y() == p.second().y()) || (Math.abs(p.first()
        .y() - p.second()
        .y()) <= 1 && p.first().x() == p.second().x());
    Grid.Entry<K> entryFirst = kGrid.stream()
        .min(entryComparator)
        .orElseThrow(() -> new IllegalArgumentException("Grid has no max element"));
    Set<Grid.Entry<K>> selected = new HashSet<>(n);
    selected.add(entryFirst);
    while (selected.size() < n) {
      Set<Grid.Entry<K>> candidates = kGrid.stream()
          .filter(e -> e.value() != null)
          .filter(e -> !selected.contains(e))
          .filter(e -> selected.stream().anyMatch(f -> adjacencyPredicate.test(new KeyPair(e.key(), f.key()))))
          .collect(Collectors.toSet());
      if (candidates.isEmpty()) {
        break;
      }
      selected.add(candidates.stream().min(entryComparator).orElse(entryFirst));
    }
    Grid<K> outGrid = Grid.create(kGrid.getW(), kGrid.getH());
    selected.forEach(e -> outGrid.set(e.key().x(), e.key().y(), e.value()));
    return outGrid;
  }

  public static <K> Grid<K> gridLargestConnected(Grid<K> kGrid, Predicate<K> p) {
    Grid<Integer> iGrid = partitionGrid(kGrid, p);
    //count elements per partition
    Multiset<Integer> counts = LinkedHashMultiset.create();
    for (Integer i : iGrid.values()) {
      if (i != null) {
        counts.add(i);
      }
    }
    //find largest
    Integer maxIndex = null;
    int max = 0;
    for (Integer index : counts.elementSet()) {
      int count = counts.count(index);
      if (count > max) {
        max = count;
        maxIndex = index;
      }
    }
    //if no largest, return empty
    if (maxIndex == null) {
      return Grid.create(kGrid);
    }
    //filter map
    Grid<K> filtered = Grid.create(kGrid);
    for (Grid.Entry<Integer> iEntry : iGrid) {
      if (iEntry.value() != null && iEntry.value().equals(maxIndex)) {
        filtered.set(iEntry.key().x(), iEntry.key().y(), kGrid.get(iEntry.key().x(), iEntry.key().y()));
      }
    }
    return filtered;
  }

  @SafeVarargs
  public static <E> List<E> ofNonNull(E... es) {
    List<E> list = new ArrayList<>();
    for (E e : es) {
      if (e != null) {
        list.add(e);
      }
    }
    return list;
  }

  public static String param(String pattern, String string, String paramName) {
    Matcher matcher = Pattern.compile(pattern).matcher(string);
    if (matcher.matches()) {
      return matcher.group(paramName);
    }
    throw new IllegalStateException(String.format(
        "Param %s not found in %s with pattern %s",
        paramName,
        string,
        pattern
    ));
  }

  public static Map<String, String> params(String pattern, String string) {
    if (!string.matches(pattern)) {
      return null;
    }
    Matcher m = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>").matcher(pattern);
    List<String> groupNames = new ArrayList<>();
    while (m.find()) {
      groupNames.add(m.group(1));
    }
    Map<String, String> params = new HashMap<>();
    for (String groupName : groupNames) {
      String value = param(pattern, string, groupName);
      if (value != null) {
        params.put(groupName, value);
      }
    }
    return params;
  }

  private static <K> Grid<Integer> partitionGrid(Grid<K> kGrid, Predicate<K> p) {
    Grid<Integer> iGrid = Grid.create(kGrid);
    for (int x = 0; x < kGrid.getW(); x++) {
      for (int y = 0; y < kGrid.getH(); y++) {
        if (iGrid.get(x, y) == null) {
          int index = iGrid.values().stream().filter(Objects::nonNull).mapToInt(i -> i).max().orElse(0);
          partitionGrid(x, y, index + 1, kGrid, iGrid, p);
        }
      }
    }
    return iGrid;
  }

  private static <K> void partitionGrid(int x, int y, int i, Grid<K> kGrid, Grid<Integer> iGrid, Predicate<K> p) {
    boolean hereFilled = p.test(kGrid.get(x, y));
    //already done
    if (iGrid.get(x, y) != null) {
      return;
    }
    //filled but not done
    if (hereFilled) {
      iGrid.set(x, y, i);
      //expand east
      if (x > 0) {
        partitionGrid(x - 1, y, i, kGrid, iGrid, p);
      }
      //expand west
      if (x < kGrid.getW() - 1) {
        partitionGrid(x + 1, y, i, kGrid, iGrid, p);
      }
      //expand north
      if (y > 0) {
        partitionGrid(x, y - 1, i, kGrid, iGrid, p);
      }
      //expand south
      if (y < kGrid.getH() - 1) {
        partitionGrid(x, y + 1, i, kGrid, iGrid, p);
      }
    }
  }

  public static double shapeCompactness(Grid<Boolean> posture) {
    // approximate convex hull
    Grid<Boolean> convexHull = Grid.create(posture.getW(), posture.getH(), posture::get);
    boolean none = false;
    // loop as long as there are false cells have at least five of the eight Moore neighbors as true
    while (!none) {
      none = true;
      for (Grid.Entry<Boolean> entry : convexHull) {
        if (convexHull.get(entry.key().x(), entry.key().y())) {
          continue;
        }
        int currentX = entry.key().x();
        int currentY = entry.key().y();
        int adjacentCount = 0;
        // count how many of the Moore neighbors are true
        for (int i : new int[]{1, -1}) {
          int neighborX = currentX;
          int neighborY = currentY + i;
          if (0 <= neighborY && neighborY < convexHull.getH() && convexHull.get(neighborX, neighborY)) {
            adjacentCount += 1;
          }
          neighborX = currentX + i;
          neighborY = currentY;
          if (0 <= neighborX && neighborX < convexHull.getW() && convexHull.get(neighborX, neighborY)) {
            adjacentCount += 1;
          }
          neighborX = currentX + i;
          neighborY = currentY + i;
          if (0 <= neighborX && 0 <= neighborY && neighborX < convexHull.getW() && neighborY < convexHull.getH() && convexHull.get(
              neighborX,
              neighborY
          )) {
            adjacentCount += 1;
          }
          neighborX = currentX + i;
          neighborY = currentY - i;
          if (0 <= neighborX && 0 <= neighborY && neighborX < convexHull.getW() && neighborY < convexHull.getH() && convexHull.get(
              neighborX,
              neighborY
          )) {
            adjacentCount += 1;
          }
        }
        // if at least five, fill the cell
        if (adjacentCount >= 5) {
          convexHull.set(entry.key().x(), entry.key().y(), true);
          none = false;
        }
      }
    }
    // compute are ratio between convex hull and posture
    int nVoxels = (int) posture.count(e -> e);
    int nConvexHull = (int) convexHull.count(e -> e);
    // -> 0.0 for less compact shapes, -> 1.0 for more compact shapes
    return (double) nVoxels / nConvexHull;
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public static double shapeElongation(Grid<Boolean> posture, int n) {
    if (posture.values().stream().noneMatch(e -> e)) {
      throw new IllegalArgumentException("Grid is empty");
    } else if (n <= 0) {
      throw new IllegalArgumentException(String.format("Non-positive number of directions provided: %d", n));
    }
    List<Grid.Key> coordinates = posture.stream()
        .filter(Grid.Entry::value)
        .map(Grid.Entry::key)
        .toList();
    List<Double> diameters = new ArrayList<>();
    for (int i = 0; i < n; ++i) {
      double theta = (2 * i * Math.PI) / n;
      List<Grid.Key> rotatedCoordinates = coordinates.stream()
          .map(p -> new Grid.Key(
              (int) Math.round(p.x() * Math.cos(theta) - p.y() * Math.sin(theta)),
              (int) Math.round(p.x() * Math.sin(theta) + p.y() * Math.cos(theta))
          ))
          .toList();
      double minX = rotatedCoordinates.stream().min(Comparator.comparingInt(Grid.Key::x)).get().x();
      double maxX = rotatedCoordinates.stream().max(Comparator.comparingDouble(Grid.Key::x)).get().x();
      double minY = rotatedCoordinates.stream().min(Comparator.comparingDouble(Grid.Key::y)).get().y();
      double maxY = rotatedCoordinates.stream().max(Comparator.comparingDouble(Grid.Key::y)).get().y();
      double sideX = maxX - minX + 1;
      double sideY = maxY - minY + 1;
      diameters.add(Math.min(sideX, sideY) / Math.max(sideX, sideY));
    }
    return 1.0 - Collections.min(diameters);
  }

}
