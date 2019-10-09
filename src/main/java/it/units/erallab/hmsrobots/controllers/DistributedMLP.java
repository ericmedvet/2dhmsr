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
import java.util.EnumSet;
import java.util.function.Function;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class DistributedMLP extends ClosedLoopController {

  private final Grid<SerializableFunction<Double, Double>> drivingFunctions;
  private final Grid<MultiLayerPerceptron> mlps;
  private final int signals;

  private final Grid<double[][]> lastSignals; //1st index: 0=N, 1=E, 2=S, 3=W

  public static int countParams(Grid<Boolean> structure, EnumSet<Voxel.Sensor> inputs, int signals, int[] innerNeurons) {
    //count voxels
    int nOfVoxels = (int) structure.values().stream().filter((b) -> b).count();
    //compute mlp topology
    int voxelInputs = inputs.size() + 1 + 1 + signals * 4; //+1 for bias, +1 for driving function
    int voxelOutputs = 1 + signals * 4;
    int[] neurons = new int[innerNeurons.length + 2];
    neurons[0] = voxelInputs;
    neurons[neurons.length - 1] = voxelOutputs;
    System.arraycopy(innerNeurons, 0, neurons, 1, innerNeurons.length);
    int mlpNOfWeights = MultiLayerPerceptron.countWeights(neurons);
    int nOfWeights = mlpNOfWeights * nOfVoxels;
    return nOfWeights;
  }

  public DistributedMLP(Grid<Boolean> structure, Grid<SerializableFunction<Double, Double>> drivingFunctions, EnumSet<Voxel.Sensor> inputs, int signals, int[] innerNeurons, double[] weights) {
    super(inputs);
    if ((drivingFunctions.getW() != structure.getW()) || (drivingFunctions.getH() != structure.getH())) {
      throw new IllegalArgumentException("Grids of driving functions and structure should have the same shape");
    }
    this.signals = signals;
    this.drivingFunctions = drivingFunctions;
    //count voxels
    int nOfVoxels = (int) structure.values().stream().filter((b) -> b).count();
    //compute mlp topology
    int voxelInputs = inputs.size() + 1 + 1 + signals * 4;
    int voxelOutputs = 1 + signals * 4;
    int[] neurons = new int[innerNeurons.length + 2];
    neurons[0] = voxelInputs;
    neurons[neurons.length - 1] = voxelOutputs;
    System.arraycopy(innerNeurons, 0, neurons, 1, innerNeurons.length);
    int mlpNOfWeights = MultiLayerPerceptron.countWeights(neurons);
    int nOfWeights = mlpNOfWeights * nOfVoxels;
    if (weights.length != nOfWeights) {
      throw new IllegalArgumentException(String.format("%d weights expected and %d found", nOfWeights, weights.length));
    }
    //set mlps
    mlps = Grid.create(structure);
    int c = 0;
    for (Grid.Entry<Boolean> entry : structure) {
      if (entry.getValue()) {
        double[] mlpWeights = new double[mlpNOfWeights];
        System.arraycopy(weights, c * mlpNOfWeights, mlpWeights, 0, mlpNOfWeights);
        MultiLayerPerceptron mlp = new MultiLayerPerceptron(MultiLayerPerceptron.ActivationFunction.TANH, neurons, mlpWeights);
        mlps.set(entry.getX(), entry.getY(), mlp);
        c = c + 1;
      }
    }
    //set last signals
    lastSignals = Grid.create(structure);
    for (Grid.Entry<Boolean> entry : structure) {
      double[][] localSignals = new double[4][];
      for (int i = 0; i < 4; i++) {
        localSignals[i] = new double[signals];
      }
      lastSignals.set(entry.getX(), entry.getY(), localSignals);
    }
  }

  public DistributedMLP(Grid<SerializableFunction<Double, Double>> drivingFunctions, Grid<MultiLayerPerceptron> mlps, int signals, EnumSet<Voxel.Sensor> inputs) {
    super(inputs);
    this.drivingFunctions = drivingFunctions;
    this.mlps = mlps;
    this.signals = signals;
    lastSignals = Grid.create(mlps);
    for (Grid.Entry<MultiLayerPerceptron> entry : mlps) {
      double[][] localSignals = new double[4][];
      for (int i = 0; i < 4; i++) {
        localSignals[i] = new double[signals];
      }
      lastSignals.set(entry.getX(), entry.getY(), localSignals);
    }
  }
    
  @Override
  public Grid<Double> control(double t, double dt, Grid<Voxel> voxelGrid) {
    Grid<Double> outputs = Grid.create(voxelGrid);
    Grid<double[][]> localLastSignals = Grid.create(lastSignals);    
    for (int x = 0; x < voxelGrid.getW(); x++) {
      for (int y = 0; y < voxelGrid.getH(); y++) {
        final Voxel voxel = voxelGrid.get(x, y);
        final Function<Double, Double> drivingFunction = drivingFunctions.get(x, y);
        if (voxel != null) {
          //compute driving value
          double drivingValue = (drivingFunction != null) ? drivingFunction.apply(t) : 1d;
          //build input signals
          double[][] localSignals = new double[4][];
          double[] nullSignals = new double[signals];
          double[][] northSignals = lastSignals.get(x, y - 1);
          double[][] eastSignals = lastSignals.get(x + 1, y);
          double[][] southSignals = lastSignals.get(x, y + 1);
          double[][] westSignals = lastSignals.get(x - 1, y);
          localSignals[0] = (northSignals != null) ? northSignals[2] : nullSignals;
          localSignals[1] = (eastSignals != null) ? eastSignals[3] : nullSignals;
          localSignals[2] = (southSignals != null) ? southSignals[0] : nullSignals;
          localSignals[3] = (westSignals != null) ? westSignals[1] : nullSignals;
          //compute and set output
          double[] outputValues = computeLocalOutput(voxel, mlps.get(x, y), drivingValue, localSignals);
          outputs.set(x, y, outputValues[0]);
          //update lastSignals
          localLastSignals.set(x, y, unflatSignals(outputValues, 1));
        }
      }
    }
    //update last signals
    for (Grid.Entry<double[][]> entry : localLastSignals) {
      if (entry.getValue()!=null) {
        lastSignals.set(entry.getX(), entry.getY(), localLastSignals.get(entry.getX(), entry.getY()));
      }
    }
    return outputs;
  }

  private double[] computeLocalOutput(Voxel voxel, MultiLayerPerceptron mlp, double drivingValue, double[][] localSignals) {
    double[] inputValues = new double[mlp.getNeurons()[0] - 1];
    int c = 0;
    //collect inputs
    collectInputs(voxel, inputValues, c);
    c = c + getInputs().size();
    //collect driving function (or bias)
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
