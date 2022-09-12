/*
 * Copyright (C) 2022 Giorgia Nadizar <giorgia.nadizar@gmail.com> (as Giorgia Nadizar)
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
import it.units.erallab.hmsrobots.util.Parametrized;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron.ActivationFunction;

public class RecurrentNeuralNetwork implements Serializable, RealFunction, Parametrized, Resettable {

  @JsonProperty
  private final ActivationFunction activationFunction;
  @JsonProperty
  private final int[] neurons;
  @JsonProperty
  private final double[][] inputWeights;
  @JsonProperty
  private final double[][] recurrentWeights;
  @JsonProperty
  private final double[][] outputWeights;
  @JsonProperty
  private final double[][] biases;
  private final double[][] activationValues;

  @JsonCreator
  public RecurrentNeuralNetwork(
      @JsonProperty("activationFunction") ActivationFunction activationFunction,
      @JsonProperty("neurons") int[] neurons,
      @JsonProperty("inputWeights") double[][] inputWeights,
      @JsonProperty("recurrentWeights") double[][] recurrentWeights,
      @JsonProperty("outputWeights") double[][] outputWeights,
      @JsonProperty("biases") double[][] biases
  ) {
    this.activationFunction = activationFunction;
    if (neurons.length != 3) {
      throw new IllegalArgumentException(String.format(
          "Wrong number of neurons parameters: 3 expected, %d found",
          neurons.length
      ));
    }
    this.neurons = neurons;
    if (flat(inputWeights).length != neurons[0] * neurons[1]) {
      throw new IllegalArgumentException(String.format(
          "Wrong number of input weights: %d expected, %d found",
          neurons[0] * neurons[1],
          flat(inputWeights).length
      ));
    }
    this.inputWeights = inputWeights;
    if (flat(recurrentWeights).length != neurons[1] * neurons[1]) {
      throw new IllegalArgumentException(String.format(
          "Wrong number of recurrent weights: %d expected, %d found",
          neurons[1] * neurons[1],
          flat(recurrentWeights).length
      ));
    }
    this.recurrentWeights = recurrentWeights;
    if (flat(outputWeights).length != neurons[1] * neurons[2]) {
      throw new IllegalArgumentException(String.format(
          "Wrong number of output weights: %d expected, %d found",
          neurons[1] * neurons[2],
          flat(outputWeights).length
      ));
    }
    this.outputWeights = outputWeights;
    if (flat(biases).length != neurons[1] + neurons[2]) {
      throw new IllegalArgumentException(String.format(
          "Wrong number of biases: %d expected, %d found",
          neurons[1] + neurons[2],
          flat(biases).length
      ));
    }
    this.biases = biases;
    activationValues = new double[3][];
    IntStream.range(0, neurons.length).forEach(i -> activationValues[i] = new double[neurons[i]]);
  }

  public RecurrentNeuralNetwork(ActivationFunction activationFunction, int[] neurons, double[] weights) {
    this(
        activationFunction,
        neurons,
        unflat(Arrays.copyOfRange(weights, 0, neurons[0] * neurons[1]), neurons[0], neurons[1]),
        unflat(Arrays.copyOfRange(weights, neurons[0] * neurons[1], neurons[0] * neurons[1] + neurons[1] * neurons[1]), neurons[1], neurons[1]),
        unflat(Arrays.copyOfRange(weights, neurons[0] * neurons[1] + neurons[1] * neurons[1], neurons[0] * neurons[1] + neurons[1] * neurons[1] + neurons[1] * neurons[2]), neurons[1], neurons[2]),
        unflatBiases(Arrays.copyOfRange(weights, neurons[0] * neurons[1] + neurons[1] * neurons[1] + neurons[1] * neurons[2], weights.length), neurons)
    );
  }

  public RecurrentNeuralNetwork(ActivationFunction activationFunction, int[] neurons) {
    this(activationFunction, neurons, new double[countWeights(neurons)]);
  }

  @Override
  public double[] getParams() {
    double[] flatInputWeights = flat(inputWeights);
    double[] flatRecurrentWeights = flat(recurrentWeights);
    double[] flatOutputWeights = flat(outputWeights);
    double[] flatBiases = flat(biases);

    double[] flatWeights = new double[flatInputWeights.length + flatRecurrentWeights.length + flatOutputWeights.length + flatBiases.length];
    System.arraycopy(flatInputWeights, 0, flatWeights, 0, flatInputWeights.length);
    System.arraycopy(flatRecurrentWeights, 0, flatWeights, flatInputWeights.length, flatRecurrentWeights.length);
    System.arraycopy(flatOutputWeights, 0, flatWeights, flatInputWeights.length + flatRecurrentWeights.length, flatOutputWeights.length);
    System.arraycopy(flatBiases, 0, flatWeights, flatInputWeights.length + flatRecurrentWeights.length + flatOutputWeights.length, flatBiases.length);

    return flatWeights;
  }

  @Override
  public void setParams(double[] params) {
    double[][] inputWeights = unflat(Arrays.copyOfRange(params, 0, neurons[0] * neurons[1]), neurons[0], neurons[1]);
    double[][] recurrentWeights = unflat(Arrays.copyOfRange(params, neurons[0] * neurons[1], neurons[0] * neurons[1] + neurons[1] * neurons[1]), neurons[1], neurons[1]);
    double[][] outputWeights = unflat(Arrays.copyOfRange(params, neurons[0] * neurons[1] + neurons[1] * neurons[1], neurons[0] * neurons[1] + neurons[1] * neurons[1] + neurons[1] * neurons[2]), neurons[1], neurons[2]);
    double[][] biases = unflatBiases(Arrays.copyOfRange(params, neurons[0] * neurons[1] + neurons[1] * neurons[1] + neurons[1] * neurons[2], params.length), neurons);

    for (int i = 0; i < inputWeights.length; i++) {
      System.arraycopy(inputWeights[i], 0, this.inputWeights[i], 0, inputWeights[i].length);
    }
    for (int i = 0; i < recurrentWeights.length; i++) {
      System.arraycopy(recurrentWeights[i], 0, this.recurrentWeights[i], 0, recurrentWeights[i].length);
    }
    for (int i = 0; i < outputWeights.length; i++) {
      System.arraycopy(outputWeights[i], 0, this.outputWeights[i], 0, outputWeights[i].length);
    }
    for (int i = 0; i < biases.length; i++) {
      System.arraycopy(biases[i], 0, this.biases[i], 0, biases[i].length);
    }

  }

  private static double[] flat(double[][] weights) {
    List<Double> flatWeights = new ArrayList<>();
    for (double[] weight : weights) {
      flatWeights.addAll(Arrays.stream(weight).boxed().toList());
    }
    return flatWeights.stream().mapToDouble(i -> i).toArray();
  }

  private static double[][] unflatBiases(double[] flatBiases, int[] neurons) {
    double[][] biases = new double[neurons.length - 1][];
    int index = 0;
    for (int i = 0; i < neurons.length - 1; i++) {
      biases[i] = new double[neurons[i + 1]];
      System.arraycopy(flatBiases, index, biases[i], 0, neurons[i + 1]);
      index = index + neurons[i + 1];
    }
    return biases;
  }

  private static double[][] unflat(double[] flatWeights, int firstDimension, int secondDimension) {
    double[][] weights = new double[firstDimension][secondDimension];
    int c = 0;
    for (int i = 0; i < firstDimension; i++) {
      for (int j = 0; j < secondDimension; j++) {
        weights[i][j] = flatWeights[c];
        c++;
      }
    }
    return weights;
  }

  public static int countWeights(int[] neurons) {
    return neurons[0] * neurons[1] + neurons[1] * neurons[1] + neurons[1] * neurons[2] + neurons[1] + neurons[2];
  }

  @Override
  public void reset() {
    IntStream.range(0, activationValues.length).forEach(i -> activationValues[i] = new double[activationValues[i].length]);
  }

  @Override
  public double[] apply(double[] input) {
    activationValues[0] = Arrays.stream(input).map(activationFunction::apply).toArray();
    double[] recurrentOutputs = IntStream.range(0, neurons[1]).mapToDouble(h -> {
      double inputs = IntStream.range(0, neurons[0]).mapToDouble(i ->
          activationValues[0][i] * inputWeights[i][h]
      ).sum();
      double recurrent = IntStream.range(0, neurons[1]).mapToDouble(h1 ->
          activationValues[1][h1] * recurrentWeights[h1][h]
      ).sum();
      return activationFunction.apply(inputs + recurrent + biases[0][h]);
    }).toArray();
    activationValues[1] = recurrentOutputs;
    activationValues[2] = IntStream.range(0, neurons[2]).mapToDouble(o ->
        activationFunction.apply(
            biases[1][o] +
                IntStream.range(0, neurons[1]).mapToDouble(h -> activationValues[1][h] * outputWeights[h][o]).sum()
        )
    ).toArray();
    return activationValues[2];
  }

  @Override
  public int getInputDimension() {
    return neurons[0];
  }

  @Override
  public int getOutputDimension() {
    return neurons[2];
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RecurrentNeuralNetwork that = (RecurrentNeuralNetwork) o;
    return activationFunction == that.activationFunction &&
        Arrays.equals(neurons, that.neurons) &&
        Arrays.deepEquals(inputWeights, that.inputWeights) &&
        Arrays.deepEquals(recurrentWeights, that.recurrentWeights) &&
        Arrays.deepEquals(outputWeights, that.outputWeights) &&
        Arrays.deepEquals(biases, that.biases);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(activationFunction);
    result = 31 * result + Arrays.hashCode(neurons);
    result = 31 * result + Arrays.deepHashCode(inputWeights);
    result = 31 * result + Arrays.deepHashCode(recurrentWeights);
    result = 31 * result + Arrays.deepHashCode(outputWeights);
    result = 31 * result + Arrays.deepHashCode(biases);
    return result;
  }

  @Override
  public String toString() {
    return "RNN." + activationFunction.toString().toLowerCase() + "[" +
        Arrays.stream(neurons).mapToObj(Integer::toString).collect(Collectors.joining(","))
        + "]";
  }
}
