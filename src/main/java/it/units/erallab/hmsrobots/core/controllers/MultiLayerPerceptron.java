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

import it.units.erallab.hmsrobots.util.Parametrized;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author eric
 */
public class MultiLayerPerceptron implements Serializable, Function<double[], double[]>, Parametrized {

  public enum ActivationFunction {
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

    ActivationFunction(Function<Double, Double> f) {
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

  public static double[][][] unflat(double[] flatWeights, int[] neurons) {
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

  public static double[] flat(double[][][] unflatWeights, int[] neurons) {
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

  public static int[] neurons(int nOfInputs, int[] innerNeurons, int nOfOutputs) {
    int[] neurons = new int[innerNeurons.length + 2];
    neurons[0] = nOfInputs;
    neurons[neurons.length - 1] = nOfOutputs;
    System.arraycopy(innerNeurons, 0, neurons, 1, innerNeurons.length);
    return neurons;
  }

  @Override
  public double[] apply(double[] input) {
    if (input.length != neurons[0] - 1) {
      throw new IllegalArgumentException(String.format("Expected input length is %d: found %d", neurons[0] - 1, input.length));
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

  @Override
  public double[] getParams() {
    return MultiLayerPerceptron.flat(weights, neurons);
  }

  @Override
  public void setParams(double[] params) {
    double[][][] newWeights = MultiLayerPerceptron.unflat(params, neurons);
    for (int l = 0; l < newWeights.length; l++) {
      for (int s = 0; s < newWeights[l].length; s++) {
        for (int d = 0; d < newWeights[l][s].length; d++) {
          weights[l][s][d] = newWeights[l][s][d];
        }
      }
    }
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 67 * hash + Objects.hashCode(this.activationFunction);
    hash = 67 * hash + Arrays.deepHashCode(this.weights);
    hash = 67 * hash + Arrays.hashCode(this.neurons);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final MultiLayerPerceptron other = (MultiLayerPerceptron) obj;
    if (this.activationFunction != other.activationFunction) {
      return false;
    }
    if (!Arrays.deepEquals(this.weights, other.weights)) {
      return false;
    }
    return Arrays.equals(this.neurons, other.neurons);
  }

}
