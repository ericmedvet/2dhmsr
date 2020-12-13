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

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import it.units.erallab.hmsrobots.core.objects.BreakableVoxel;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.core.sensors.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Utils {

  private Utils() {
  }

  private static final Logger L = Logger.getLogger(Utils.class.getName());

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
      if (iEntry.getValue() != null && iEntry.getValue().equals(maxIndex)) {
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

  public static UnaryOperator<Robot<?>> buildRobotTransformation(String name) {
    String breakable = "breakable-(?<triggerType>time|area)-(?<thresholdMean>\\d+(\\.\\d+)?)/(?<thresholdStDev>\\d+(\\.\\d+)?)-(?<restTimeMean>\\d+(\\.\\d+)?)/(?<restTimeStDev>\\d+(\\.\\d+)?)-(?<seed>\\d+)";
    String identity = "identity";
    Map<String, String> params;
    if ((params = params(identity, name)) != null) {
      return UnaryOperator.identity();
    }
    if ((params = params(breakable, name)) != null) {
      String type = params.get("triggerType");
      double thresholdMean = Double.parseDouble(params.get("thresholdMean"));
      double thresholdStDev = Double.parseDouble(params.get("thresholdStDev"));
      double restoreTimeMean = Double.parseDouble(params.get("restoreTimeMean"));
      double restoreTimeStDev = Double.parseDouble(params.get("restoreTimeStDev"));
      long randomSeed = Long.parseLong(params.get("seed"));
      Random random = new Random(randomSeed);
      return new UnaryOperator<Robot<?>>() {
        @Override
        @SuppressWarnings("unchecked")
        public Robot<?> apply(Robot<?> robot) {
          return new Robot<>(
              ((Robot<SensingVoxel>) robot).getController(),
              Grid.create(SerializationUtils.clone((Grid<SensingVoxel>) robot.getVoxels()), v -> v == null ? null : new BreakableVoxel(
                  v.getSensors(),
                  randomSeed,
                  Map.of(
                      BreakableVoxel.ComponentType.ACTUATOR, Set.of(BreakableVoxel.MalfunctionType.FROZEN)
                  ),
                  Map.of(
                      BreakableVoxel.MalfunctionTrigger.valueOf(type.toUpperCase()),
                      random.nextGaussian() * thresholdStDev + thresholdMean
                  ),
                  random.nextGaussian() * restoreTimeStDev + restoreTimeMean
              ))
          );
        }
      };
    }
    throw new IllegalArgumentException(String.format("Unknown body name: %s", name));
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
    throw new IllegalStateException(String.format("Param %s not found in %s with pattern %s", paramName, string, pattern));
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
    return groupNames.stream()
        .collect(Collectors.toMap(n -> n, n -> param(pattern, string, n)));
  }

  public static Function<Grid<Boolean>, Grid<? extends SensingVoxel>> buildSensorizingFunction(String name) {
    String spineTouch = "spinedTouch-(?<cpg>[tf])-(?<malfunction>[tf])";
    String spineTouchSighted = "spinedTouchSighted-(?<cpg>[tf])-(?<malfunction>[tf])";
    String uniform = "uniform-(?<position>[tf])";
    Map<String, String> params;
    if ((params = params(spineTouch, name)) != null) {
      final Map<String, String> pars = params;
      return body -> Grid.create(body.getW(), body.getH(),
          (x, y) -> {
            if (!body.get(x, y)) {
              return null;
            }
            return new SensingVoxel(Utils.ofNonNull(
                new Normalization(new AreaRatio()),
                pars.get("malfunction").equals("t") ? new Malfunction() : null,
                (y == 0) ? new Touch() : null,
                (y == body.getH() - 1) ? new Normalization(new Velocity(true, 5d, Velocity.Axis.X, Velocity.Axis.Y)) : null,
                (x == body.getW() - 1 && y == body.getH() - 1 && pars.get("cpg").equals("t")) ? new Normalization(new TimeFunction(t -> Math.sin(2 * Math.PI * -1 * t), -1, 1)) : null
            ));
          }
      );
    }
    if ((params = params(spineTouchSighted, name)) != null) {
      final Map<String, String> pars = params;
      return body -> Grid.create(body.getW(), body.getH(),
          (x, y) -> {
            if (!body.get(x, y)) {
              return null;
            }
            return new SensingVoxel(Utils.ofNonNull(
                new Normalization(new AreaRatio()),
                pars.get("malfunction").equals("t") ? new Malfunction() : null,
                (y == 0) ? new Touch() : null,
                (y == body.getH() - 1) ? new Normalization(new Velocity(true, 5d, Velocity.Axis.X, Velocity.Axis.Y)) : null,
                (x == body.getW() - 1 && y == body.getH() - 1 && pars.get("cpg").equals("t")) ? new Normalization(new TimeFunction(t -> Math.sin(2 * Math.PI * -1 * t), -1, 1)) : null,
                (x == body.getW() - 1) ? new Lidar(10d, Map.of(Lidar.Side.E, 5)) : null
            ));
          }
      );
    }
    if ((params = params(uniform, name)) != null) {
      final Map<String, String> pars = params;
      return body -> Grid.create(body.getW(), body.getH(), (x, y) -> !body.get(x, y) ? null : new SensingVoxel(Utils.ofNonNull(
          new Normalization(new Velocity(true, 5d, Velocity.Axis.X, Velocity.Axis.Y)),
          new Normalization(new AreaRatio()),
          pars.get("position").equals("t") ? new Constant((double) x / (double) body.getW(), (double) y / (double) body.getH()) : null
      )));
    }
    throw new IllegalArgumentException(String.format("Unknown sensorizing function name: %s", name));
  }


  public static Grid<Boolean> buildShape(String name) {
    String box = "(box|worm)-(?<w>\\d+)x(?<h>\\d+)";
    String biped = "biped-(?<w>\\d+)x(?<h>\\d+)";
    String tripod = "tripod-(?<w>\\d+)x(?<h>\\d+)";
    String ball = "ball-(?<d>\\d+)";
    Map<String, String> params;
    if ((params = params(box, name)) != null) {
      int w = Integer.parseInt(params.get("w"));
      int h = Integer.parseInt(params.get("h"));
      return Grid.create(w, h, true);
    }
    if ((params = params(biped, name)) != null) {
      int w = Integer.parseInt(params.get("w"));
      int h = Integer.parseInt(params.get("h"));
      return Grid.create(w, h, (x, y) -> !(y == 0 && x > 0 && x < w - 1));
    }
    if ((params = params(tripod, name)) != null) {
      int w = Integer.parseInt(params.get("w"));
      int h = Integer.parseInt(params.get("h"));
      return Grid.create(w, h, (x, y) -> !(y != h - 1 && x != 0 && x != w - 1 && x != w / 2));
    }
    if ((params = params(ball, name)) != null) {
      int d = Integer.parseInt(params.get("d"));
      return Grid.create(
          d, d,
          (x, y) -> Math.round(Math.sqrt((x - (d - 1) / 2d) * (x - (d - 1) / 2d) + (y - (d - 1) / 2d) * (y - (d - 1) / 2d))) <= (int) Math.floor(d / 2d)
      );
    }
    throw new IllegalArgumentException(String.format("Unknown body name: %s", name));
  }

}
