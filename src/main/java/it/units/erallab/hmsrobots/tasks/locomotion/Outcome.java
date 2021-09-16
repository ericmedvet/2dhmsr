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

import it.units.erallab.hmsrobots.behavior.BehaviorUtils;
import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.snapshots.VoxelPoly;
import it.units.erallab.hmsrobots.util.Grid;

import java.util.*;
import java.util.stream.Collectors;

public class Outcome {

  public static class Observation {
    private final Grid<VoxelPoly> voxelPolies;
    private final double terrainHeight;
    private final double controlEnergy;
    private final double areaRatioEnergy;
    private final double computationTime;

    public Observation(Grid<VoxelPoly> voxelPolies, double terrainHeight, double controlEnergy, double areaRatioEnergy, double computationTime) {
      this.voxelPolies = voxelPolies;
      this.terrainHeight = terrainHeight;
      this.controlEnergy = controlEnergy;
      this.areaRatioEnergy = areaRatioEnergy;
      this.computationTime = computationTime;
    }

    public Grid<VoxelPoly> getVoxelPolies() {
      return voxelPolies;
    }

    public double getTerrainHeight() {
      return terrainHeight;
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

  private final SortedMap<Double, Observation> observations;

  public Outcome(Map<Double, Observation> observations) {
    this.observations = Collections.unmodifiableSortedMap(new TreeMap<>(observations));
  }

  public double getComputationTime() {
    return observations.get(observations.lastKey()).getComputationTime() - observations.get(observations.firstKey()).getComputationTime();
  }

  public double getDistance() {
    Point2 initialCenter = BehaviorUtils.center(observations.get(observations.firstKey()).getVoxelPolies().values().stream().filter(Objects::nonNull).collect(Collectors.toList()));
    ;
    Point2 finalCenter = BehaviorUtils.center(observations.get(observations.lastKey()).getVoxelPolies().values().stream().filter(Objects::nonNull).collect(Collectors.toList()));
    return finalCenter.x - initialCenter.x;
  }

  public double getTime() {
    return observations.lastKey() - observations.firstKey();
  }

  public double getControlPower() {
    return (observations.get(observations.lastKey()).getControlEnergy() - observations.get(observations.firstKey()).getControlEnergy()) / getTime();
  }

  public double getAreaRatioPower() {
    return (observations.get(observations.lastKey()).getAreaRatioEnergy() - observations.get(observations.firstKey()).getAreaRatioEnergy()) / getTime();
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

}