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

import java.util.*;
import java.util.stream.IntStream;

public class PruningMLP extends MultiLayerPerceptron {

  public enum Context {WHOLE, LAYER, NEURON}

  public enum Criterion {
    WEIGHT,
    SIGNAL,
    ABS_SIGNAL,
    SIGNAL_VARIANCE
  }

  @JsonProperty
  private final long nOfCalls;
  @JsonProperty
  private final Context context;
  @JsonProperty
  private final Criterion criterion;

  private long counter;

  private double[][][] means;
  private double[][][] absMeans;
  private double[][][] meanDiffSquareSums; //https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Weighted_incremental_algorithm

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
    absMeans = new double[weights.length][][];
    meanDiffSquareSums = new double[weights.length][][];
    for (int i = 1; i < neurons.length; i++) {
      means[i - 1] = new double[weights[i - 1].length][];
      absMeans[i - 1] = new double[weights[i - 1].length][];
      meanDiffSquareSums[i - 1] = new double[weights[i - 1].length][];
      for (int j = 0; j < weights[i - 1].length; j++) {
        means[i - 1] = new double[weights[i - 1].length][weights[i - 1][j].length];
        absMeans[i - 1] = new double[weights[i - 1].length][weights[i - 1][j].length];
        meanDiffSquareSums[i - 1] = new double[weights[i - 1].length][weights[i - 1][j].length];
      }
    }
  }

  private void prune() {
    List<Pair<int[], Double>> pairs = new ArrayList<>();
    for (int i = 1; i < neurons.length; i++) {
      for (int j = 0; j < neurons[i]; j++) {
        for (int k = 0; k < neurons[i - 1] + 1; k++) {
          pairs.add(Pair.create(
              new int[]{i - 1, j, k},
              switch (criterion) {
                case WEIGHT -> Math.abs(weights[i - 1][j][k]);
                case SIGNAL -> means[i - 1][j][k];
                case ABS_SIGNAL -> absMeans[i - 1][j][k];
                case SIGNAL_VARIANCE -> meanDiffSquareSums[i - 1][j][k];
              }
          ));
        }
      }
    }
    pairs.sort(Comparator.comparing(Pair::getValue));

    pairs.forEach(p -> System.out.printf("%s -> %+5.3f%n", Arrays.toString(p.getKey()), p.getValue()));

  }

  @Override
  public double[] apply(double[] input) {
    if (nOfCalls == counter) {
      prune();
    }
    if (input.length != neurons[0]) {
      throw new IllegalArgumentException(String.format("Expected input length is %d: found %d", neurons[0], input.length));
    }
    double[][] values = new double[neurons.length][];
    values[0] = new double[neurons[0]];
    System.arraycopy(input, 0, values[0], 0, input.length);
    for (int i = 1; i < neurons.length; i++) {
      values[i] = new double[neurons[i]];
      for (int j = 0; j < neurons[i]; j++) {
        double sum = weights[i - 1][j][0]; //set the bias
        for (int k = 1; k < neurons[i - 1] + 1; k++) {
          double signal = values[i - 1][k - 1] * weights[i - 1][j][k];
          sum = sum + signal;
          double delta = signal - means[i - 1][j][k];
          means[i - 1][j][k] = means[i - 1][j][k] + delta / ((double) counter + 1d);
          absMeans[i - 1][j][k] = absMeans[i - 1][j][k] + (Math.abs(signal) - absMeans[i - 1][j][k]) / ((double) counter + 1d);
          meanDiffSquareSums[i - 1][j][k] = meanDiffSquareSums[i - 1][j][k] + delta * (signal - means[i - 1][j][k]);
        }
        values[i][j] = activationFunction.getF().apply(sum);
      }
    }
    counter = counter + 1;

    for (int i = 1; i < neurons.length; i++) {
      for (int j = 0; j < neurons[i]; j++) {
        for (int k = 0; k <= neurons[i - 1]; k++) {
          System.out.printf("l=%d %d->%d: w=%5.3f s.avg=%5.3f as.avg=%5.3f s.var=%5.3f%n", i, j, k, weights[i - 1][j][k], means[i - 1][j][k], absMeans[i - 1][j][k], meanDiffSquareSums[i - 1][j][k]);
        }
      }
    }

    return values[neurons.length - 1];
  }


  public static void main(String[] args) {
    MultiLayerPerceptron nn = new MultiLayerPerceptron(ActivationFunction.TANH, 2, new int[]{2}, 2);
    PruningMLP pnn = new PruningMLP(ActivationFunction.TANH, 2, new int[]{2}, 2, 5, Context.LAYER, Criterion.SIGNAL_VARIANCE);
    Random r = new Random(1);
    double[] ws = IntStream.range(0, pnn.getParams().length).mapToDouble(i -> r.nextDouble() * 2d - 1d).toArray();
    nn.setParams(ws);
    pnn.setParams(ws);
    System.out.println(pnn.printWeights());
    for (int i = 0; i < 10; i++) {
      System.out.println(i);
      double[] in = new double[]{1d, i % 2};
      double[] out = pnn.apply(in);
      System.out.printf("%s -> %s%n", Arrays.toString(in), Arrays.toString(out));
      System.out.printf("%s -> %s%n%n", Arrays.toString(in), Arrays.toString(nn.apply(in)));
    }
  }
}
