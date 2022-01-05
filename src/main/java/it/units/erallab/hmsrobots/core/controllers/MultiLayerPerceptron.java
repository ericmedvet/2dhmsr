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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.snapshots.MLPState;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import it.units.erallab.hmsrobots.util.DoubleRange;
import it.units.erallab.hmsrobots.util.Parametrized;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author eric
 */
public class MultiLayerPerceptron implements Serializable, RealFunction, Parametrized, Snapshottable {

  @JsonProperty
  protected final ActivationFunction activationFunction;
  @JsonProperty
  protected final double[][][] weights;
  @JsonProperty
  protected final int[] neurons;
  protected final double[][] activationValues;

  @JsonCreator
  public MultiLayerPerceptron(
      @JsonProperty("activationFunction") ActivationFunction activationFunction,
      @JsonProperty("weights") double[][][] weights,
      @JsonProperty("neurons") int[] neurons
  ) {
    this.activationFunction = activationFunction;
    this.weights = weights;
    this.neurons = neurons;
    activationValues = new double[neurons.length][];
    if (flat(weights, neurons).length != countWeights(neurons)) {
      throw new IllegalArgumentException(String.format(
          "Wrong number of weights: %d expected, %d found",
          countWeights(neurons),
          flat(weights, neurons).length
      ));
    }
  }

  public MultiLayerPerceptron(
      ActivationFunction activationFunction,
      int nOfInput,
      int[] innerNeurons,
      int nOfOutput,
      double[] weights
  ) {
    this(
        activationFunction,
        unflat(weights, countNeurons(nOfInput, innerNeurons, nOfOutput)),
        countNeurons(nOfInput, innerNeurons, nOfOutput)
    );
  }

  public MultiLayerPerceptron(ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput) {
    this(
        activationFunction,
        nOfInput,
        innerNeurons,
        nOfOutput,
        new double[countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput))]
    );
  }

  public enum ActivationFunction implements Function<Double, Double> {
    RELU(x -> (x < 0) ? 0d : x, DoubleRange.of(0d, Double.POSITIVE_INFINITY)),
    SIGMOID(x -> 1d / (1d + Math.exp(-x)), DoubleRange.of(0d, 1d)),
    SIN(Math::sin, DoubleRange.of(-1d, 1d)),
    TANH(Math::tanh, DoubleRange.of(-1d, 1d)),
    SIGN(Math::signum, DoubleRange.of(-1d, 1d)),
    IDENTITY(x -> x, DoubleRange.of(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));

    private final Function<Double, Double> f;
    private final DoubleRange domain;

    ActivationFunction(Function<Double, Double> f, DoubleRange domain) {
      this.f = f;
      this.domain = domain;
    }

    public Double apply(Double x) {
      return f.apply(x);
    }

    public DoubleRange getDomain() {
      return domain;
    }

    public Function<Double, Double> getF() {
      return f;
    }
  }

  public static int[] countNeurons(int nOfInput, int[] innerNeurons, int nOfOutput) {
    final int[] neurons;
    neurons = new int[2 + innerNeurons.length];
    System.arraycopy(innerNeurons, 0, neurons, 1, innerNeurons.length);
    neurons[0] = nOfInput;
    neurons[neurons.length - 1] = nOfOutput;
    return neurons;
  }

  public static int countWeights(int[] neurons) {
    int c = 0;
    for (int i = 1; i < neurons.length; i++) {
      c = c + neurons[i] * (neurons[i - 1] + 1);
    }
    return c;
  }

  public static int countWeights(int nOfInput, int[] innerNeurons, int nOfOutput) {
    return countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput));
  }

  public static double[] flat(double[][][] unflatWeights, int[] neurons) {
    double[] flatWeights = new double[countWeights(neurons)];
    int c = 0;
    for (int i = 1; i < neurons.length; i++) {
      for (int j = 0; j < neurons[i]; j++) {
        for (int k = 0; k < neurons[i - 1] + 1; k++) {
          flatWeights[c] = unflatWeights[i - 1][j][k];
          c = c + 1;
        }
      }
    }
    return flatWeights;
  }

  public static double[][][] unflat(double[] flatWeights, int[] neurons) {
    double[][][] unflatWeights = new double[neurons.length - 1][][];
    int c = 0;
    for (int i = 1; i < neurons.length; i++) {
      unflatWeights[i - 1] = new double[neurons[i]][neurons[i - 1] + 1];
      for (int j = 0; j < neurons[i]; j++) {
        for (int k = 0; k < neurons[i - 1] + 1; k++) {
          unflatWeights[i - 1][j][k] = flatWeights[c];
          c = c + 1;
        }
      }
    }
    return unflatWeights;
  }

  @Override
  public double[] apply(double[] input) {
    if (input.length != neurons[0]) {
      throw new IllegalArgumentException(String.format(
          "Expected input length is %d: found %d",
          neurons[0],
          input.length
      ));
    }
    activationValues[0] = Arrays.stream(input).map(activationFunction.f::apply).toArray();
    for (int i = 1; i < neurons.length; i++) {
      activationValues[i] = new double[neurons[i]];
      for (int j = 0; j < neurons[i]; j++) {
        double sum = weights[i - 1][j][0]; //set the bias
        for (int k = 1; k < neurons[i - 1] + 1; k++) {
          sum = sum + activationValues[i - 1][k - 1] * weights[i - 1][j][k];
        }
        activationValues[i][j] = activationFunction.apply(sum);
      }
    }
    return activationValues[neurons.length - 1];
  }

  public double[][] getActivationValues() {
    return activationValues;
  }

  @Override
  public int getInputDimension() {
    return neurons[0];
  }

  @Override
  public int getOutputDimension() {
    return neurons[neurons.length - 1];
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
        System.arraycopy(newWeights[l][s], 0, weights[l][s], 0, newWeights[l][s].length);
      }
    }
  }

  @Override
  public Snapshot getSnapshot() {
    return new Snapshot(
        new MLPState(getActivationValues(), getWeights(), activationFunction.getDomain()),
        getClass()
    );
  }

  public double[][][] getWeights() {
    return weights;
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

  @Override
  public String toString() {
    return "MLP." + activationFunction.toString().toLowerCase() + "[" +
        Arrays.stream(neurons).mapToObj(Integer::toString).collect(Collectors.joining(","))
        + "]";
  }

  public String weightsString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 1; i < neurons.length; i++) {
      for (int j = 0; j < neurons[i]; j++) {
        sb.append("->(").append(i).append(",").append(j).append("):");
        for (int k = 0; k < neurons[i - 1] + 1; k++) {
          sb.append(String.format(" %+5.3f", weights[i - 1][j][k]));
        }
        sb.append("\n");
      }
    }
    return sb.toString();
  }

}
