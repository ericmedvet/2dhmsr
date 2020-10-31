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
package it.units.erallab.hmsrobots.core.controllers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.core.sensors.Sensor;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Function;

/**
 * @author eric
 * @created 2020/08/18
 * @project TwoDimHighlyModularSoftRobots
 */
public class DistributedSensing implements Controller<SensingVoxel> {

  private enum Dir {

    N(0, -1, 0),
    E(1, 0, 1),
    S(0, 1, 2),
    W(-1, 0, 3);

    private final int dx;
    private final int dy;
    private final int index;

    Dir(int dx, int dy, int index) {
      this.dx = dx;
      this.dy = dy;
      this.index = index;
    }

    private static Dir adjacent(Dir dir) {
      switch (dir) {
        case N:
          return Dir.S;
        case E:
          return Dir.W;
        case S:
          return Dir.N;
        case W:
          return Dir.E;
      }
      return Dir.N;
    }
  }

  @JsonProperty
  private final int signals;
  @JsonProperty
  private final Grid<Integer> nOfInputGrid;
  @JsonProperty
  private final Grid<Integer> nOfOutputGrid;
  @JsonProperty
  private final Grid<Function<double[], double[]>> functions;

  private final Grid<double[]> lastSignalsGrid;

  @JsonCreator
  public DistributedSensing(
      @JsonProperty("signals") int signals,
      @JsonProperty("nOfInputGrid") Grid<Integer> nOfInputGrid,
      @JsonProperty("nOfOutputGrid") Grid<Integer> nOfOutputGrid,
      @JsonProperty("functions") Grid<Function<double[], double[]>> functions
  ) {
    this.signals = signals;
    this.nOfInputGrid = nOfInputGrid;
    this.nOfOutputGrid = nOfOutputGrid;
    this.functions = functions;
    lastSignalsGrid = Grid.create(functions, f -> new double[signals * Dir.values().length]);
    reset();
  }

  public DistributedSensing(Grid<? extends SensingVoxel> voxels, int signals) {
    this(
        signals,
        Grid.create(voxels, v -> (v == null) ? 0 : (signals * Dir.values().length + v.getSensors().stream().mapToInt(s -> s.domains().length).sum())),
        Grid.create(voxels, v -> (v == null) ? 0 : (1 + signals * Dir.values().length)),
        Grid.create(voxels.getW(), voxels.getH(), (x, y) -> voxels.get(x, y) == null ? null : ((double[] in) -> new double[1 + signals * Dir.values().length]))
    );
  }

  public Grid<Function<double[], double[]>> getFunctions() {
    return functions;
  }

  @Override
  public void reset() {
    for (int x = 0; x < lastSignalsGrid.getW(); x++) {
      for (int y = 0; y < lastSignalsGrid.getH(); y++) {
        lastSignalsGrid.set(x, y, new double[signals * Dir.values().length]);
      }
    }

  }

  @Override
  public void control(double t, Grid<? extends SensingVoxel> voxels) {
    for (Grid.Entry<? extends SensingVoxel> entry : voxels) {
      if (entry.getValue() == null) {
        continue;
      }
      //get inputs
      double[] signals = getLastSignals(entry.getX(), entry.getY());
      double[] inputs = flatten(entry.getValue().sense(t), signals);
      //compute outputs
      Function<double[], double[]> function = functions.get(entry.getX(), entry.getY());
      double[] outputs = function != null ? function.apply(inputs) : new double[1 + this.signals * Dir.values().length];
      //apply outputs
      entry.getValue().applyForce(outputs[0]);
      System.arraycopy(outputs, 1, lastSignalsGrid.get(entry.getX(), entry.getY()), 0, this.signals * Dir.values().length);
    }
  }

  public int nOfInputs(int x, int y) {
    return nOfInputGrid.get(x, y);
  }

  public int nOfOutputs(int x, int y) {
    return nOfOutputGrid.get(x, y);
  }

  private double[] getLastSignals(int x, int y) {
    double[] values = new double[signals * Dir.values().length];
    int c = 0;
    for (int i = 0; i < Dir.values().length; i++) {
      int adjacentX = x + Dir.values()[i].dx;
      int adjacentY = y + Dir.values()[i].dy;
      double[] lastSignals = lastSignalsGrid.get(adjacentX, adjacentY);
      if ((lastSignals != null) && (signals > 0)) {
        int index = Dir.adjacent(Dir.values()[i]).index;
        System.arraycopy(lastSignals, index * signals, values, c, signals);
      }
      c = c + 1;
    }
    return values;
  }

  private double[] flatten(List<Pair<Sensor, double[]>> sensorsReadings, double... otherValues) {
    int n = otherValues.length + sensorsReadings.stream().mapToInt(p -> p.getValue().length).sum();
    double[] flatValues = new double[n];
    System.arraycopy(otherValues, 0, flatValues, 0, otherValues.length);
    int c = otherValues.length;
    for (Pair<Sensor, double[]> sensorPair : sensorsReadings) {
      double[] values = sensorPair.getValue();
      System.arraycopy(values, 0, flatValues, c, values.length);
    }
    return flatValues;
  }
}
