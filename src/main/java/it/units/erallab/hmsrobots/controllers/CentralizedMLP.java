/*
 * Copyright (C) 2019 eric
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
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.objects.Voxel;

import java.util.EnumSet;
import java.util.function.Function;

/**
 * @author eric
 */
public class CentralizedMLP extends ClosedLoopController {

  private final MultiLayerPerceptron mlp;
  private final SerializableFunction<Double, Double> drivingFunction;

  public static int countParams(Grid<Boolean> structure, EnumSet<Voxel.Sensor> inputs, int[] innerNeurons) {
    //count active voxels
    int c = (int) structure.values().stream().filter((b) -> b).count();
    //set neurons count
    int[] neurons = new int[innerNeurons.length + 2];
    neurons[0] = c * inputs.size() + 1 + 1; //+1 is bias, +1 is the driving function;
    neurons[neurons.length - 1] = c;
    System.arraycopy(innerNeurons, 0, neurons, 1, innerNeurons.length);
    //ask mlp
    return MultiLayerPerceptron.countWeights(neurons);
  }

  public CentralizedMLP(Grid<Boolean> structure, EnumSet<Voxel.Sensor> inputs, int[] innerNeurons, double[] weights, SerializableFunction<Double, Double> drivingFunction) {
    super(inputs);
    //count active voxels
    int c = (int) structure.values().stream().filter((b) -> b).count();
    //set neurons count
    int[] neurons = new int[innerNeurons.length + 2];
    neurons[0] = c * inputs.size() + 1 + 1; //+1 is bias, +1 is the driving function
    neurons[neurons.length - 1] = c;
    System.arraycopy(innerNeurons, 0, neurons, 1, innerNeurons.length);
    //build perceptron
    mlp = new MultiLayerPerceptron(MultiLayerPerceptron.ActivationFunction.TANH, neurons, weights);
    //set driving function
    this.drivingFunction = drivingFunction;
  }

  @Override
  public Grid<Double> control(double t, double dt, Grid<Voxel> voxelGrid) {
    //compute driving function
    double v = drivingFunction.apply(t);
    //collect input
    double[] inputValues = new double[mlp.getNeurons()[0] - 1];
    int c = 0;
    for (Voxel voxel : voxelGrid.values()) {
      if (voxel != null) {
        collectInputs(voxel, inputValues, c);
        c = c + getInputs().size();
      }
    }
    inputValues[inputValues.length - 1] = v;
    //compute output
    double[] outputValues = mlp.apply(inputValues);
    //fill grid
    Grid<Double> control = Grid.create(voxelGrid);
    c = 0;
    for (Grid.Entry<Voxel> entry : voxelGrid) {
      if (entry.getValue() != null) {
        control.set(entry.getX(), entry.getY(), outputValues[c]);
        c = c + 1;
      }
    }
    return control;
  }

}
