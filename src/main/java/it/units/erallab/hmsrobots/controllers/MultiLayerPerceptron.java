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

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.Function;

/**
 *
 * @author eric
 */
public class MultiLayerPerceptron implements Serializable, Function<double[], double[]> {

  public static enum ActivationFunction {
    RELU((Double x) -> {
      if (x < 0) {
        return 0d;
      } else {
        return x;
      }
    }),
    SIGMOID((Double x) -> {
      return 1d / (1d + Math.exp(-x));
    }),
    TANH((Double x) -> {
      return Math.tanh(x);
    });

    private final Function<Double, Double> f;

    private ActivationFunction(Function<Double, Double> f) {
      this.f = f;
    }

  }

  private final ActivationFunction activationFunction;
  private final double[][][] weights;
  private final int[] neurons;

  public MultiLayerPerceptron(ActivationFunction activationFunction, int[] neurons, double[] weights) {
    this.activationFunction = activationFunction;
    this.neurons = neurons;
    this.weights = unflat(weights, neurons);
  }

  protected static double[][][] unflat(double[] flatWeights, int[] neurons) {
    double[][][] unflatWeights = new double[neurons.length - 1][][];
    int c = 0;
    for (int i = 0; i < neurons.length - 1; i++) {
      unflatWeights[i] = new double[neurons[i]][neurons[i + 1]];
      for (int j = 0; j < neurons[i]; j++) {
        for (int k = 0; k < neurons[i + 1]; k++) {
          unflatWeights[i][j][k] = flatWeights[c];
          c = c + 1;
        }
      }
    }
    return unflatWeights;
  }

  protected static double[] flat(double[][][] unflatWeights, int[] neurons) {
    int n = 0;
    for (int i = 0; i < neurons.length - 1; i++) {
      n = n + neurons[i] * neurons[i + 1];
    }
    double[] flatWeights = new double[n];
    int c = 0;
    for (int i = 0; i < neurons.length - 1; i++) {
      for (int j = 0; j < neurons[i]; j++) {
        for (int k = 0; k < neurons[i + 1]; k++) {
          flatWeights[c] = unflatWeights[i][j][k];
          c = c + 1;
        }
      }
    }
    return flatWeights;
  }
  
  public static int countWeights(int[] neurons) {
    int largestLayerSize = Arrays.stream(neurons).max().orElse(0);
    double[][][] fakeWeights = new double[neurons.length][][];
    double[][] fakeLayerWeights = new double[largestLayerSize][];
    Arrays.fill(fakeLayerWeights, new double[largestLayerSize]);
    Arrays.fill(fakeWeights, fakeLayerWeights);
    return flat(fakeWeights, neurons).length;
  }

  @Override
  public double[] apply(double[] input) {
    if (input.length != neurons[0] - 1) {
      throw new IllegalArgumentException(String.format("Expected input length is %d: found %d", neurons.length - 1, input.length));
    }
    double[][] values = new double[neurons.length][];
    values[0] = new double[neurons[0]];
    System.arraycopy(input, 0, values[0], 0, input.length);
    values[0][values[0].length - 1] = 1d; //set the bias    
    for (int i = 1; i < neurons.length; i++) {
      values[i] = new double[neurons[i]];
      for (int j = 0; j < neurons[i]; j++) {
        double sum = 0d;
        for (int k = 0; k < neurons[i - 1]; k++) {
          sum = sum + values[i - 1][k] * weights[i - 1][k][j];
        }
        values[i][j] = activationFunction.f.apply(sum);
      }
    }
    return values[neurons.length - 1];
  }

  public double[][][] getWeights() {
    return weights;
  }

  public int[] getNeurons() {
    return neurons;
  }

}
