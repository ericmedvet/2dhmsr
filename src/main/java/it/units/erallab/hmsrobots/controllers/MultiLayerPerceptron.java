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
    
    private Function<Double, Double> f;

    private ActivationFunction(Function<Double, Double> f) {
      this.f = f;
    }

  }
  
  private final ActivationFunction activationFunction;
  private final double[] weights;
  private final int[] neurons;

  public MultiLayerPerceptron(ActivationFunction activationFunction, int[] neurons, double[] weights) {
    this.activationFunction = activationFunction;
    this.neurons = neurons;
    //count weights
    int expectedWeights = 0;
    for (int i = 1; i<neurons.length; i++) {
      expectedWeights = expectedWeights+(neurons[i-1]+1)*neurons[i]; //+1 is the bias
    }
    //set weights
    if (expectedWeights!=weights.length) {
      throw new IllegalArgumentException(String.format(
              "The network with %d layers of %s neurons should have %d weights",
              neurons.length,
              Arrays.toString(neurons),
              expectedWeights
      ));
    }
    this.weights = weights;
  }

  @Override
  public double[] apply(double[] t) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
  
}
