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
package it.units.erallab.hmsrobots.controllers;

import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.sensors.Sensor;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.Parametrized;
import it.units.erallab.hmsrobots.util.SerializableFunction;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class DistributedMLP implements Controller, Parametrized {

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

  private final Grid<SerializableFunction<Double, Double>> drivingFunctionsGrid;
  private final Grid<MultiLayerPerceptron> mlpGrid;
  private final int signals;

  private final Grid<double[]> lastSignalsGrid;

  public DistributedMLP(Grid<SerializableFunction<Double, Double>> drivingFunctionsGrid, Grid<MultiLayerPerceptron> mlpGrid, int signals) {
    this.drivingFunctionsGrid = drivingFunctionsGrid;
    this.mlpGrid = mlpGrid;
    this.signals = signals;
    lastSignalsGrid = Grid.create(mlpGrid, mlp -> new double[signals * Dir.values().length]);
  }

  public DistributedMLP(Grid<Voxel.Description> voxelGrid, Grid<SerializableFunction<Double, Double>> drivingFunctionsGrid, int[] innerNeurons, double[] weights, int signals) {
    this.drivingFunctionsGrid = drivingFunctionsGrid;
    this.signals = signals;
    mlpGrid = Grid.create(voxelGrid);
    int c = 0;
    for (Grid.Entry<Voxel.Description> entry : voxelGrid) {
      if (entry.getValue() != null) {
        int nOfReadings = entry.getValue().getSensors().stream()
            .mapToInt(Sensor::n)
            .sum();
        int nOfInputs = 1 + (drivingFunctionsGrid.get(entry.getX(), entry.getY()) == null ? 0 : 1) + nOfReadings;
        int nOfOutputs = 1 + Dir.values().length * signals;
        int[] neurons = MultiLayerPerceptron.neurons(nOfInputs, innerNeurons, nOfOutputs);
        int nOfWeights = MultiLayerPerceptron.countWeights(neurons);
        double[] localWeights = new double[nOfWeights];
        System.arraycopy(weights, c, localWeights, 0, nOfWeights);
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

  @Override
  public Grid<Double> control(double t, Grid<List<Pair<Sensor, double[]>>> sensorsValues) {
    Grid<double[]> outputGrid = Grid.create(mlpGrid.getW(), mlpGrid.getH(), (x, y) -> {
      SerializableFunction<Double, Double> drivingFunction = drivingFunctionsGrid.get(x, y);
      double[] signalsAndFunctionValues = getLastSignals(x, y,
          drivingFunction != null ? new double[]{drivingFunction.apply(t)} : new double[0]
      );
      double[] inputs = flatten(sensorsValues.get(x, y), signalsAndFunctionValues);
      return mlpGrid.get(x, y).apply(inputs);
    });
    Grid<Double> controlGrid = Grid.create(outputGrid);
    for (Grid.Entry<double[]> entry : outputGrid) {
      controlGrid.set(entry.getX(), entry.getY(), entry.getValue()[0]);
      updateLastSignals(entry.getX(), entry.getY(), entry.getValue(), 1);
    }
    return controlGrid;
  }

  private double[] getLastSignals(int x, int y, double... otherValues) {
    double[] values = new double[otherValues.length + signals * 4];
    if (otherValues.length > 0) {
      System.arraycopy(otherValues, 0, values, 0, otherValues.length);
    }
    int c = otherValues.length;
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
    return new double[0];
  }

  @Override
  public void setParams(double[] params) {
    int c = 0;
    for (Grid.Entry<MultiLayerPerceptron> entry : mlpGrid) {
      if (entry.getValue() != null) {
        double[] weights = new double[entry.getValue().getWeights().length];
        System.arraycopy(params, c, weights, 0, weights.length);
        c = c + weights.length;
        mlpGrid.set(entry.getX(), entry.getY(), new MultiLayerPerceptron(
            MultiLayerPerceptron.ActivationFunction.TANH,
            entry.getValue().getNeurons(),
            weights
        ));
      }
    }
  }
}
