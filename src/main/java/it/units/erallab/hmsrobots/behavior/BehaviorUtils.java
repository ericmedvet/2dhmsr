/*
 * Copyright (c) "Eric Medvet" 2021.
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

package it.units.erallab.hmsrobots.behavior;

import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.geometry.Shape;
import it.units.erallab.hmsrobots.core.snapshots.RobotShape;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.VoxelPoly;
import it.units.erallab.hmsrobots.util.DoubleRange;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author "Eric Medvet" on 2021/09/16 for 2dhmsr
 */
public class BehaviorUtils {

  private BehaviorUtils() {
  }

  public static Point2 center(Collection<? extends Shape> shapes) {
    double x = 0d;
    double y = 0d;
    for (Shape shape : shapes) {
      Point2 center = shape.center();
      x = x + center.x();
      y = y + center.y();
    }
    return Point2.of(x / (double) shapes.size(), y / (double) shapes.size());
  }

  public static Grid<Boolean> computeAveragePosture(Collection<Grid<Boolean>> postures) {
    int w = postures.iterator().next().getW();
    int h = postures.iterator().next().getH();
    return Grid.create(
        w,
        h,
        (x, y) -> postures.stream().mapToDouble(p -> p.get(x, y) ? 1d : 0d).average().orElse(0d) > 0.5d
    );
  }

  public static Footprint computeFootprint(Collection<? extends VoxelPoly> polies, int n) {
    double robotMinX = polies.stream()
        .mapToDouble(b -> b.boundingBox().min().x())
        .min()
        .orElseThrow(() -> new IllegalArgumentException("Empty robot"));
    double robotMaxX = polies.stream()
        .mapToDouble(b -> b.boundingBox().max().x())
        .max()
        .orElseThrow(() -> new IllegalArgumentException("Empty robot"));
    List<DoubleRange> contacts = polies.stream()
        .filter(VoxelPoly::isTouchingGround)
        .map(b -> DoubleRange.of(b.boundingBox().min().x(), b.boundingBox().max().x())).toList();
    boolean[] mask = new boolean[n];
    for (DoubleRange contact : contacts) {
      int minIndex = (int) Math.round((contact.min() - robotMinX) / (robotMaxX - robotMinX) * (double) (n - 1));
      int maxIndex = (int) Math.round((contact.max() - robotMinX) / (robotMaxX - robotMinX) * (double) (n - 1));
      for (int x = minIndex; x <= Math.min(maxIndex, n - 1); x++) {
        mask[x] = true;
      }
    }
    return new Footprint(mask);
  }

  public static List<Gait> computeGaits(
      SortedMap<Double, Footprint> footprints, int minSequenceLength, int maxSequenceLength, double interval
  ) {
    // compute subsequences
    Map<List<Footprint>, List<DoubleRange>> sequences = new HashMap<>();
    List<Footprint> footprintList = new ArrayList<>(footprints.values());
    List<DoubleRange> ranges = footprints.keySet()
        .stream()
        .map(d -> DoubleRange.of(d, d + interval))
        .toList(); // list of range of each footprint
    for (int l = minSequenceLength; l <= maxSequenceLength; l++) {
      for (int i = l; i <= footprintList.size(); i++) {
        List<Footprint> sequence = footprintList.subList(i - l, i);
        List<DoubleRange> localRanges = sequences.getOrDefault(sequence, new ArrayList<>());
        // make sure there's no overlap
        if (localRanges.size() == 0 || localRanges.get(localRanges.size() - 1).max() <= ranges.get(i - l).min()) {
          localRanges.add(DoubleRange.of(
              ranges.get(i - l).min(), // first t of the first footprint
              ranges.get(i - 1).max() // last t of the last footprint
          ));
        }
        sequences.put(sequence, localRanges);
      }
    }
    // compute median interval
    List<Double> allIntervals = sequences.values()
        .stream()
        .map(l -> IntStream.range(0, l.size() - 1)
            .mapToObj(i -> l.get(i + 1).min() - l.get(i).max())
            .toList()) // stream of List<Double>, each being a list of the intervals of that subsequence
        .reduce((l1, l2) -> Stream.concat(l1.stream(), l2.stream()).toList())
        .orElse(List.of());
    if (allIntervals.isEmpty()) {
      return List.of();
    }
    double modeInterval = mode(allIntervals);
    // compute gaits
    return sequences.entrySet().stream().filter(e -> e.getValue().size() > 1) // discard subsequences observed only once
        .map(e -> {
          List<Double> intervals = IntStream.range(0, e.getValue().size() - 1)
              .mapToObj(i -> e.getValue().get(i + 1).min() - e.getValue().get(i).max())
              .toList();
          List<Double> coverages = IntStream.range(0, intervals.size())
              .mapToObj(i -> (e.getValue().get(i).max() - e.getValue().get(i).min()) / intervals.get(i))
              .toList();
          double localModeInterval = mode(intervals);
          return new Gait(
              e.getKey(),
              localModeInterval,
              coverages.stream().mapToDouble(d -> d).average().orElse(0d),
              e.getValue().stream().mapToDouble(DoubleRange::extent).sum(),
              (double) intervals.stream().filter(d -> d == localModeInterval).count() / (double) e.getValue().size()
          );
        }).filter(g -> g.getModeInterval() == modeInterval).toList();
  }

  public static Gait computeMainGait(
      double interval, double longestInterval, SortedMap<Double, Collection<? extends VoxelPoly>> polies, int n
  ) {
    List<Gait> gaits = computeGaits(
        computeQuantizedFootprints(interval, polies, n),
        2,
        (int) Math.round(longestInterval / interval),
        interval
    );
    if (gaits.isEmpty()) {
      return null;
    }
    gaits.sort(Comparator.comparingDouble(Gait::getDuration).reversed());
    return gaits.get(0);
  }

  public static Grid<Boolean> computePosture(Collection<? extends Shape> shapes, int n) {
    Collection<BoundingBox> boxes = shapes.stream().map(Shape::boundingBox).toList();
    double robotMinX = boxes.stream()
        .mapToDouble(b -> b.min().x())
        .min()
        .orElseThrow(() -> new IllegalArgumentException("Empty robot"));
    double robotMaxX = boxes.stream()
        .mapToDouble(b -> b.max().x())
        .max()
        .orElseThrow(() -> new IllegalArgumentException("Empty robot"));
    double robotMinY = boxes.stream()
        .mapToDouble(b -> b.min().y())
        .min()
        .orElseThrow(() -> new IllegalArgumentException("Empty robot"));
    double robotMaxY = boxes.stream()
        .mapToDouble(b -> b.max().y())
        .max()
        .orElseThrow(() -> new IllegalArgumentException("Empty robot"));
    //adjust box to make it squared
    if ((robotMaxY - robotMinY) < (robotMaxX - robotMinX)) {
      double d = (robotMaxX - robotMinX) - (robotMaxY - robotMinY);
      robotMaxY = robotMaxY + d / 2;
      robotMinY = robotMinY - d / 2;
    } else if ((robotMaxY - robotMinY) > (robotMaxX - robotMinX)) {
      double d = (robotMaxY - robotMinY) - (robotMaxX - robotMinX);
      robotMaxX = robotMaxX + d / 2;
      robotMinX = robotMinX - d / 2;
    }
    Grid<Boolean> mask = Grid.create(n, n, false);
    for (BoundingBox b : boxes) {
      int minXIndex = (int) Math.round((b.min().x() - robotMinX) / (robotMaxX - robotMinX) * (double) (n - 1));
      int maxXIndex = (int) Math.round((b.max().x() - robotMinX) / (robotMaxX - robotMinX) * (double) (n - 1));
      int minYIndex = (int) Math.round((b.min().y() - robotMinY) / (robotMaxY - robotMinY) * (double) (n - 1));
      int maxYIndex = (int) Math.round((b.max().y() - robotMinY) / (robotMaxY - robotMinY) * (double) (n - 1));
      for (int x = minXIndex; x <= maxXIndex; x++) {
        for (int y = minYIndex; y <= maxYIndex; y++) {
          mask.set(x, y, true);
        }
      }
    }
    return mask;
  }

  public static SortedMap<Double, Footprint> computeQuantizedFootprints(
      double interval, SortedMap<Double, Collection<? extends VoxelPoly>> polies, int n
  ) {
    SortedMap<Double, Footprint> quantized = new TreeMap<>();
    for (double t = polies.firstKey(); t <= polies.lastKey(); t = t + interval) {
      List<Footprint> local = polies.subMap(t, t + interval)
          .values()
          .stream()
          .map(voxelPolies -> computeFootprint(voxelPolies, n))
          .toList();
      double[] counts = new double[n];
      double tot = local.size();
      for (int x = 0; x < n; x++) {
        final int finalX = x;
        counts[x] = local.stream().mapToDouble(f -> f.getMask()[finalX] ? 1d : 0d).sum();
      }
      boolean[] localFootprint = new boolean[n];
      for (int x = 0; x < n; x++) {
        if (counts[x] > tot / 2d) {
          localFootprint[x] = true;
        }
      }
      quantized.put(t, new Footprint(localFootprint));
    }
    return quantized;
  }

  public static SortedMap<DoubleRange, Double> computeQuantizedSpectrum(
      SortedMap<Double, Double> signal, double minF, double maxF, int nBins
  ) {
    SortedMap<Double, Double> spectrum = computeSpectrum(signal);
    SortedMap<DoubleRange, Double> qSpectrum = new TreeMap<>(Comparator.comparingDouble(DoubleRange::min));
    double binSpan = (maxF - minF) / (double) nBins;
    for (int i = 0; i < nBins; i++) {
      double binMinF = minF + binSpan * (double) i;
      double binMaxF = minF + binSpan * ((double) i + 1d);
      qSpectrum.put(
          DoubleRange.of(binMinF, binMaxF),
          spectrum.subMap(binMinF, binMaxF).values().stream().mapToDouble(d -> d).average().orElse(0d)
      );
    }
    return qSpectrum;
  }

  public static SortedMap<Double, Double> computeSpectrum(SortedMap<Double, Double> signal) {
    List<Double> intervals = new ArrayList<>(signal.size() - 1);
    double previousT = Double.NaN;
    for (double t : signal.keySet()) {
      if (!Double.isNaN(previousT)) {
        intervals.add(t - previousT);
      }
      previousT = t;
    }
    return computeSpectrum(
        signal.values().stream().mapToDouble(d -> d).toArray(),
        intervals.stream().mapToDouble(d -> d).average().orElse(0d)
    );
  }

  public static SortedMap<Double, Double> computeSpectrum(double[] signal, double dT) {
    // pad
    int paddedSize = (int) Math.pow(2d, Math.ceil(Math.log(signal.length) / Math.log(2d)));
    if (paddedSize != signal.length) {
      double[] paddedSignal = new double[paddedSize];
      System.arraycopy(signal, 0, paddedSignal, 0, signal.length);
      signal = paddedSignal;
    }
    // compute fft
    FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
    List<Double> f = Stream.of(fft.transform(signal, TransformType.FORWARD))
        .map(Complex::abs)
        .toList()
        .subList(0, paddedSize / 2 + 1);
    SortedMap<Double, Double> spectrum = new TreeMap<>();
    for (int i = 0; i < f.size(); i++) {
      spectrum.put(1d / dT / 2d * (double) i / (double) f.size(), f.get(i));
    }
    return spectrum;
  }

  public static <K> K getCentralElement(Grid<K> grid) {
    if (grid.values().stream().noneMatch(Objects::nonNull)) {
      throw new IllegalArgumentException("Cannot get central element of an empty grid");
    }
    double mX = grid.stream().filter(e -> e.value() != null).mapToInt(e -> e.key().x()).average().orElse(0d);
    double mY = grid.stream().filter(e -> e.value() != null).mapToInt(e -> e.key().y()).average().orElse(0d);
    double minD = Double.MAX_VALUE;
    int closestX = 0;
    int closestY = 0;
    for (int x = 0; x < grid.getW(); x++) {
      for (int y = 0; y < grid.getH(); y++) {
        double d = (x - mX) * (x - mX) + (y - mY) * (y - mY);
        if (d < minD) {
          minD = d;
          closestX = x;
          closestY = y;
        }
      }
    }
    return grid.get(closestX, closestY);
  }

  private static <K> K mode(Collection<K> collection) {
    return collection.stream()
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
        .entrySet()
        .stream()
        .max(Map.Entry.comparingByValue())
        .orElseThrow()
        .getKey();
  }

  public static Function<Snapshot, Grid<? extends VoxelPoly>> voxelPolyGrid() {
    return s -> {
      if (!RobotShape.class.isAssignableFrom(s.getContent().getClass())) {
        throw new IllegalArgumentException("Cannot extract voxel polies from a snapshots of a non robot");
      }
      return ((RobotShape) s.getContent()).getPolies();
    };
  }

}
