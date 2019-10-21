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

import java.util.List;

/**
 * @author eric
 */
public class CentralizedMLP extends ClosedLoopController {

  private final MultiLayerPerceptron mlp;
  private final SerializableFunction<Double, Double> drivingFunction;

  public static int countParams(Grid<Boolean> structure, Grid<List<Voxel.Sensor>> sensorsGrid, int[] innerNeurons) {
    doChecks(structure, sensorsGrid);
    //count sensors
    int sensors = (int) sensorsGrid.values().stream().filter((s) -> s != null).mapToInt(List::size).sum();
    int voxels = (int) structure.count(b -> b);
    //set neurons count
    int[] neurons = new int[innerNeurons.length + 2];
    neurons[0] = sensors + 1 + 1; //+1 is bias, +1 is the driving function;
    neurons[neurons.length - 1] = voxels;
    System.arraycopy(innerNeurons, 0, neurons, 1, innerNeurons.length);
    //ask mlp
    return MultiLayerPerceptron.countWeights(neurons);
  }

  public CentralizedMLP(Grid<Boolean> structure, Grid<List<Voxel.Sensor>> sensorsGrid, int[] innerNeurons, double[] weights, SerializableFunction<Double, Double> drivingFunction) {
    super(sensorsGrid);
    doChecks(structure, sensorsGrid);
    //count sensors and voxels
    int sensors = (int) sensorsGrid.values().stream().filter((s) -> s != null).mapToInt(List::size).sum();
    int voxels = (int) structure.count(b -> b);
    //set neurons count
    int[] neurons = new int[innerNeurons.length + 2];
    neurons[0] = sensors + 1 + 1; //+1 is bias, +1 is the driving function
    neurons[neurons.length - 1] = voxels;
    System.arraycopy(innerNeurons, 0, neurons, 1, innerNeurons.length);
    //build perceptron
    mlp = new MultiLayerPerceptron(MultiLayerPerceptron.ActivationFunction.TANH, neurons, weights);
    //set driving function
    this.drivingFunction = drivingFunction;
  }

  private static void doChecks(Grid<Boolean> structure, Grid<List<Voxel.Sensor>> sensorsGrid) throws IllegalArgumentException {
    //checks
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

  @Override
  public Grid<Double> control(double t, double dt, Grid<Voxel> voxelGrid) {
    //compute driving function
    double v = drivingFunction.apply(t);
    //collect input
    double[] inputValues = new double[mlp.getNeurons()[0] - 1];
    int c = 0;
    for (Grid.Entry<Voxel> entry : voxelGrid) {
      if (entry.getValue() != null) {
        final List<Voxel.Sensor> sensors = getSensorsGrid().get(entry.getX(), entry.getY());
        collectInputs(entry.getValue(), sensors, inputValues, c);
        c = c + sensors.size();
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
