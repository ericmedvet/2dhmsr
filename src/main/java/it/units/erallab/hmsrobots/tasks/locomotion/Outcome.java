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

package it.units.erallab.hmsrobots.tasks.locomotion;

import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.Point2;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Outcome {
  private final double distance;
  private final double time;
  private final double robotLargestDim;
  private final double controlPower;
  private final double areaRatioPower;
  private final SortedMap<Double, Point2> centerTrajectory;
  private final SortedMap<Double, Footprint> footprints;
  private final SortedMap<Double, Grid<Boolean>> masks;

  public Outcome(double distance, double time, double robotLargestDim, double controlPower, double areaRatioPower, SortedMap<Double, Point2> centerTrajectory, SortedMap<Double, Footprint> footprints, SortedMap<Double, Grid<Boolean>> masks) {
    this.distance = distance;
    this.time = time;
    this.robotLargestDim = robotLargestDim;
    this.controlPower = controlPower;
    this.areaRatioPower = areaRatioPower;
    this.centerTrajectory = centerTrajectory;
    this.footprints = footprints;
    this.masks = masks;
  }

  public double getDistance() {
    return distance;
  }

  public double getTime() {
    return time;
  }

  public double getRobotLargestDim() {
    return robotLargestDim;
  }

  public double getControlPower() {
    return controlPower;
  }

  public double getAreaRatioPower() {
    return areaRatioPower;
  }

  public SortedMap<Double, Point2> getCenterTrajectory() {
    return centerTrajectory;
  }

  public SortedMap<Double, Footprint> getFootprints() {
    return footprints;
  }

  public SortedMap<Double, Grid<Boolean>> getMasks() {
    return masks;
  }

  @Override
  public String toString() {
    processGaits(computeGaits(getQuantizedFootprints(5d, time, 0.5d), 3, 5), 0.5d, 2);
    return "Outcome{" +
        "distance=" + distance +
        ", time=" + time +
        ", robotLargestDim=" + robotLargestDim +
        ", controlPower=" + controlPower +
        ", areaRatioPower=" + areaRatioPower +
        '}';
  }

  public double getVelocity() {
    return distance / time;
  }

  public double getRelativeVelocity() {
    return getVelocity() / getRobotLargestDim();
  }

  public double getCorrectedEfficiency() {
    return distance / controlPower;
  }

  public Grid<Boolean> getAverageMask(double startingT, double endingT) {
    return Grid.create(
        masks.get(masks.firstKey()).getW(),
        masks.get(masks.firstKey()).getH(),
        (x, y) -> masks.subMap(startingT, endingT).values().stream()
            .mapToDouble(m -> m.get(x, y) ? 1d : 0d)
            .average()
            .orElse(0d) > 0.5d
    );
  }

  public SortedMap<Double, Footprint> getQuantizedFootprints(double startingT, double endingT, double interval) {
    SortedMap<Double, Footprint> quantized = new TreeMap<>();
    int n = footprints.get(footprints.firstKey()).length();
    for (double t = startingT; t <= endingT; t = t + interval) {
      SortedMap<Double, Footprint> local = footprints.subMap(t, t + interval);
      double[] counts = new double[n];
      double tot = local.size();
      for (int x = 0; x < n; x++) {
        final int finalX = x;
        counts[x] = local.values().stream().mapToDouble(f -> f.getMask()[finalX] ? 1d : 0d).sum();
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

  private static Map<List<Footprint>, List<Double>> computeGaits(SortedMap<Double, Footprint> footprints, int minSequenceLength, int maxSequenceLength) {
    Map<List<Footprint>, List<Double>> startTimeMap = new HashMap<>();
    List<Footprint> footprintList = new ArrayList<>(footprints.values());
    List<Double> times = new ArrayList<>(footprints.keySet());
    for (int l = minSequenceLength; l <= maxSequenceLength; l++) {
      for (int i = l; i < footprintList.size(); i++) {
        List<Footprint> sequence = footprintList.subList(i - l, i);
        List<Double> startingTimes = startTimeMap.getOrDefault(sequence, new ArrayList<>());
        startingTimes.add(times.get(i - l));
        startTimeMap.put(sequence, startingTimes);
      }
    }
    //compute intervals
    Map<List<Footprint>, List<Double>> intervalMap = startTimeMap.entrySet().stream()
        .filter(e -> e.getValue().size() > 1)
        .map(e -> {
          List<Double> intervals = new ArrayList<>(e.getValue().size() - 1);
          for (int i = 1; i < e.getValue().size(); i++) {
            intervals.add(e.getValue().get(i) - e.getValue().get(i - 1));
          }
          return Map.entry(
              e.getKey(),
              intervals
          );
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    //merge
    Map<List<Footprint>, List<Double>> mergedIntervalMap = new HashMap<>();
    for (Map.Entry<List<Footprint>, List<Double>> entry : intervalMap.entrySet()) {
      boolean found = false;
      for (Map.Entry<List<Footprint>, List<Double>> existingEntry : mergedIntervalMap.entrySet()) {
        if (areTheSame(existingEntry.getKey(), entry.getKey())) {
          found = true;
          existingEntry.getValue().addAll(entry.getValue());
          break;
        }
      }
      if (!found) {
        mergedIntervalMap.put(entry.getKey(), entry.getValue());
      }
    }
    return mergedIntervalMap;
  }

  private static List<Double> processGaits(Map<List<Footprint>, List<Double>> intervalMap, double interval, int topN) {
    List<Double> allIntervals = intervalMap.values().stream()
        .reduce((l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()))
        .orElse(List.of(0d));
    Double modeInterval = mode(allIntervals);
    //compute per seq median interval
    Map<List<Footprint>, List<Double>> modeIntervalMap = intervalMap.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> {
              double localModeInterval = mode(e.getValue());
              return List.of(
                  localModeInterval, //interval
                  (double) e.getValue().size(), //number of samples
                  (double) e.getKey().size() * interval / localModeInterval, //sequence filling
                  (double) e.getValue().stream().filter(d -> d == localModeInterval).count() / (double) e.getValue().size(), //purity
                  (double) e.getKey().size() * interval / localModeInterval * (double) e.getValue().size() //overall time
              );
            }
        ));
    //filter by mode period, sort, truncate, and return
    System.out.println(modeIntervalMap.values().stream()
        .filter(a -> a.get(0).equals(modeInterval))
        .sorted(Comparator.comparingDouble(a -> a.get(4)))
        .collect(Collectors.toList())
    );

    return null;
  }

  private static <K> boolean areTheSame(List<K> seq1, List<K> seq2) {
    if (seq1.size() != seq2.size()) {
      return false;
    }
    for (int r = 0; r < seq2.size(); r++) {
      boolean same = true;
      for (int i = 0; i < seq1.size(); i++) {
        if (!seq1.get(i).equals(seq2.get((i + r) % seq2.size()))) {
          same = false;
          break;
        }
      }
      if (same) {
        return true;
      }
    }
    return false;
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

}
