package it.units.erallab.hmsrobots.core.controllers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HebbianMultiLayerPerceptron extends MultiLayerPerceptron implements Resettable {

  @JsonProperty
  private final double[][][] startingWeights;  // layer, destination neuron, starting neuron
  @JsonProperty
  private final double[][][][] hebbianCoefficients;
  @JsonProperty
  private final double[][][] eta;
  @JsonProperty
  private final boolean weightsNormalization;

  private boolean learning = true;

  @JsonCreator
  public HebbianMultiLayerPerceptron(
      @JsonProperty("activationFunction") ActivationFunction activationFunction,
      @JsonProperty("weights") double[][][] weights,
      @JsonProperty("hebbianCoefficients") double[][][][] hebbianCoefficients,
      @JsonProperty("neurons") int[] neurons,
      @JsonProperty("eta") double[][][] eta,
      @JsonProperty("weightsNormalization") boolean weightsNormalization
  ) {
    super(activationFunction, weights, neurons);
    this.hebbianCoefficients = hebbianCoefficients;
    this.weightsNormalization = weightsNormalization;
    this.eta = eta;
    startingWeights = deepCopy(weights);
    if (flatHebbianCoefficients(hebbianCoefficients, neurons).length != 4 * countWeights(neurons)) {
      throw new IllegalArgumentException(String.format(
          "Wrong number of hebbian coefficients: %d expected, %d found",
          4 * countWeights(neurons),
          flatHebbianCoefficients(hebbianCoefficients, neurons).length
      ));
    }
  }

  public HebbianMultiLayerPerceptron(ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, double[] hebbianCoefficients, double eta, boolean weightsNormalization) {
    this(
        activationFunction,
        unflat(weights, countNeurons(nOfInput, innerNeurons, nOfOutput)),
        unflatHebbianCoefficients(hebbianCoefficients, countNeurons(nOfInput, innerNeurons, nOfOutput)),
        countNeurons(nOfInput, innerNeurons, nOfOutput),
        initEta(eta, countNeurons(nOfInput, innerNeurons, nOfOutput)),
        weightsNormalization
    );
  }

  public HebbianMultiLayerPerceptron(ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[] hebbianCoefficients, double eta, Random rnd, boolean weightsNormalization) {
    this(
        activationFunction,
        generateRandomWeights(countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput)), countNeurons(nOfInput, innerNeurons, nOfOutput), rnd),
        unflatHebbianCoefficients(hebbianCoefficients, countNeurons(nOfInput, innerNeurons, nOfOutput)),
        countNeurons(nOfInput, innerNeurons, nOfOutput),
        initEta(eta, countNeurons(nOfInput, innerNeurons, nOfOutput)),
        weightsNormalization
    );
  }

  public HebbianMultiLayerPerceptron(ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double eta, boolean weightsNormalization) {
    this(activationFunction, nOfInput, innerNeurons, nOfOutput, new double[countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput))], flatHebbianCoefficients(initHebbianCoefficients(countNeurons(nOfInput, innerNeurons, nOfOutput)), countNeurons(nOfInput, innerNeurons, nOfOutput)), eta, weightsNormalization);
  }

  public HebbianMultiLayerPerceptron(ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, double[] hebbianCoefficients, double[][][] eta, boolean weightsNormalization) {
    this(
        activationFunction,
        unflat(weights, countNeurons(nOfInput, innerNeurons, nOfOutput)),
        unflatHebbianCoefficients(hebbianCoefficients, countNeurons(nOfInput, innerNeurons, nOfOutput)),
        countNeurons(nOfInput, innerNeurons, nOfOutput),
        eta,
        weightsNormalization
    );
  }

  public HebbianMultiLayerPerceptron(ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[] hebbianCoefficients, double[][][] eta, Random rnd, boolean weightsNormalization) {
    this(
        activationFunction,
        generateRandomWeights(countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput)), countNeurons(nOfInput, innerNeurons, nOfOutput), rnd),
        unflatHebbianCoefficients(hebbianCoefficients, countNeurons(nOfInput, innerNeurons, nOfOutput)),
        countNeurons(nOfInput, innerNeurons, nOfOutput),
        eta,
        weightsNormalization
    );
  }

  public HebbianMultiLayerPerceptron(ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[][][] eta, boolean weightsNormalization) {
    this(activationFunction, nOfInput, innerNeurons, nOfOutput, new double[countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput))], flatHebbianCoefficients(initHebbianCoefficients(countNeurons(nOfInput, innerNeurons, nOfOutput)), countNeurons(nOfInput, innerNeurons, nOfOutput)), eta, weightsNormalization);
  }

  @Override
  public void reset() {
    for (int i = 0; i < weights.length; i++) {
      for (int j = 0; j < weights[i].length; j++) {
        System.arraycopy(this.startingWeights[i][j], 0, this.weights[i][j], 0, weights[i][j].length);
      }
    }
  }

  public void setInitialWeights(double[] params) {
    double[][][] newWeights = unflat(params, neurons);
    for (int l = 0; l < newWeights.length; l++) {
      for (int s = 0; s < newWeights[l].length; s++) {
        System.arraycopy(newWeights[l][s], 0, startingWeights[l][s], 0, newWeights[l][s].length);
      }
    }
  }

  public void setWeights(double[] params) {
    double[][][] newWeights = unflat(params, neurons);
    for (int l = 0; l < newWeights.length; l++) {
      for (int s = 0; s < newWeights[l].length; s++) {
        System.arraycopy(newWeights[l][s], 0, weights[l][s], 0, newWeights[l][s].length);
      }
    }
  }

  public double[][][] getStartingWeights() {
    return startingWeights;
  }

  private static double[][][] generateRandomWeights(int nOfWeights, int[] neurons, Random random) {
    double[] randomWeights = IntStream.range(0, nOfWeights)
        .mapToDouble(i -> random != null ? (random.nextDouble() * 2) - 1 : 0d)
        .toArray();
    return unflat(randomWeights, neurons);
  }

  public static int countHebbianCoefficients(int nOfInput, int[] innerNeurons, int nOfOutput) {
    return 4 * countWeights(nOfInput, innerNeurons, nOfOutput);
  }

  public static int countHebbianCoefficients(int[] neurons) {
    return 4 * countWeights(neurons);
  }

  public static double[][][][] unflatHebbianCoefficients(double[] hebbianCoefficients, int[] neurons) {
    double[][][][] unflatHebbianCoefficients = new double[neurons.length - 1][][][];
    int c = 0;
    for (int i = 1; i < neurons.length; i++) {
      unflatHebbianCoefficients[i - 1] = new double[neurons[i]][neurons[i - 1] + 1][4];
      for (int j = 0; j < neurons[i]; j++) {
        for (int k = 0; k < neurons[i - 1] + 1; k++) {
          for (int v = 0; v < 4; v++) {
            unflatHebbianCoefficients[i - 1][j][k][v] = hebbianCoefficients[c];
            c = c + 1;
          }
        }
      }
    }
    return unflatHebbianCoefficients;
  }

  public static double[] flatHebbianCoefficients(double[][][][] hebbianCoefficients, int[] neurons) {
    double[] flatHebbianCoefficients = new double[countHebbianCoefficients(neurons)];
    int c = 0;
    for (int i = 1; i < neurons.length; i++) {
      for (int j = 0; j < neurons[i]; j++) {
        for (int k = 0; k < neurons[i - 1] + 1; k++) {
          for (int v = 0; v < 4; v++) {
            flatHebbianCoefficients[c] = hebbianCoefficients[i - 1][j][k][v];
            c = c + 1;
          }
        }
      }
    }
    return flatHebbianCoefficients;
  }

  private static double[][][][] initHebbianCoefficients(int[] neurons) {
    double[][][][] hebbianCoefficients = new double[neurons.length - 1][][][];
    for (int i = 1; i < neurons.length; i++) {
      hebbianCoefficients[i - 1] = new double[neurons[i]][neurons[i - 1] + 1][4];
      for (int j = 0; j < neurons[i]; j++) {
        for (int k = 0; k < neurons[i - 1] + 1; k++) {
          hebbianCoefficients[i - 1][j][k] = new double[]{1d, 2d, 3d, 4d};
        }
      }
    }
    return hebbianCoefficients;
  }

  private static double[] rescale(double[] vector, double min, double max) {
    double minValue = Arrays.stream(vector).min().getAsDouble();
    double maxValue = Arrays.stream(vector).max().getAsDouble();
    if (minValue != maxValue) {
      vector = Arrays.stream(vector).map(v -> {
        Double tmp = (max - min) * ((v - minValue) / (maxValue - minValue)) + min;
        if (tmp.isNaN()) {
          throw new IllegalArgumentException("NaN value found.");
        }
        return tmp;
      }).toArray();
    }
    return vector;
  }

  private void hebbianUpdate(double[][] values) {
    for (int layer = 1; layer < neurons.length; layer++) {
      for (int destNeuron = 0; destNeuron < neurons[layer]; destNeuron++) {
        // update bias
        double dW = eta[layer - 1][destNeuron][0] * (
            hebbianCoefficients[layer - 1][destNeuron][0][0] * values[layer][destNeuron] * weights[layer - 1][destNeuron][0] +   // A*a_i,j*a_j,k
                hebbianCoefficients[layer - 1][destNeuron][0][1] * values[layer][destNeuron] +                 // B*a_i,j
                hebbianCoefficients[layer - 1][destNeuron][0][2] * weights[layer - 1][destNeuron][0] +                 // C*a_j,k
                hebbianCoefficients[layer - 1][destNeuron][0][3]                                  // D
        );
        weights[layer - 1][destNeuron][0] = weights[layer - 1][destNeuron][0] + dW;
        // update weights
        for (int startNeuron = 1; startNeuron < neurons[layer - 1] + 1; startNeuron++) {
          dW = eta[layer - 1][destNeuron][startNeuron] * (
              hebbianCoefficients[layer - 1][destNeuron][startNeuron][0] * values[layer][destNeuron] * values[layer - 1][startNeuron - 1] +   // A*a_i,j*a_j,k
                  hebbianCoefficients[layer - 1][destNeuron][startNeuron][1] * values[layer][destNeuron] +                 // B*a_i,j
                  hebbianCoefficients[layer - 1][destNeuron][startNeuron][2] * values[layer - 1][startNeuron - 1] +                 // C*a_j,k
                  hebbianCoefficients[layer - 1][destNeuron][startNeuron][3]                                  // D
          );
          weights[layer - 1][destNeuron][startNeuron] = weights[layer - 1][destNeuron][startNeuron] + dW;
        }
      }
    }
  }

  private void hebbianNormalization() {
    for (int l = 1; l < neurons.length; l++) {
      for (int o = 0; o < neurons[l]; o++) {
        double[] norm = rescale(weights[l - 1][o], -1, 1);
        if (neurons[l - 1] >= 0) {
          System.arraycopy(norm, 0, weights[l - 1][o], 0, neurons[l - 1]);
        }
      }
    }
  }

  @Override
  public double[] getParams() {
    return flatHebbianCoefficients(hebbianCoefficients, neurons);
  }

  @Override
  public void setParams(double[] params) {
    double[][][][] tmp = unflatHebbianCoefficients(params, neurons);
    for (int i = 1; i < neurons.length; i++) {
      for (int j = 0; j < neurons[i]; j++) {
        for (int k = 0; k < neurons[i - 1] + 1; k++) {
          System.arraycopy(tmp[i - 1][j][k], 0, hebbianCoefficients[i - 1][j][k], 0, 4);
        }
      }
    }
  }

  public double[] getEta() {
    return flat(eta, neurons);
  }

  public void setEta(double[] etas) {
    double[][][] newEtas = unflat(etas, neurons);
    for (int l = 0; l < newEtas.length; l++) {
      for (int s = 0; s < newEtas[l].length; s++) {
        System.arraycopy(newEtas[l][s], 0, eta[l][s], 0, newEtas[l][s].length);
      }
    }
  }

  private static double[][][] initEta(double eta, int[] neurons) {
    double[][][] unflatEtas = new double[neurons.length - 1][][];
    for (int i = 1; i < neurons.length; i++) {
      unflatEtas[i - 1] = new double[neurons[i]][neurons[i - 1] + 1];
      for (int j = 0; j < neurons[i]; j++) {
        for (int k = 0; k < neurons[i - 1] + 1; k++) {
          unflatEtas[i - 1][j][k] = eta;
        }
      }
    }
    return unflatEtas;
  }

  @Override
  public double[] apply(double[] input) {
    super.apply(input);
    if (learning) {
      hebbianUpdate(activationValues);
      if (weightsNormalization) {
        hebbianNormalization();
      }
    }
    return activationValues[neurons.length - 1];
  }

  private double[][][] deepCopy(double[][][] matrix) {
    double[][][] save = new double[matrix.length][][];
    for (int i = 0; i < matrix.length; i++) {
      save[i] = new double[matrix[i].length][];
      for (int j = 0; j < matrix[i].length; j++)
        save[i][j] = Arrays.copyOf(matrix[i][j], matrix[i][j].length);
    }
    return save;
  }

  public boolean invertLearning() {
    this.learning = !learning;
    return learning;
  }

  public void stopLearning() {
    this.learning = false;
  }

  public void startLearning() {
    this.learning = true;
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 67 * hash + Objects.hashCode(this.activationFunction);
    hash = 67 * hash + Arrays.deepHashCode(this.weights);
    hash = 67 * hash + Arrays.hashCode(this.neurons);
    hash = 67 * hash + Arrays.deepHashCode(this.hebbianCoefficients);
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
    final HebbianMultiLayerPerceptron other = (HebbianMultiLayerPerceptron) obj;
    if (this.activationFunction != other.activationFunction) {
      return false;
    }
    if (!Arrays.deepEquals(this.weights, other.weights)) {
      return false;
    }
    if (!Arrays.deepEquals(this.hebbianCoefficients, other.hebbianCoefficients)) {
      return false;
    }
    return Arrays.equals(this.neurons, other.neurons);
  }

  @Override
  public String toString() {
    return "hMLP." + activationFunction.toString().toLowerCase() + "[" +
        Arrays.stream(neurons).mapToObj(Integer::toString).collect(Collectors.joining(","))
        + "]";
  }

}

