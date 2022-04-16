/*
 * Copyright (C) 2022 Giorgia Nadizar <giorgia.nadizar@gmail.com> (as Giorgia Nadizar)
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

package it.units.erallab.hmsrobots.core.controllers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.util.Grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class BrokenDistributedSensing extends DistributedSensing {

  @JsonProperty
  private final double breakageTime;

  @JsonProperty
  private final Grid<boolean[]> lastSignalsGridMask;

  @JsonCreator
  public BrokenDistributedSensing(
      @JsonProperty("signals") int signals,
      @JsonProperty("nOfInputGrid") Grid<Integer> nOfInputGrid,
      @JsonProperty("nOfOutputGrid") Grid<Integer> nOfOutputGrid,
      @JsonProperty("functions") Grid<TimedRealFunction> functions,
      @JsonProperty("breakageTime") double breakageTime,
      @JsonProperty("lastSignalsGridMask") Grid<boolean[]> lastSignalsGridMask
  ) {
    super(signals, nOfInputGrid, nOfOutputGrid, functions);
    this.breakageTime = breakageTime;
    this.lastSignalsGridMask = lastSignalsGridMask;
  }

  public BrokenDistributedSensing(
      int signals,
      Grid<Integer> nOfInputGrid,
      Grid<Integer> nOfOutputGrid,
      Grid<TimedRealFunction> functions,
      double breakageTime,
      Damage damage,
      double rate
  ) {
    this(signals, nOfInputGrid, nOfOutputGrid, functions, breakageTime,
        computeLastSignalsGridMask(nOfOutputGrid, signals, damage, rate));
  }

  public BrokenDistributedSensing(
      int signals,
      Grid<Integer> nOfInputGrid,
      Grid<Integer> nOfOutputGrid,
      Grid<TimedRealFunction> functions,
      double rate
  ) {
    this(signals, nOfInputGrid, nOfOutputGrid, functions, 0, Damage.DIRECTIONAL, rate);
  }

  public BrokenDistributedSensing(
      DistributedSensing distributedSensing,
      double breakageTime,
      Damage damage,
      double rate
  ) {
    this(distributedSensing.signals,
        distributedSensing.nOfInputGrid,
        distributedSensing.nOfOutputGrid,
        distributedSensing.functions,
        breakageTime, damage, rate);
  }

  public enum Damage {RANDOM, DIRECTIONAL}

  @Override
  public Grid<Double> computeControlSignals(double t, Grid<Voxel> voxels) {
    Grid<Double> controlSignals = super.computeControlSignals(t, voxels);
    if (t >= breakageTime) {
      int voxelCommunicationSize = signals * Dir.values().length;
      for (int x = 0; x < lastSignalsGrid.getW(); x++) {
        for (int y = 0; y < lastSignalsGrid.getH(); y++) {
          double[] localLastSignals = lastSignalsGrid.get(x, y);
          for (int i = 0; i < voxelCommunicationSize; i++) {
            localLastSignals[i] = lastSignalsGridMask.get(x, y)[i] ? localLastSignals[i] : 0d;
          }
          lastSignalsGrid.set(x, y, localLastSignals);
        }
      }
    }
    return controlSignals;
  }

  private static Grid<boolean[]> computeLastSignalsGridMask(Grid<Integer> nOfOutputGrid, int signals, Damage damage, double rate) {
    int maskSize = nOfOutputGrid.stream().filter(e -> e.value() > 0).mapToInt(e -> e.value() - 1).sum();
    int nOfRemovals = (int) (rate * maskSize);
    boolean[] flatMask = new boolean[maskSize];
    Arrays.fill(flatMask, true);
    List<Integer> indexes = damage.equals(Damage.RANDOM) ? randomIndexes(maskSize, nOfRemovals) :
        directionalIndexes(maskSize, signals, rate);
    indexes.forEach(i -> flatMask[i] = false);

    int voxelCommunicationSize = signals * Dir.values().length;
    int c = 0;
    Grid<boolean[]> grid = Grid.create(nOfOutputGrid, n -> new boolean[voxelCommunicationSize]);
    for (int x = 0; x < grid.getW(); x++) {
      for (int y = 0; y < grid.getH(); y++) {
        if (nOfOutputGrid.get(x, y) > 0) {
          int localC = c;
          boolean[] localMask = new boolean[voxelCommunicationSize];
          IntStream.range(0, voxelCommunicationSize).forEach(i -> localMask[i] = flatMask[localC * i]);
          grid.set(x, y, localMask);
          c++;
        }
      }
    }
    return grid;
  }

  private static List<Integer> randomIndexes(int size, double rate) {
    List<Integer> list = new ArrayList<>(IntStream.range(0, size).boxed().toList());
    Collections.shuffle(list);
    return list.subList(0, (int) (rate * size));
  }

  private static List<Integer> directionalIndexes(int size, int signals, double rate) {
    List<Integer> list = new ArrayList<>(IntStream.range(0, size / signals).boxed().toList());
    Collections.shuffle(list);
    list = list.subList(0, (int) (rate * size / signals));
    return list.stream().map(i -> IntStream.range(0, signals).mapToObj(j -> signals * i + j).toList())
        .flatMap(List::stream).toList();
  }

}
