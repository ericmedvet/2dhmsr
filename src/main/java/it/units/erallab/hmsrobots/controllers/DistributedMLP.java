/*
 * Copyright (C) 2019 Eric Medvet <eric.medvet@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.controllers;

import it.units.erallab.hmsrobots.util.SerializableFunction;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.util.Grid;
import java.util.List;
import java.util.function.Function;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class DistributedMLP extends ClosedLoopController {

  private final Grid<SerializableFunction<Double, Double>> drivingFunctionsGrid;
  private final Grid<MultiLayerPerceptron> mlpGrid;
  private final int signals;

  private final Grid<double[][]> lastSignalsGrid; //1st index: 0=N, 1=E, 2=S, 3=W

  public static int countParams(Grid<Boolean> structure, Grid<List<Voxel.Sensor>> sensorsGrid, int signals, int[] innerNeurons) {
    doChecks(structure, sensorsGrid);
    int sumOfNOfWeights = 0;
    for (Grid.Entry<Boolean> entry : structure) {
      if (entry.getValue()) {
        int inputs = signals * 4 + sensorsGrid.get(entry.getX(), entry.getY()).size() + 1 + 1; //+1 bias, +1 driving function
        int outputs = signals * 4 + 1;
        int[] neurons = new int[innerNeurons.length + 2];
        neurons[0] = inputs;
        neurons[neurons.length - 1] = outputs;
        System.arraycopy(innerNeurons, 0, neurons, 1, innerNeurons.length);
        sumOfNOfWeights = sumOfNOfWeights + MultiLayerPerceptron.countWeights(neurons);
      }
    }
    return sumOfNOfWeights;
  }

  public DistributedMLP(Grid<Boolean> structure, Grid<SerializableFunction<Double, Double>> drivingFunctionsGrid, Grid<List<Voxel.Sensor>> sensorsGrid, int signals, int[] innerNeurons, double[] weights) {
    super(sensorsGrid);
    doChecks(structure, sensorsGrid, drivingFunctionsGrid);
    //set fields
    this.signals = signals;
    this.drivingFunctionsGrid = drivingFunctionsGrid;
    //compute mlp topologies
    Grid<int[]> neuronsGrid = Grid.create(structure);
    int sumOfNOfWeights = 0;
    for (Grid.Entry<Boolean> entry : structure) {
      if (entry.getValue()) {
        int inputs = signals * 4 + sensorsGrid.get(entry.getX(), entry.getY()).size() + 1 + 1; //+1 bias, +1 driving function
        int outputs = signals * 4 + 1;
        int[] neurons = new int[innerNeurons.length + 2];
        neurons[0] = inputs;
        neurons[neurons.length - 1] = outputs;
        System.arraycopy(innerNeurons, 0, neurons, 1, innerNeurons.length);
        neuronsGrid.set(entry.getX(), entry.getY(), neurons);
        sumOfNOfWeights = sumOfNOfWeights + MultiLayerPerceptron.countWeights(neurons);
      }
    }
    if (weights.length != sumOfNOfWeights) {
      throw new IllegalArgumentException(String.format("%d weights expected and %d found", neuronsGrid, weights.length));
    }
    //set mlps
    mlpGrid = Grid.create(structure);
    int c = 0;
    for (Grid.Entry<Boolean> entry : structure) {
      if (entry.getValue()) {
        int[] neurons = neuronsGrid.get(entry.getX(), entry.getY());
        double[] mlpWeights = new double[MultiLayerPerceptron.countWeights(neurons)];
        System.arraycopy(weights, c, mlpWeights, 0, mlpWeights.length);
        MultiLayerPerceptron mlp = new MultiLayerPerceptron(MultiLayerPerceptron.ActivationFunction.TANH, neurons, mlpWeights);
        mlpGrid.set(entry.getX(), entry.getY(), mlp);
        c = c + mlpWeights.length;
      }
    }
    //set last signals
    lastSignalsGrid = Grid.create(structure);
    for (Grid.Entry<Boolean> entry : structure) {
      double[][] localSignals = new double[4][];
      for (int i = 0; i < 4; i++) {
        localSignals[i] = new double[signals];
      }
      lastSignalsGrid.set(entry.getX(), entry.getY(), localSignals);
    }
  }

  private static void doChecks(Grid<Boolean> structure, Grid<List<Voxel.Sensor>> sensorsGrid, Grid<SerializableFunction<Double, Double>> drivingFunctionsGrid) throws IllegalArgumentException {
    if ((drivingFunctionsGrid.getW() != structure.getW()) || (drivingFunctionsGrid.getH() != structure.getH())) {
      throw new IllegalArgumentException("Structure and driving functions grids should have the same shape");
    }
    for (Grid.Entry<Boolean> entry : structure) {
      if (entry.getValue()) {
        if (drivingFunctionsGrid.get(entry.getX(), entry.getY())==null) {
          throw new IllegalArgumentException(String.format("Null driving function at filled grid position (%d,%d)", entry.getX(), entry.getY()));
        }
      }
    }
    doChecks(structure, sensorsGrid);
  }

  private static void doChecks(Grid<Boolean> structure, Grid<List<Voxel.Sensor>> sensorsGrid) throws IllegalArgumentException {
    if ((structure.getW() != sensorsGrid.getW()) || (structure.getH() != sensorsGrid.getH())) {
      throw new IllegalArgumentException("Structure and sensors grids should have the same shape");
    }
    for (Grid.Entry<Boolean> entry : structure) {
      if (entry.getValue()) {
        if (sensorsGrid.get(entry.getX(), entry.getY())==null) {
          throw new IllegalArgumentException(String.format("Null sensors at filled grid position (%d,%d)", entry.getX(), entry.getY()));
        }
      }
    }
  }

  public DistributedMLP(Grid<MultiLayerPerceptron> mlpGrid, Grid<List<Voxel.Sensor>> sensorsGrid, Grid<SerializableFunction<Double, Double>> drivingFunctions, int signals) {
    super(sensorsGrid);
    this.drivingFunctionsGrid = drivingFunctions;
    this.mlpGrid = mlpGrid;
    this.signals = signals;
    lastSignalsGrid = Grid.create(mlpGrid);
    for (Grid.Entry<MultiLayerPerceptron> entry : mlpGrid) {
      double[][] localSignals = new double[4][];
      for (int i = 0; i < 4; i++) {
        localSignals[i] = new double[signals];
      }
      lastSignalsGrid.set(entry.getX(), entry.getY(), localSignals);
    }
  }

  @Override
  public Grid<Double> control(double t, double dt, Grid<Voxel> voxelGrid) {
    Grid<Double> outputs = Grid.create(voxelGrid);
    Grid<double[][]> localLastSignals = Grid.create(lastSignalsGrid);
    for (int x = 0; x < voxelGrid.getW(); x++) {
      for (int y = 0; y < voxelGrid.getH(); y++) {
        final Voxel voxel = voxelGrid.get(x, y);
        final Function<Double, Double> drivingFunction = drivingFunctionsGrid.get(x, y);
        if (voxel != null) {
          //compute driving value
          double drivingValue = (drivingFunction != null) ? drivingFunction.apply(t) : 1d;
          //build input signals
          double[][] localSignals = new double[4][];
          double[] nullSignals = new double[signals];
          double[][] northSignals = lastSignalsGrid.get(x, y - 1);
          double[][] eastSignals = lastSignalsGrid.get(x + 1, y);
          double[][] southSignals = lastSignalsGrid.get(x, y + 1);
          double[][] westSignals = lastSignalsGrid.get(x - 1, y);
          localSignals[0] = (northSignals != null) ? northSignals[2] : nullSignals;
          localSignals[1] = (eastSignals != null) ? eastSignals[3] : nullSignals;
          localSignals[2] = (southSignals != null) ? southSignals[0] : nullSignals;
          localSignals[3] = (westSignals != null) ? westSignals[1] : nullSignals;
          //compute and set output
          double[] outputValues = computeLocalOutput(voxel, mlpGrid.get(x, y), getSensorsGrid().get(x, y), drivingValue, localSignals);
          outputs.set(x, y, outputValues[0]);
          //update lastSignals
          localLastSignals.set(x, y, unflatSignals(outputValues, 1));
        }
      }
    }
    //update last signals
    for (Grid.Entry<double[][]> entry : localLastSignals) {
      if (entry.getValue() != null) {
        lastSignalsGrid.set(entry.getX(), entry.getY(), localLastSignals.get(entry.getX(), entry.getY()));
      }
    }
    return outputs;
  }

  private double[] computeLocalOutput(Voxel voxel, MultiLayerPerceptron mlp, List<Voxel.Sensor> sensors, double drivingValue, double[][] localSignals) {
    double[] inputValues = new double[mlp.getNeurons()[0] - 1];
    int c = 0;
    //collect inputs
    collectInputs(voxel, sensors, inputValues, c);
    c = c + sensors.size();
    //collect driving function (and bias)
    inputValues[c] = drivingValue;
    c = c + 1;
    //collect local signals
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < signals; j++) {
        inputValues[c] = localSignals[i][j];
        c = c + 1;
      }
    }
    return mlp.apply(inputValues);
  }

  private double[][] unflatSignals(double[] values, int srcOffset) {
    double[][] signalValues = new double[4][];
    for (int i = 0; i < 4; i++) {
      signalValues[i] = new double[signals];
      System.arraycopy(values, i * signals + srcOffset, signalValues[i], 0, signals);
    }
    return signalValues;
  }

}
