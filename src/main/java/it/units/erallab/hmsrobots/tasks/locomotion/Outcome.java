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

package it.units.erallab.hmsrobots.tasks.locomotion;

import it.units.erallab.hmsrobots.behavior.BehaviorUtils;
import it.units.erallab.hmsrobots.behavior.Footprint;
import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.snapshots.VoxelPoly;
import it.units.erallab.hmsrobots.util.Domain;
import it.units.erallab.hmsrobots.util.Grid;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Outcome {

  public static class Observation {
    private final Grid<VoxelPoly> voxelPolies;
    private final double terrainHeight;
    private final double computationTime;

    public Observation(Grid<VoxelPoly> voxelPolies, double terrainHeight, double computationTime) {
      this.voxelPolies = voxelPolies;
      this.terrainHeight = terrainHeight;
      this.computationTime = computationTime;
    }

    public Grid<VoxelPoly> getVoxelPolies() {
      return voxelPolies;
    }

    public double getTerrainHeight() {
      return terrainHeight;
    }

    public double getComputationTime() {
      return computationTime;
    }
  }

  protected final SortedMap<Double, Observation> observations;

  public Outcome(Map<Double, Observation> observations) {
    this.observations = Collections.unmodifiableSortedMap(new TreeMap<>(observations));
  }

  public double getComputationTime() {
    return observations.get(observations.lastKey()).getComputationTime() - observations.get(observations.firstKey()).getComputationTime();
  }

  public double getDistance() {
    Point2 initialCenter = BehaviorUtils.center(observations.get(observations.firstKey()).getVoxelPolies().values().stream().filter(Objects::nonNull).collect(Collectors.toList()));
    Point2 finalCenter = BehaviorUtils.center(observations.get(observations.lastKey()).getVoxelPolies().values().stream().filter(Objects::nonNull).collect(Collectors.toList()));
    return finalCenter.x - initialCenter.x;
  }

  public double getTime() {
    return observations.lastKey() - observations.firstKey();
  }

  public double getControlEnergy() {
    double initialEnergy = observations.get(observations.firstKey()).getVoxelPolies().values().stream()
        .filter(Objects::nonNull)
        .mapToDouble(VoxelPoly::getControlEnergy)
        .sum();
    double finalEnergy = observations.get(observations.lastKey()).getVoxelPolies().values().stream()
        .filter(Objects::nonNull)
        .mapToDouble(VoxelPoly::getControlEnergy)
        .sum();
    return finalEnergy - initialEnergy;
  }

  public double getAreaRatioEnergy() {
    double initialEnergy = observations.get(observations.firstKey()).getVoxelPolies().values().stream()
        .filter(Objects::nonNull)
        .mapToDouble(VoxelPoly::getAreaRatioEnergy)
        .sum();
    double finalEnergy = observations.get(observations.lastKey()).getVoxelPolies().values().stream()
        .filter(Objects::nonNull)
        .mapToDouble(VoxelPoly::getAreaRatioEnergy)
        .sum();
    return finalEnergy - initialEnergy;
  }

  public double getControlPower() {
    return getControlEnergy() / getTime();
  }

  public double getAreaRatioPower() {
    return getAreaRatioEnergy() / getTime();
  }

  public SortedMap<Double, Observation> getObservations() {
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

  public Outcome subOutcome(double startT, double endT) {
    return new Outcome(observations.subMap(startT, endT));
  }

  public SortedMap<Domain, Double> getCenterXPositionSpectrum(double minF, double maxF, int nBins) {
    SortedMap<Double, Double> signal = new TreeMap<>(
        observations.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> BehaviorUtils.getCentralElement(e.getValue().getVoxelPolies()).center().x
        )));
    return BehaviorUtils.computeQuantizedSpectrum(signal, minF, maxF, nBins);
  }

  public SortedMap<Domain, Double> getCenterYPositionSpectrum(double minF, double maxF, int nBins) {
    SortedMap<Double, Double> signal = new TreeMap<>(
        observations.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> BehaviorUtils.getCentralElement(e.getValue().getVoxelPolies()).center().y
        )));
    return BehaviorUtils.computeQuantizedSpectrum(signal, minF, maxF, nBins);
  }

  public SortedMap<Domain, Double> getCenterXVelocitySpectrum(double minF, double maxF, int nBins) {
    SortedMap<Double, Double> signal = new TreeMap<>(
        observations.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> BehaviorUtils.getCentralElement(e.getValue().getVoxelPolies()).getLinearVelocity().x
        )));
    return BehaviorUtils.computeQuantizedSpectrum(signal, minF, maxF, nBins);
  }

  public SortedMap<Domain, Double> getCenterYVelocitySpectrum(double minF, double maxF, int nBins) {
    SortedMap<Double, Double> signal = new TreeMap<>(
        observations.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> BehaviorUtils.getCentralElement(e.getValue().getVoxelPolies()).getLinearVelocity().y
        )));
    return BehaviorUtils.computeQuantizedSpectrum(signal, minF, maxF, nBins);
  }

  public SortedMap<Domain, Double> getCenterAngleSpectrum(double minF, double maxF, int nBins) {
    SortedMap<Double, Double> signal = new TreeMap<>(
        observations.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> BehaviorUtils.getCentralElement(e.getValue().getVoxelPolies()).getAngle()
        )));
    return BehaviorUtils.computeQuantizedSpectrum(signal, minF, maxF, nBins);
  }

  public Grid<Boolean> getAveragePosture(int n) {
    return BehaviorUtils.computeAveragePosture(
        observations.values().stream()
            .map(o -> BehaviorUtils.computePosture(
                o.getVoxelPolies().values().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()),
                n
            ))
            .collect(Collectors.toList())
    );
  }

  public List<SortedMap<Domain, Double>> getFootprintsSpectra(int n, double minF, double maxF, int nBins) {
    SortedMap<Double, Footprint> footprints = new TreeMap<>(
        observations.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> BehaviorUtils.computeFootprint(
                e.getValue().getVoxelPolies().values().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()),
                n
            ))));
    return IntStream.range(0, n)
        .mapToObj(i -> BehaviorUtils.computeQuantizedSpectrum(
            new TreeMap<>(
                footprints.entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().getMask()[i] ? 1d : 0d
                ))),
            minF, maxF, nBins
        ))
        .collect(Collectors.toList());
  }
}