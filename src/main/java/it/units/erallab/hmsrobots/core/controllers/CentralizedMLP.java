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
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.Parametrized;
import it.units.erallab.hmsrobots.util.SerializableFunction;

/**
 * @author eric
 */
public class CentralizedMLP extends FlatSensing implements Parametrized {

  private final MultiLayerPerceptron mlp;
  private final SerializableFunction<Double, Double> drivingFunction;

  public CentralizedMLP(Grid<SensingVoxel> voxelGrid, MultiLayerPerceptron mlp, SerializableFunction<Double, Double> drivingFunction) {
    super(voxelGrid);
    this.mlp = mlp;
    this.drivingFunction = drivingFunction;
  }

  public CentralizedMLP(Grid<SensingVoxel> voxelGrid, int[] innerNeurons, double[] weights, SerializableFunction<Double, Double> drivingFunction) {
    super(voxelGrid);
    int[] neurons = MultiLayerPerceptron.neurons(nOfInputs() + 1 + 1, innerNeurons, nOfOutputs());
    double[] localWeights = new double[MultiLayerPerceptron.countWeights(neurons)];
    if (weights != null) {
      System.arraycopy(weights, 0, localWeights, 0, weights.length);
    }
    mlp = new MultiLayerPerceptron(
        MultiLayerPerceptron.ActivationFunction.TANH,
        neurons,
        localWeights
    );
    this.drivingFunction = drivingFunction;
  }

  public CentralizedMLP(Grid<SensingVoxel> voxelGrid, int[] innerNeurons, SerializableFunction<Double, Double> drivingFunction) {
    this(voxelGrid, innerNeurons, null, drivingFunction);
  }

  @Override
  protected double[] control(double t, double[] inputs) {
    double[] mlpInputs = new double[1 + nOfInputs()];
    mlpInputs[0] = drivingFunction == null ? 0d : drivingFunction.apply(t);
    System.arraycopy(inputs, 0, mlpInputs, 1, inputs.length);
    return mlp.apply(mlpInputs);
  }

  @Override
  public double[] getParams() {
    return mlp.getParams();
  }

  @Override
  public void setParams(double[] params) {
    mlp.setParams(params);
  }
}
