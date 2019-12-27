/*
 * Copyright (C) 2019 Eric Medvet <eric.medvet@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.util;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Util {

  private static final Logger L = Logger.getLogger(Util.class.getName());

  public static <T extends Serializable> String serialize(T t, boolean compress) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos;
    if (compress) {
      oos = new ObjectOutputStream(new GZIPOutputStream(baos, true));
    } else {
      oos = new ObjectOutputStream(baos);
    }
    oos.writeObject(t);
    oos.flush();
    oos.close();
    return Base64.getEncoder().encodeToString(baos.toByteArray());
  }

  public static <T> T deserialize(String s, boolean compress) throws IOException, ClassNotFoundException {
    ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(s));
    ObjectInputStream ois;
    if (compress) {
      ois = new ObjectInputStream(new GZIPInputStream(bais));
    } else {
      ois = new ObjectInputStream(bais);
    }
    Object o =  ois.readObject();
    return (T) o;
  }

  public static <T extends Serializable> String lazilySerialize(T t) {
    try {
      return serialize(t, true);
    } catch (IOException e) {
      L.log(Level.SEVERE, String.format("Cannot serialize due to %s", e), e);
      return e.getClass().getSimpleName();
    }
  }

  public static <T> T lazilyDeserialize(String s) {
    try {
      return (T)deserialize(s, true);
    } catch (IOException | ClassNotFoundException e) {
      L.log(Level.SEVERE, String.format("Cannot serialize due to %s", e), e);
      return null;
    }
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
      if (iEntry.getValue() == maxIndex) {
        filtered.set(iEntry.getX(), iEntry.getY(), kGrid.get(iEntry.getX(), iEntry.getY()));
      }
    }
    return filtered;
  }

  private static <K> Grid<Integer> partitionGrid(Grid<K> kGrid, Predicate<K> p) {
    Grid<Integer> iGrid = Grid.create(kGrid);
    for (int x = 0; x < kGrid.getW(); x++) {
      for (int y = 0; y < kGrid.getH(); y++) {
        if (iGrid.get(x, y) == null) {
          int index = iGrid.values().stream().filter(i -> i != null).mapToInt(i -> i).max().orElse(0);
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

  public static <K> Grid<K> cropGrid(Grid<K> inGrid, Predicate<K> p) {
    //find bounds
    int minX = inGrid.getW();
    int maxX = 0;
    int minY = inGrid.getH();
    int maxY = 0;
    for (Grid.Entry<K> entry : inGrid) {
      if (p.test(entry.getValue())) {
        minX = Math.min(minX, entry.getX());
        maxX = Math.max(maxX, entry.getX());
        minY = Math.min(minY, entry.getY());
        maxY = Math.max(maxY, entry.getY());
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

}
