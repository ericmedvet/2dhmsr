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

import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.objects.Voxel;
import java.util.EnumSet;

/**
 *
 * @author eric
 */
public class CentralizedMLP implements Controller {

  public enum Input {
    AREA_RATIO, ABS_LINEAR_VELOCITY;
  }

  private final EnumSet<Input> inputs;
  private final MultiLayerPerceptron mlp;

  public CentralizedMLP(Grid<Boolean> structure, EnumSet<Input> inputs, int[] innerNeurons, double[] weights) {
    this.inputs = inputs;
    //count active voxels
    int c = 0;
    for (int x = 0; x < structure.getW(); x++) {
      for (int y = 0; y < structure.getH(); y++) {
        if (structure.get(x, y)) {
          c = c + 1;
        }
      }
    }
    //set neurons count
    int[] neurons = new int[innerNeurons.length + 2];
    neurons[0] = c * inputs.size() + 1;
    neurons[neurons.length - 1] = c;
    System.arraycopy(innerNeurons, 0, neurons, 1, innerNeurons.length);
    //build perceptron
    mlp = new MultiLayerPerceptron(MultiLayerPerceptron.ActivationFunction.SIGMOID, neurons, weights);
  }

  @Override
  public Grid<Double> control(double t, double dt, Grid<Voxel> voxelGrid) {
    //collect input
    double[] inputValues = new double[mlp.getNeurons()[0] - 1];
    int c = 0;
    for (int x = 0; x < voxelGrid.getW(); x++) {
      for (int y = 0; y < voxelGrid.getH(); y++) {
        final Voxel voxel = voxelGrid.get(x, y);
        if (voxel != null) {
          for (Input input : inputs) {
            if (input.equals(Input.AREA_RATIO)) {
              inputValues[c] = voxel.getAreaRatio();
              c = c + 1;
            } else if (input.equals(Input.ABS_LINEAR_VELOCITY)) {
              inputValues[c] = voxel.getLinearVelocity().getMagnitude();
              c = c + 1;
            }
          }
        }
      }
    }
    //compute output
    double[] outputValues = mlp.apply(inputValues);
    //fill grid
    Grid<Double> control = Grid.create(voxelGrid.getW(), voxelGrid.getH(), 0d);
    c = 0;
    for (int x = 0; x < voxelGrid.getW(); x++) {
      for (int y = 0; y < voxelGrid.getH(); y++) {
        final Voxel voxel = voxelGrid.get(x, y);
        if (voxel!=null) {
          control.set(x, y, outputValues[c]);
          c = c+1;
        }
      }
    }
    return control;
  }

}
