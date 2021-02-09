/*
 * Copyright (C) 2021 Eric Medvet <eric.medvet@gmail.com> (as Eric Medvet <eric.medvet@gmail.com>)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.units.erallab.hmsrobots.core.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.math3.util.Pair;

import java.util.List;

public class PruningMLP extends MultiLayerPerceptron {

  private final class EdgeInfo {
    private final int layer;
    private final int source;
    private final int dest;

    public EdgeInfo(int layer, int source, int dest) {
      this.layer = layer;
      this.source = source;
      this.dest = dest;
    }
  }

  public enum Context {WHOLE, LAYER, NEURON}

  //see https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Welford's_online_algorithm for SIGNAL_VARIANCE
  public enum Criterion {
    WEIGHT,
    SIGNAL
  }

  @JsonProperty
  private final long nOfCalls;
  @JsonProperty
  private final Context context;
  @JsonProperty
  private final Criterion criterion;

  private long counter;

  private double[][][] means;
  private double[][][] variances;

  public PruningMLP(
      @JsonProperty("activationFunction") ActivationFunction activationFunction,
      @JsonProperty("weights") double[][][] weights,
      @JsonProperty("neurons") int[] neurons,
      @JsonProperty("nOfCalls") long nOfCalls,
      @JsonProperty("context") Context context,
      @JsonProperty("criterion") Criterion criterion
  ) {
    super(activationFunction, weights, neurons);
    this.nOfCalls = nOfCalls;
    this.context = context;
    this.criterion = criterion;
    reset();
  }

  public PruningMLP(ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, long nOfCalls, Context context, Criterion criterion) {
    super(activationFunction, nOfInput, innerNeurons, nOfOutput, weights);
    this.nOfCalls = nOfCalls;
    this.context = context;
    this.criterion = criterion;
    reset();
  }

  public PruningMLP(ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, long nOfCalls, Context context, Criterion criterion) {
    super(activationFunction, nOfInput, innerNeurons, nOfOutput);
    this.nOfCalls = nOfCalls;
    this.context = context;
    this.criterion = criterion;
    reset();
  }

  private void reset() {
    counter = 0;
    means = new double[weights.length][][];
    variances = new double[weights.length][][];
    for (int i = 0; i < weights.length; i++) {
      means[i] = new double[weights[i].length][];
      variances[i] = new double[weights[i].length][];
      for (int j = 0; j < weights[i].length; j++) {
        means[i] = new double[weights[i].length][weights[i][j].length];
        means[i] = new double[weights[i].length][weights[i][j].length];
      }
    }
  }

  private void prune() {
    List<Pair<EdgeInfo, Double>> pairs;
    for (int i = 1; i < neurons.length; i++) {
      for (int j = 0; j < neurons[i]; j++) {
        for (int k = 0; k < neurons[i - 1]; k++) {

        }
      }
    }
  }

  @Override
  public double[] apply(double[] input) {
    if (nOfCalls == counter) {
      prune();
    }
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
          double signal = values[i - 1][k] * weights[i - 1][k][j];
          sum = sum + signal;
          means[i - 1][k][j] = (means[i - 1][k][j] * (double) counter + signal) / ((double) counter + 1);
          //TODO update average
        }
        values[i][j] = activationFunction.getF().apply(sum);
      }
    }
    counter = counter + 1;
    return values[neurons.length - 1];
  }
}
