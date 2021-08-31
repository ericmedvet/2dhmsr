package it.units.erallab.hmsrobots.core.controllers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.util.Parametrized;
import org.apache.commons.math3.util.Pair;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HebbianPerceptronFullModel implements Serializable, RealFunction, Parametrized {


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
    private final HebbianPerceptronFullModel.ActivationFunction activationFunction;
    @JsonProperty
    private final double[][][] weights;
    @JsonProperty
    private final double[][][] startingWeights;
    @JsonProperty
    double[][][][] hebbCoef;
    @JsonProperty
    private final int[] neurons;
    @JsonProperty
    private final double[][][] eta;
    @JsonProperty
    private final HashSet<Integer> disabled;
    @JsonProperty
    private final HashMap<Integer, Integer> mapper;
    @JsonProperty
    private final double[] normalization;

    @JsonCreator
    public HebbianPerceptronFullModel(
            @JsonProperty("activationFunction") HebbianPerceptronFullModel.ActivationFunction activationFunction,
            @JsonProperty("weights") double[][][] weights,
            @JsonProperty("hebbCoef") double[][][][] hebbCoef,
            @JsonProperty("neurons") int[] neurons,
            @JsonProperty("eta") double[][][] eta,
            @JsonProperty("disabled") HashSet<Integer> disabled,
            @JsonProperty("mapper") HashMap<Integer, Integer> mapper,
            @JsonProperty("normalization") double[] normalization
    ) {
        this.activationFunction = activationFunction;
        this.weights = weights;
        this.startingWeights = deepCopy(weights);
        this.neurons = neurons;
        this.hebbCoef = hebbCoef;

        this.disabled = disabled;
        if (flat(weights, neurons).length != countWeights(neurons)) {
            throw new IllegalArgumentException(String.format(
                    "Wrong number of weights: %d expected, %d found",
                    countWeights(neurons),
                    flat(weights, neurons).length
            ));
        }
        this.normalization = normalization;
        this.mapper = mapper;
        this.eta = eta;
        if (flatHebbCoef(hebbCoef, neurons).length != 4 * countWeights(neurons)) {
            throw new IllegalArgumentException(String.format(
                    "Wrong number of hebbian coeff: %d   expected, %d found",
                    4 * countWeights(neurons),
                    flatHebbCoef(hebbCoef, neurons).length
            ));
        }
    }

    public HebbianPerceptronFullModel(HebbianPerceptronFullModel.ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, double[] hebbCoef, double eta, HashSet<Integer> disabled, HashMap<Integer, Integer> mapper) {
        this(
                activationFunction,
                unflat(weights, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                unflatHebbCoef(hebbCoef, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                countNeurons(nOfInput, innerNeurons, nOfOutput),
                initEta(eta, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                disabled,
                mapper,
                null

        );
    }

    public HebbianPerceptronFullModel(HebbianPerceptronFullModel.ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, double[] hebbCoef, double eta, HashSet<Integer> disabled, HashMap<Integer, Integer> mapper, double[] normalization) {
        this(
                activationFunction,
                unflat(weights, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                unflatHebbCoef(hebbCoef, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                countNeurons(nOfInput, innerNeurons, nOfOutput),
                initEta(eta, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                disabled,
                mapper,
                normalization

        );
    }

    public HebbianPerceptronFullModel(HebbianPerceptronFullModel.ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[] hebbCoef, double eta, Random rnd, HashSet<Integer> disabled, HashMap<Integer, Integer> mapper, double[] normalization) {

        this(
                activationFunction,
                randW(countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput)), countNeurons(nOfInput, innerNeurons, nOfOutput), rnd),
                unflatHebbCoef(hebbCoef, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                countNeurons(nOfInput, innerNeurons, nOfOutput),
                initEta(eta, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                disabled,
                mapper,
                normalization
        );
    }

    public HebbianPerceptronFullModel(HebbianPerceptronFullModel.ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double eta, HashSet<Integer> disabled, HashMap<Integer, Integer> mapper, double[] normalization) {
        this(activationFunction, nOfInput, innerNeurons, nOfOutput, new double[countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput))], flatHebbCoef(initHebbCoef(countNeurons(nOfInput, innerNeurons, nOfOutput)), countNeurons(nOfInput, innerNeurons, nOfOutput)), eta, disabled, mapper, normalization);

    }

    public HebbianPerceptronFullModel(HebbianPerceptronFullModel.ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, double[] hebbCoef, double[][][] eta, HashSet<Integer> disabled, HashMap<Integer, Integer> mapper, double[] normalization) {
        this(
                activationFunction,
                unflat(weights, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                unflatHebbCoef(hebbCoef, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                countNeurons(nOfInput, innerNeurons, nOfOutput),
                eta,
                disabled,
                mapper,
                normalization

        );
    }

    public HebbianPerceptronFullModel(HebbianPerceptronFullModel.ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[] hebbCoef, double[][][] eta, Random rnd, HashSet<Integer> disabled, HashMap<Integer, Integer> mapper, double[] normalization) {

        this(
                activationFunction,
                randW(countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput)), countNeurons(nOfInput, innerNeurons, nOfOutput), rnd),
                unflatHebbCoef(hebbCoef, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                countNeurons(nOfInput, innerNeurons, nOfOutput),
                eta,
                disabled,
                mapper,
                normalization
        );
    }

    public HebbianPerceptronFullModel(HebbianPerceptronFullModel.ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[][][] eta, HashSet<Integer> disabled, HashMap<Integer, Integer> mapper, double[] normalization) {
        this(activationFunction, nOfInput, innerNeurons, nOfOutput, new double[countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput))], flatHebbCoef(initHebbCoef(countNeurons(nOfInput, innerNeurons, nOfOutput)), countNeurons(nOfInput, innerNeurons, nOfOutput)), eta, disabled, mapper, normalization);

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

    public static double[][][][] unflatHebbCoef(double[] hebbCoef, int[] neurons) {
        double[][][][] unflatWeights = new double[neurons.length - 1][][][];
        int c = 0;
        for (int i = 0; i < neurons.length - 1; i++) {
            unflatWeights[i] = new double[neurons[i]][neurons[i + 1]][4];
            for (int j = 0; j < neurons[i]; j++) {
                for (int k = 0; k < neurons[i + 1]; k++) {
                    for (int o = 0; o < 4; o++) {
                        unflatWeights[i][j][k][o] = hebbCoef[c];
                        c = c + 1;
                    }
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

    public static double[] flatHebbCoef(double[][][][] hebbCoef, int[] neurons) {
        int n;
        /*for (int i = 0; i < neurons.length - 1; i++) {
            n = n + neurons[i] * neurons[i + 1];
        }*/
        n = 4 * countWeights(neurons);
        //System.out.println(n);
        double[] flatHebbCoef = new double[n];
        int c = 0;
        for (int i = 0; i < neurons.length - 1; i++) {
            for (int j = 0; j < neurons[i]; j++) {
                for (int k = 0; k < neurons[i + 1]; k++) {
                    for (int o = 0; o < 4; o++) {
                        flatHebbCoef[c] = hebbCoef[i][j][k][o];
                        c = c + 1;
                    }
                }
            }
        }
        return flatHebbCoef;
    }


    public static double[][][][] initHebbCoef(int[] neurons) {
        double[][][][] unflatHebbCoef = new double[neurons.length - 1][][][];
        int c = 0;
        for (int i = 0; i < neurons.length - 1; i++) {
            for (int j = 0; j < neurons[i]; j++) {
                for (int k = 0; k < neurons[i + 1]; k++) {
                    unflatHebbCoef[i][j][k] = new double[]{0, 0, 0, 0};
                }
            }
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
        StringBuilder s = new StringBuilder();
        int[] n = countNeurons(nOfInput, innerNeurons, nOfOutput);
        for (int i : n) {
            s.append(i).append(" ");
        }
        return countWeights(n);
    }

    public static int countHebbCoef(int nOfInput, int[] innerNeurons, int nOfOutput) {
        return 4 * countWeights(nOfInput, innerNeurons, nOfOutput);
    }

    private double norm2(double[] vector) {
        double norm = 0d;
        for (int i = 0; i < vector.length; i++) {
            norm += vector[i] * vector[i];
        }
        return Math.sqrt(norm);

    }

    private double[] norm(double[] vector) {
        double mmin = Arrays.stream(vector).min().getAsDouble();
        double mmax = Arrays.stream(vector).max().getAsDouble();
        return Arrays.stream(vector).sequential().map(d -> 2 * ((d - mmin) / (mmax - mmin)) - 1).toArray();
    }

    private double[] bound(double[] vector) {
        for (int i = 0; i < vector.length; i++) {
            if (vector[i] < this.normalization[0]) {
                vector[i] = this.normalization[0];
            }
            if (vector[i] > this.normalization[1]) {
                vector[i] = this.normalization[1];
            }
        }
        return vector;
    }

    public void hebbianUpdate(double[][] values) {
        for (int l = 1; l < neurons.length; l++) {
            for (int o = 0; o < neurons[l]; o++) {
                for (int i = 0; i < neurons[l - 1]; i++) {
                    if ((l == 1 && !disabled.contains(i)) || l > 1) {
                        //System.out.println(o);
                        double dW = eta[l - 1][i][o] * (
                                hebbCoef[l - 1][i][o][0] * values[l][o] * values[l - 1][i] +   // A*a_i,j*a_j,k
                                        hebbCoef[l - 1][i][o][1] * values[l][o] +                 // B*a_i,j
                                        hebbCoef[l - 1][i][o][2] * values[l - 1][i] +                 // C*a_j,k
                                        hebbCoef[l - 1][i][o][3]                                  // D
                        );
                        weights[l - 1][i][o] = weights[l - 1][i][o] + dW;
                    }
                }
            }
        }

    }


    public void hebbianNormalization() {
        for (int l = 1; l < neurons.length; l++) {
            for (int o = 0; o < neurons[l]; o++) {
                double[] v_j = new double[neurons[l - 1]];
                for (int i = 0; i < neurons[l - 1]; i++) {
                    v_j[i] = weights[l - 1][i][o];
                }
                double[] norm;
                if (this.normalization[0] == this.normalization[1]) {
                    norm = norm(v_j);
                } else {
                    norm = bound(v_j);
                }

                for (int i = 0; i < neurons[l - 1]; i++) {
                    weights[l - 1][i][o] = v_j[i];
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
            throw new IllegalArgumentException(String.format("Expected input length is %d: found %d", neurons[0], input.length));
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

        if (!(this.normalization == null)) {
            hebbianNormalization();
        }

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
        return flatHebbCoef(hebbCoef, neurons);
    }


    public void setWeights(double[] params) {
        double[][][] newWeights = unflat(params, neurons);
        for (int l = 0; l < newWeights.length; l++) {
            for (int s = 0; s < newWeights[l].length; s++) {
                for (int d = 0; d < newWeights[l][s].length; d++) {
                    weights[l][s][d] = newWeights[l][s][d];
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
                for (int d = 0; d < newEtas[l][s].length; d++) {
                    eta[l][s][d] = newEtas[l][s][d];
                }
            }
        }
    }

    public static double[][][] initEta(double initEta, int[] neurons) {
        double[][][] unflatWeights = new double[neurons.length - 1][][];
        int c = 0;
        for (int i = 0; i < neurons.length - 1; i++) {
            unflatWeights[i] = new double[neurons[i]][neurons[i + 1]];
            for (int j = 0; j < neurons[i]; j++) {
                for (int k = 0; k < neurons[i + 1]; k++) {
                    unflatWeights[i][j][k] = initEta;
                    c = c + 1;
                }
            }
        }
        return unflatWeights;
    }

    @Override
    public void setParams(double[] params) {
        double[][][][] tmp = unflatHebbCoef(params, neurons);
        for (int i = 0; i < neurons.length - 1; i++) {
            for (int j = 0; j < neurons[i]; j++) {
                for (int k = 0; k < neurons[i + 1]; k++) {
                    for (int o = 0; o < 4; o++) {
                        hebbCoef[i][j][k][o] = tmp[i][j][k][o];
                    }
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
        final HebbianPerceptronFullModel other = (HebbianPerceptronFullModel) obj;
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
        return "HLP-full." + activationFunction.toString().toLowerCase() + "[" +
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
        //System.out.println("ahahaha "+neurons[0]);
        return neurons[0];
    }

    @Override
    public int getOutputDimension() {
        return neurons[neurons.length - 1];
    }

}

