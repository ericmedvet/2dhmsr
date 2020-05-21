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
package it.units.erallab.hmsrobots.core.controllers;

import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.core.sensors.Sensor;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.Parametrized;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class DistributedMLP extends SensingController<Double, SensingVoxel> implements Parametrized {

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

  private final Grid<MultiLayerPerceptron> mlpGrid;
  private final int signals;

  private final Grid<double[]> lastSignalsGrid;

  public DistributedMLP(Grid<MultiLayerPerceptron> mlpGrid, int signals) {
    this.mlpGrid = mlpGrid;
    this.signals = signals;
    lastSignalsGrid = Grid.create(mlpGrid, mlp -> new double[signals * Dir.values().length]);
  }

  public DistributedMLP(Grid<SensingVoxel> voxels, int[] innerNeurons, double[] weights, int signals) {
    this.signals = signals;
    mlpGrid = Grid.create(voxels);
    int c = 0;
    for (Grid.Entry<SensingVoxel> entry : voxels) {
      if (entry.getValue() != null) {
        int nOfReadings = entry.getValue().getSensors().stream()
            .mapToInt(s -> s.domains().length)
            .sum();
        int nOfInputs = 1 + nOfReadings + Dir.values().length * signals;
        int nOfOutputs = 1 + Dir.values().length * signals;
        int[] neurons = MultiLayerPerceptron.neurons(nOfInputs, innerNeurons, nOfOutputs);
        int nOfWeights = MultiLayerPerceptron.countWeights(neurons);
        double[] localWeights = new double[nOfWeights];
        if (weights != null) {
          System.arraycopy(weights, c, localWeights, 0, nOfWeights);
        }
        c = c + nOfWeights;
        mlpGrid.set(entry.getX(), entry.getY(), new MultiLayerPerceptron(
            MultiLayerPerceptron.ActivationFunction.TANH,
            neurons,
            localWeights
        ));
      }
    }
    lastSignalsGrid = Grid.create(mlpGrid, mlp -> new double[signals * Dir.values().length]);
  }

  public DistributedMLP(Grid<SensingVoxel> voxels, int[] innerNeurons, int signals) {
    this(voxels, innerNeurons, null, signals);
  }

  @Override
  protected Grid<Double> computeControlValues(double t, Grid<List<Pair<Sensor, double[]>>> sensorsValues) {
    Grid<double[]> outputGrid = Grid.create(mlpGrid);
    for (Grid.Entry<MultiLayerPerceptron> entry : mlpGrid) {
      if (entry.getValue() != null) {
        double[] signalsAndFunctionValues = getLastSignals(entry.getX(), entry.getY());
        double[] inputs = flatten(sensorsValues.get(entry.getX(), entry.getY()), signalsAndFunctionValues);
        outputGrid.set(entry.getX(), entry.getY(), entry.getValue().apply(inputs));
      }
    }
    Grid<Double> controlGrid = Grid.create(outputGrid);
    for (Grid.Entry<double[]> entry : outputGrid) {
      if (entry.getValue() != null) {
        controlGrid.set(entry.getX(), entry.getY(), entry.getValue()[0]);
        updateLastSignals(entry.getX(), entry.getY(), entry.getValue(), 1);
      }
    }
    return controlGrid;
  }

  @Override
  protected void control(Double value, SensingVoxel voxel) {
    voxel.applyForce(value);
  }

  private double[] getLastSignals(int x, int y) {
    double[] values = new double[signals * Dir.values().length];
    int c = 0;
    for (int i = 0; i < Dir.values().length; i++) {
      int adjacentX = x + Dir.values()[i].dx;
      int adjacentY = y + Dir.values()[i].dy;
      double[] lastSignals = lastSignalsGrid.get(adjacentX, adjacentY);
      if (lastSignals != null) {
        int index = Dir.adjacent(Dir.values()[i]).index;
        System.arraycopy(lastSignals, index * signals, values, c, signals);
      }
      c = c + 1;
    }
    return values;
  }

  private void updateLastSignals(int x, int y, double[] values, int index) {
    System.arraycopy(values, index, lastSignalsGrid.get(x, y), 0, Dir.values().length * signals);
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

  @Override
  public double[] getParams() {
    int size = mlpGrid.values().stream()
        .filter(mlp -> mlp != null)
        .mapToInt(mlp -> mlp.getParams().length)
        .sum();
    double[] values = new double[size];
    int c = 0;
    for (Grid.Entry<MultiLayerPerceptron> entry : mlpGrid) {
      if (entry.getValue() != null) {
        double[] localValues = entry.getValue().getParams();
        System.arraycopy(localValues, 0, values, c, localValues.length);
        c = c + localValues.length;
      }
    }
    return values;
  }

  @Override
  public void setParams(double[] params) {
    int c = 0;
    for (Grid.Entry<MultiLayerPerceptron> entry : mlpGrid) {
      if (entry.getValue() != null) {
        double[] localValues = new double[entry.getValue().getParams().length];
        System.arraycopy(params, c, localValues, 0, localValues.length);
        entry.getValue().setParams(localValues);
        c = c + localValues.length;
      }
    }
  }

  public Grid<MultiLayerPerceptron> getMlpGrid() {
    return mlpGrid;
  }
}
