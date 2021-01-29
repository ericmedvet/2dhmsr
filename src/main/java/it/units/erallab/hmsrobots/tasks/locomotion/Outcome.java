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

import com.google.common.collect.Range;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.Point2;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Outcome {

  public static final double FOOTPRINT_INTERVAL = 0.5d;
  public static final double GAIT_LONGEST_INTERVAL = 5d;

  public enum Component {X, RELATIVE_Y, Y, MODULE}

  public static class Observation {
    private final double time;
    private final Point2 centerPosition;
    private final double terrainHeight;
    private final Footprint footprint;
    private final Grid<Boolean> posture;
    private final double controlEnergy;
    private final double areaRatioEnergy;
    private final double computationTime;

    public Observation(double time, Point2 centerPosition, double terrainHeight, Footprint footprint, Grid<Boolean> posture, double controlEnergy, double areaRatioEnergy, double computationTime) {
      this.time = time;
      this.centerPosition = centerPosition;
      this.terrainHeight = terrainHeight;
      this.footprint = footprint;
      this.posture = posture;
      this.controlEnergy = controlEnergy;
      this.areaRatioEnergy = areaRatioEnergy;
      this.computationTime = computationTime;
    }

    public double getTime() {
      return time;
    }

    public Point2 getCenterPosition() {
      return centerPosition;
    }

    public double getTerrainHeight() {
      return terrainHeight;
    }

    public Footprint getFootprint() {
      return footprint;
    }

    public Grid<Boolean> getPosture() {
      return posture;
    }

    public double getControlEnergy() {
      return controlEnergy;
    }

    public double getAreaRatioEnergy() {
      return areaRatioEnergy;
    }

    public double getComputationTime() {
      return computationTime;
    }
  }

  public static class Mode {
    private final double strength;
    private final double frequency;

    public Mode(double strength, double frequency) {
      this.strength = strength;
      this.frequency = frequency;
    }

    public double getStrength() {
      return strength;
    }

    public double getFrequency() {
      return frequency;
    }

    @Override
    public String toString() {
      return String.format("Mode{%.2f @ %.1fHz}", strength, frequency);
    }
  }

  public static class Gait {
    private final List<Footprint> footprints;
    private final double modeInterval;
    private final double coverage;
    private final double duration;
    private final double purity;

    public Gait(List<Footprint> footprints, double modeInterval, double coverage, double duration, double purity) {
      this.footprints = footprints;
      this.modeInterval = modeInterval;
      this.coverage = coverage;
      this.duration = duration;
      this.purity = purity;
    }

    public List<Footprint> getFootprints() {
      return footprints;
    }

    public double getModeInterval() {
      return modeInterval;
    }

    public double getCoverage() {
      return coverage;
    }

    public double getDuration() {
      return duration;
    }

    public double getPurity() {
      return purity;
    }

    public double getAvgTouchArea() {
      return footprints.stream()
          .mapToDouble(f -> IntStream.range(0, f.length())
              .mapToDouble(i -> f.getMask()[i] ? 1d : 0d)
              .sum() / (double) f.length())
          .average()
          .orElse(0d);
    }

    @Override
    public String toString() {
      return String.format("Gait{footprints=%s, modeInterval=%.1fs, coverage=%.2f, duration=%.1fs, purity=%.2f}",
          footprints, modeInterval, coverage, duration, purity);
    }
  }

  private final List<Observation> observations;

  public Outcome(List<Observation> observations) {
    observations.sort(Comparator.comparingDouble(Observation::getTime));
    this.observations = Collections.unmodifiableList(observations);
  }

  public double getComputationTime() {
    return observations.get(observations.size() - 1).getComputationTime();
  }

  public double getDistance() {
    return observations.get(observations.size() - 1).getCenterPosition().x - observations.get(0).getCenterPosition().x;
  }

  public double getTime() {
    return observations.get(observations.size() - 1).getTime() - observations.get(0).getTime();
  }

  public double getControlPower() {
    return (observations.get(observations.size() - 1).getControlEnergy() - observations.get(0).getControlEnergy()) / getTime();
  }

  public double getAreaRatioPower() {
    return (observations.get(observations.size() - 1).getAreaRatioEnergy() - observations.get(0).getAreaRatioEnergy()) / getTime();
  }

  public SortedMap<Double, Point2> getCenterTrajectory() {
    return new TreeMap<>(observations.stream().collect(Collectors.toMap(Observation::getTime, Observation::getCenterPosition)));
  }

  public SortedMap<Double, Footprint> getFootprints() {
    return new TreeMap<>(observations.stream().collect(Collectors.toMap(Observation::getTime, Observation::getFootprint)));
  }

  public SortedMap<Double, Grid<Boolean>> getPostures() {
    return new TreeMap<>(observations.stream().collect(Collectors.toMap(Observation::getTime, Observation::getPosture)));
  }

  public List<Observation> getObservations() {
    return observations;
  }

  @Override
  public String toString() {
    return String.format("Outcome{computationTime=%.2fs, distance=%.2f, time=%.1fs, controlPower=%.1f, areaRatioPower=%.1f}",
        getComputationTime(), getDistance(), getTime(), getControlPower(), getAreaRatioPower());
  }

  public double getVelocity() {
    return getDistance() / getTime();
  }

  public double getCorrectedEfficiency() {
    return getDistance() / (1d + getControlPower() * getTime());
  }

  public Grid<Boolean> getAveragePosture() {
    return Grid.create(
        observations.get(0).getPosture().getW(),
        observations.get(0).getPosture().getH(),
        (x, y) -> observations.stream()
            .mapToDouble(m -> m.getPosture().get(x, y) ? 1d : 0d)
            .average()
            .orElse(0d) > 0.5d
    );
  }

  public SortedMap<Double, Footprint> getQuantizedFootprints(double interval) {
    SortedMap<Double, Footprint> quantized = new TreeMap<>();
    int n = observations.get(0).getFootprint().length();
    for (double t = observations.get(0).getTime(); t <= observations.get(observations.size() - 1).getTime(); t = t + interval) {
      final double localT = t;
      List<Footprint> local = observations.stream()
          .filter(o -> o.getTime() >= localT && o.getTime() < localT + interval)
          .map(Observation::getFootprint)
          .collect(Collectors.toList());
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

  public Gait getMainGait(double interval, double longestInterval) {
    List<Gait> gaits = computeGaits(
        getQuantizedFootprints(interval),
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

  public Gait getMainGait() {
    return getMainGait(FOOTPRINT_INTERVAL, GAIT_LONGEST_INTERVAL);
  }

  private static List<Gait> computeGaits(SortedMap<Double, Footprint> footprints, int minSequenceLength, int maxSequenceLength, double interval) {
    //compute subsequences
    Map<List<Footprint>, List<Range<Double>>> sequences = new HashMap<>();
    List<Footprint> footprintList = new ArrayList<>(footprints.values());
    List<Range<Double>> ranges = footprints.keySet().stream()
        .map(d -> Range.closedOpen(d, d + interval))
        .collect(Collectors.toList()); //list of range of each footprint
    for (int l = minSequenceLength; l <= maxSequenceLength; l++) {
      for (int i = l; i <= footprintList.size(); i++) {
        List<Footprint> sequence = footprintList.subList(i - l, i);
        List<Range<Double>> localRanges = sequences.getOrDefault(sequence, new ArrayList<>());
        localRanges.add(Range.openClosed(
            ranges.get(i - l).lowerEndpoint(), // first t of the first footprint
            ranges.get(i - 1).upperEndpoint() //last t of the last footprint
        ));
        sequences.put(sequence, localRanges);
      }
    }
    //compute median interval
    List<Double> allIntervals = sequences.values().stream()
        .map(l -> IntStream.range(0, l.size() - 1)
            .mapToObj(i -> l.get(i + 1).lowerEndpoint() - l.get(i).lowerEndpoint())
            .collect(Collectors.toList())
        ) // stream of List<Double>, each being a list of the intervals of that subsequence
        .reduce((l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()))
        .orElse(List.of());
    if (allIntervals.isEmpty()) {
      return List.of();
    }
    double modeInterval = mode(allIntervals);
    //compute gaits
    return sequences.entrySet().stream()
        .filter(e -> e.getValue().size() > 1) // discard subsequences observed only once
        .map(e -> {
              List<Double> intervals = IntStream.range(0, e.getValue().size() - 1)
                  .mapToObj(i -> e.getValue().get(i + 1).lowerEndpoint() - e.getValue().get(i).lowerEndpoint())
                  .collect(Collectors.toList());
              List<Double> coverages = IntStream.range(0, intervals.size())
                  .mapToObj(i -> (e.getValue().get(i).upperEndpoint() - e.getValue().get(i).lowerEndpoint()) / intervals.get(i))
                  .collect(Collectors.toList());
              double localModeInterval = mode(intervals);
              return new Gait(
                  e.getKey(),
                  localModeInterval,
                  coverages.stream().mapToDouble(d -> d).average().orElse(0d),
                  e.getValue().stream().mapToDouble(r -> r.upperEndpoint() - r.lowerEndpoint()).sum(),
                  (double) intervals.stream().filter(d -> d == localModeInterval).count() / (double) e.getValue().size()
              );
            }
        )
        .filter(g -> g.getModeInterval() == modeInterval)
        .collect(Collectors.toList());
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

  public List<Mode> getCenterModes(Component component) {
    List<Double> v = new ArrayList<>();
    List<Double> times = observations.stream().map(Observation::getTime).collect(Collectors.toList());
    List<Point2> points = observations.stream().map(Observation::getCenterPosition).collect(Collectors.toList());
    List<Double> terrainHeights = observations.stream().map(Observation::getTerrainHeight).collect(Collectors.toList());
    List<Double> intervals = new ArrayList<>();
    for (int i = 0; i < points.size() - 1; i++) {
      v.add(switch (component) {
        case X -> Math.abs(points.get(i + 1).x - points.get(i).x);
        case Y -> Math.abs(points.get(i + 1).y - points.get(i).y);
        case RELATIVE_Y -> Math.abs(points.get(i + 1).x - terrainHeights.get(i + 1) - points.get(i).x + terrainHeights.get(i));
        case MODULE -> Math.sqrt(Math.pow(points.get(i + 1).x - points.get(i).x, 2d) + Math.pow(points.get(i + 1).y - points.get(i).y, 2d));
      });
      intervals.add(times.get(i + 1) - times.get(i));
    }
    double avgInterval = intervals.stream().mapToDouble(d -> d).average().orElseThrow();
    //pad
    int paddedSize = (int) Math.pow(2d, Math.ceil(Math.log(v.size()) / Math.log(2d)));
    if (paddedSize != v.size()) {
      v.addAll(Collections.nCopies(paddedSize - v.size(), 0d));
    }
    //compute fft
    FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
    List<Double> f = List.of(fft.transform(v.stream()
        .mapToDouble(d -> d)
        .toArray(), TransformType.FORWARD)).stream()
        .map(Complex::abs)
        .collect(Collectors.toList())
        .subList(0, paddedSize / 2 + 1);
    return IntStream.range(0, f.size())
        .mapToObj(i -> new Mode(
            f.get(i),
            1d / avgInterval / 2d * (double) i / (double) f.size()
        ))
        .sorted(Comparator.comparing(Mode::getStrength).reversed())
        .collect(Collectors.toList());
  }

  public List<Mode> getCenterPowerSpectrum(Component component, double startingF, double endingF, int n) {
    List<Mode> centerModes = getCenterModes(component);
    return IntStream.range(0, n)
        .mapToObj(i -> Range.openClosed(startingF + (endingF - startingF) / (double) n * (double) i, startingF + (endingF - startingF) / (double) n * (double) (i + 1)))
        .map(r -> new Mode(
                centerModes.stream()
                    .filter(m -> r.contains(m.getFrequency()))
                    .mapToDouble(Mode::getStrength)
                    .average()
                    .orElse(0d),
                (r.lowerEndpoint() + r.upperEndpoint()) / 2d
            )
        )
        .collect(Collectors.toList());
  }

  public Outcome subOutcome(double startT, double endT) {
    return new Outcome(observations.stream().filter(o -> o.getTime() >= startT && o.getTime() < endT).collect(Collectors.toList()));
  }

}
