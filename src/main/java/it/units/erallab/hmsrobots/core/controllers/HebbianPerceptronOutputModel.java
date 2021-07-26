package it.units.erallab.hmsrobots.core.controllers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.util.Parametrized;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HebbianPerceptronOutputModel implements Serializable, RealFunction, Parametrized {


    public enum ActivationFunction {
        RELU((Double x) -> (x < 0) ? 0d : x),
        SIGMOID((Double x) -> 1d / (1d + Math.exp(-x))),
        TANH(Math::tanh);

        private final Function<Double, Double> f;

        ActivationFunction(Function<Double, Double> f) {
            this.f = f;
        }

    }

    @JsonProperty
    private final ActivationFunction activationFunction;
    @JsonProperty
    private final double[][][] weights;
    @JsonProperty
    private final double[][][] startingWeights;
    @JsonProperty
    double[][] hebbCoef;
    @JsonProperty
    private final int[] neurons;
    @JsonProperty
    private final double eta;
    @JsonProperty
    private final HashSet<Integer> disabled;
    @JsonProperty
    private final HashMap<Integer, Integer> mapper;

    @JsonCreator
    public HebbianPerceptronOutputModel(
            @JsonProperty("activationFunction") ActivationFunction activationFunction,
            @JsonProperty("weights") double[][][] weights,
            @JsonProperty("hebbCoef") double[][] hebbCoef,
            @JsonProperty("neurons") int[] neurons,
            @JsonProperty("eta") double eta,
            @JsonProperty("disabled") HashSet<Integer> disabled,
            @JsonProperty("mapper") HashMap<Integer, Integer> mapper
    ) {
        this.activationFunction = activationFunction;
        this.weights = weights;
        this.startingWeights = deepCopy(weights);
        this.neurons = neurons;
        this.hebbCoef = hebbCoef;
        this.eta = eta;
        this.disabled = disabled;
        if (flat(weights, neurons).length != countWeights(neurons)) {
            throw new IllegalArgumentException(String.format(
                    "Wrong number of weights: %d expected, %d found",
                    countWeights(neurons),
                    flat(weights, neurons).length
            ));
        }
        this.mapper = mapper;
        //System.out.println(neurons[neurons.length-1]);
        //System.out.println(neurons.length-1);
        if (flatHebbCoef(hebbCoef, neurons[neurons.length - 1]).length != 4 * neurons[neurons.length - 1]) {
            throw new IllegalArgumentException(String.format(
                    "Wrong number of hebbian coeff: %d   expected, %d found",
                    4 * neurons[neurons.length - 1],
                    flatHebbCoef(hebbCoef, neurons[neurons.length - 1]).length
            ));
        }
    }


    public HebbianPerceptronOutputModel(ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, double[] hebbCoef, double eta, HashSet<Integer> disabled, HashMap<Integer, Integer> mapper) {
        this(
                activationFunction,
                unflat(weights, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                unflatHebbCoef(hebbCoef, nOfOutput),
                countNeurons(nOfInput, innerNeurons, nOfOutput),
                eta,
                disabled,
                mapper

        );
    }

    public HebbianPerceptronOutputModel(ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[] hebbCoef, double eta, Random rnd, HashSet<Integer> disabled, HashMap<Integer, Integer> mapper) {

        this(
                activationFunction,
                randW(countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput)), countNeurons(nOfInput, innerNeurons, nOfOutput), rnd),
                unflatHebbCoef(hebbCoef, nOfOutput),
                countNeurons(nOfInput, innerNeurons, nOfOutput),
                eta,
                disabled,
                mapper
        );
    }

    public HebbianPerceptronOutputModel(ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double eta, HashSet<Integer> disabled, HashMap<Integer, Integer> mapper) {
        this(activationFunction, nOfInput, innerNeurons, nOfOutput, new double[countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput))], flatHebbCoef(initHebbCoef(nOfOutput), nOfOutput), eta, disabled, mapper);

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

    public static double[][] unflatHebbCoef(double[] hebbCoef, int inputs) {
        double[][] unflatWeights = new double[inputs][];
        int c = 0;
        for (int i = 0; i < inputs; i++) {
            unflatWeights[i] = new double[4];
            for (int l = 0; l < 4; l++) {
                unflatWeights[i][l] = hebbCoef[c];
                c = c + 1;
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

    public static double[] flatHebbCoef(double[][] hebbCoef, int inputs) {
        int n = 0;
        /*for (int i = 0; i < neurons.length - 1; i++) {
            n = n + neurons[i] * neurons[i + 1];
        }*/
        n = 4 * (inputs);
        //System.out.println(n);
        double[] flatHebbCoef = new double[n];
        int c = 0;
        for (int i = 0; i < inputs; i++) {
            for (int l = 0; l < 4; l++) {
                flatHebbCoef[c] = hebbCoef[i][l];
                c = c + 1;
            }
        }
        return flatHebbCoef;
    }


    public static double[][] initHebbCoef(int inputs) {
        double[][] unflatHebbCoef = new double[inputs][];
        int c = 0;
        for (int i = 0; i < inputs; i++) {
            unflatHebbCoef[i] = new double[]{0, 0, 0, 0};
        }

        return unflatHebbCoef;
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
        int largestLayerSize = Arrays.stream(neurons).max().orElse(0);
        double[][][] fakeWeights = new double[neurons.length][][];
        double[][] fakeLayerWeights = new double[largestLayerSize][];
        Arrays.fill(fakeLayerWeights, new double[largestLayerSize]);
        Arrays.fill(fakeWeights, fakeLayerWeights);
        return flat(fakeWeights, neurons).length;
    }

    public static int countWeights(int nOfInput, int[] innerNeurons, int nOfOutput) {
        return countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput));
    }

    public static int countHebbCoef(int nOfInput, int[] innerNeurons, int nOfOutput) {
        return 4*nOfOutput;
    }

    private double norm2(double[] vector) {
        double norm = 0d;
        for (int i = 0; i < vector.length; i++) {
            norm += vector[i] * vector[i];
        }
        return Math.sqrt(norm);

    }

    public void hebbianUpdate(double[][] values) {
        for (int l = 1; l < neurons.length; l++) {
            for (int o = 0; o < neurons[l]; o++) {
                for (int i = 0; i < neurons[l - 1]; i++) {
                    if ((l == 1 && !disabled.contains(i)) || l > 1) {
                        //System.out.println(o);
                        double dW = eta * (
                                hebbCoef[o][0] * values[l][o] * values[l - 1][i] +   // A*a_i,j*a_j,k
                                        hebbCoef[o][1] * values[l][o] +                 // B*a_i,j
                                        hebbCoef[o][2] * values[l - 1][i] +                 // C*a_j,k
                                        hebbCoef[o][3]                                  // D
                        );
                        weights[l - 1][i][o] = weights[l - 1][i][o] + dW;
                    }
                }
            }
        }

    }


    public void hebbianNormalization() {
        /*double[] fw = flat(weights,neurons);
        double max = Arrays.stream(fw).max().getAsDouble();
        double min = Arrays.stream(fw).min().getAsDouble();
        double norm = norm2(fw);*/
        for (int l = 1; l < neurons.length; l++) {
            for (int o = 0; o < neurons[l]; o++) {
                double[] v_j = new double[neurons[l - 1]];
                for (int i = 0; i < neurons[l - 1]; i++) {
                    v_j[i] = weights[l - 1][i][o];
                }
                double norm = norm2(v_j);

                for (int i = 0; i < neurons[l - 1]; i++) {
                    weights[l - 1][i][o] /= norm;
                    //weights[l - 1][i][o] *= 100;
                }
            }
        }

    }

    public void resetInitWeights() {
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                for (int k = 0; k < weights[i][j].length; k++) {
                    this.weights[i][j][k] = this.startingWeights[i][j][k];
                }
            }
        }
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

    @Override
    public double[] apply(double[] input) {
        if (input.length != neurons[0]) {
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

        hebbianUpdate(values);
        //hebbianNormalization();

        return values[neurons.length - 1];
    }

    public double[][][] getWeightsM() {
        return weights;
    }

    public int[] getNeurons() {
        return neurons;
    }


    public double[] getWeights() {
        return flat(weights, neurons);
    }

    public double[] getStartingWeights() {
        return flat(startingWeights, neurons);
    }

    @Override
    public double[] getParams() {
        return flatHebbCoef(hebbCoef, neurons[neurons.length - 1]);
    }


    public void setWeights(double[] params) {
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
    public void setParams(double[] params) {
        double[][] tmp = unflatHebbCoef(params, weights[weights.length - 1].length);
        for (int i = 0; i < weights[0].length; i++) {
            for (int j = 0; j < 4; j++) {
                hebbCoef[i][j] = tmp[i][j];
            }
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this.activationFunction);
        hash = 67 * hash + Arrays.deepHashCode(this.weights);
        hash = 67 * hash + Arrays.hashCode(this.neurons);
        hash = 67 * hash + Arrays.hashCode(this.hebbCoef);
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
        final HebbianPerceptronOutputModel other = (HebbianPerceptronOutputModel) obj;
        if (this.activationFunction != other.activationFunction) {
            return false;
        }
        if (!Arrays.deepEquals(this.weights, other.weights)) {
            return false;
        }
        if (!Arrays.deepEquals(this.hebbCoef, other.hebbCoef)) {
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

    public static double[][][] randW(int nw, int[] neurons, Random rnd) {
        double[] randomWeights = new double[nw];
        for (int i = 0; i < nw; i++) {
            if (rnd != null) {
                //System.out.println("not null");
                randomWeights[i] = (rnd.nextDouble() * 2) - 1;
            } else {
                //System.out.println("null");
                randomWeights[i] = 0d;
            }
        }
        return unflat(randomWeights, neurons);
    }

    @Override
    public int getInputDimension() {
        return neurons[0];
    }

    @Override
    public int getOutputDimension() {
        return neurons[neurons.length - 1];
    }

}
