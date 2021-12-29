package it.units.erallab.hmsrobots.core.controllers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.snapshots.MLPState;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;

import java.util.*;
import java.util.stream.Collectors;

public class HebbianPerceptron extends MultiLayerPerceptron implements Resettable {

    @JsonProperty
    private final double[][][] startingWeights;
    @JsonProperty
    double[][][][] hebbCoef;
    @JsonProperty
    private final double[][][] eta;
    @JsonProperty
    private final boolean toNormalize;
    private boolean learning = true;

    @JsonCreator
    public HebbianPerceptron(
            @JsonProperty("activationFunction") ActivationFunction activationFunction,
            @JsonProperty("weights") double[][][] weights,
            @JsonProperty("hebbCoef") double[][][][] hebbCoef,
            @JsonProperty("neurons") int[] neurons,
            @JsonProperty("eta") double[][][] eta,
            @JsonProperty("toNormalize") boolean toNormalize
    ) {
        super(activationFunction, weights, neurons);
        // ricordati che i pesi sono salvati come layer output input
        this.startingWeights = deepCopy(weights);
        this.hebbCoef = hebbCoef;
        this.toNormalize = toNormalize;
        this.eta = eta;


        if (flatHebbCoef(hebbCoef, neurons).length != 4 * countWeights(neurons)) {
            throw new IllegalArgumentException(String.format(
                    "Wrong number of hebbian coeff: %d   expected, %d found",
                    4 * countWeights(neurons),
                    flatHebbCoef(hebbCoef, neurons).length
            ));
        }
    }

    public HebbianPerceptron(ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, double[] hebbCoef, double eta, boolean toNormalize) {
        this(
                activationFunction,
                unflat(weights, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                unflatHebbCoef(hebbCoef, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                countNeurons(nOfInput, innerNeurons, nOfOutput),
                initEta(eta, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                toNormalize

        );
    }

    public HebbianPerceptron(ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[] hebbCoef, double eta, Random rnd, boolean toNormalize) {

        this(
                activationFunction,
                randomWeights(countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput)), countNeurons(nOfInput, innerNeurons, nOfOutput), rnd),
                unflatHebbCoef(hebbCoef, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                countNeurons(nOfInput, innerNeurons, nOfOutput),
                initEta(eta, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                toNormalize
        );
    }

    public HebbianPerceptron(ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double eta, boolean toNormalize) {
        this(activationFunction, nOfInput, innerNeurons, nOfOutput, new double[countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput))], flatHebbCoef(initHebbCoef(countNeurons(nOfInput, innerNeurons, nOfOutput)), countNeurons(nOfInput, innerNeurons, nOfOutput)), eta, toNormalize);

    }

    public HebbianPerceptron(ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, double[] hebbCoef, double[][][] eta, boolean toNormalize) {
        this(
                activationFunction,
                unflat(weights, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                unflatHebbCoef(hebbCoef, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                countNeurons(nOfInput, innerNeurons, nOfOutput),
                eta,
                toNormalize

        );
    }

    public HebbianPerceptron(ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[] hebbCoef, double[][][] eta, Random rnd, boolean toNormalize) {

        this(
                activationFunction,
                randomWeights(countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput)), countNeurons(nOfInput, innerNeurons, nOfOutput), rnd),
                unflatHebbCoef(hebbCoef, countNeurons(nOfInput, innerNeurons, nOfOutput)),
                countNeurons(nOfInput, innerNeurons, nOfOutput),
                eta,
                toNormalize
        );
    }

    public HebbianPerceptron(ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput, double[][][] eta, boolean toNormalize) {
        this(activationFunction, nOfInput, innerNeurons, nOfOutput, new double[countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput))], flatHebbCoef(initHebbCoef(countNeurons(nOfInput, innerNeurons, nOfOutput)), countNeurons(nOfInput, innerNeurons, nOfOutput)), eta, toNormalize);

    }


    @Override
    public Snapshot getSnapshot() {
        return new Snapshot(new MLPState(this.activationValues, this.weights, this.activationFunction.getDomain()), this.getClass());
    }

    /*
     *
     * Weights
     *
     * */

    @Override
    public void reset() {
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                for (int k = 0; k < weights[i][j].length; k++) {
                    this.weights[i][j][k] = this.startingWeights[i][j][k];
                }
            }
        }
    }

    public void setInitWeights(double[] params) {
        double[][][] newWeights = unflat(params, neurons);
        for (int l = 0; l < newWeights.length; l++) {
            for (int s = 0; s < newWeights[l].length; s++) {
                for (int d = 0; d < newWeights[l][s].length; d++) {
                    startingWeights[l][s][d] = newWeights[l][s][d];
                }
            }
        }
    }

    public double[][][] getStartingWeights() {
        return startingWeights;
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

    public static double[][][] randomWeights(int nw, int[] neurons, Random rnd) {
        double[] randomWeights = new double[nw];
        for (int i = 0; i < nw; i++) {
            if (rnd != null) {
                randomWeights[i] = (rnd.nextDouble() * 2) - 1;
            } else {
                randomWeights[i] = 0d;
            }
        }
        return unflat(randomWeights, neurons);
    }

    /*
     *
     *  Hebbian coefs
     *
     * */
    public static int countHebbCoef(int nOfInput, int[] innerNeurons, int nOfOutput) {
        return 4 * countWeights(nOfInput, innerNeurons, nOfOutput);
    }

    public static int countHebbCoef(int[] neurons) {
        return 4 * countWeights(neurons);
    }

    public static double[][][][] unflatHebbCoef(double[] hebbCoef, int[] neurons) {
        double[][][][] unflatHebbCoef = new double[neurons.length - 1][][][];
        int c = 0;
        for (int i = 1; i < neurons.length; i++) {
            unflatHebbCoef[i - 1] = new double[neurons[i]][neurons[i - 1] + 1][4];
            for (int j = 0; j < neurons[i]; j++) {
                for (int k = 0; k < neurons[i - 1] + 1; k++) {
                    for (int v = 0; v < 4; v++) {
                        unflatHebbCoef[i - 1][j][k][v] = hebbCoef[c];
                        c = c + 1;
                    }
                }
            }
        }
        return unflatHebbCoef;
    }

    public static double[] flatHebbCoef(double[][][][] hebbCoef, int[] neurons) {
        int n;
        n = countHebbCoef(neurons);
        double[] flatHebbCoef = new double[n];
        int c = 0;
        for (int i = 1; i < neurons.length; i++) {
            for (int j = 0; j < neurons[i]; j++) {
                for (int k = 0; k < neurons[i - 1] + 1; k++) {
                    for (int v = 0; v < 4; v++) {
                        flatHebbCoef[c] = hebbCoef[i-1][j][k][v];
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
        for (int i = 1; i < neurons.length; i++) {
            unflatHebbCoef[i - 1] = new double[neurons[i]][neurons[i - 1] + 1][4];
            for (int j = 0; j < neurons[i]; j++) {
                for (int k = 0; k < neurons[i - 1] + 1; k++) {
                    unflatHebbCoef[i-1][j][k] = new double[]{1d, 2d, 3d, 4d};
                }
            }
        }

        return unflatHebbCoef;
    }


    private double[] norm(double[] vector) {
        double mmin = Arrays.stream(vector).min().getAsDouble();
        double mmax = Arrays.stream(vector).max().getAsDouble();
        if (mmin != mmax) {
            for (int i = 0; i < vector.length; i++) {
                Double tmp = 2 * ((vector[i] - mmin) / (mmax - mmin)) - 1;
                if (tmp.isNaN()) {
                    throw new IllegalArgumentException(String.format("NaN weights panic!!!"));
                }
                vector[i] = tmp;
            }
        }

        return vector;
    }

    public void hebbianUpdate(double[][] values) {

        for (int l = 1; l < neurons.length; l++) {
            for (int o = 0; o < neurons[l]; o++) {
                //update bias for each level
                double dW = eta[l - 1][o][0] * (
                        hebbCoef[l - 1][o][0][0] * values[l][o] * weights[l - 1][o][0] +   // A*a_i,j*a_j,k
                                hebbCoef[l - 1][o][0][1] * values[l][o] +                 // B*a_i,j
                                hebbCoef[l - 1][o][0][2] * weights[l - 1][o][0] +                 // C*a_j,k
                                hebbCoef[l - 1][o][0][3]                                  // D
                );
                weights[l - 1][o][0] = weights[l - 1][o][0] + dW;

                for (int i = 1; i < neurons[l - 1]+1; i++) {
                    //System.out.println(l+"  "+o+"  "+i);
                    dW = eta[l - 1][o][i] * (
                            hebbCoef[l - 1][o][i][0] * values[l][o] * values[l - 1][i-1] +   // A*a_i,j*a_j,k
                                    hebbCoef[l - 1][o][i][1] * values[l][o] +                 // B*a_i,j
                                    hebbCoef[l - 1][o][i][2] * values[l - 1][i-1] +                 // C*a_j,k
                                    hebbCoef[l - 1][o][i][3]                                  // D
                    );
                    weights[l - 1][o][i] = weights[l - 1][o][i] + dW;

                }
            }
        }

    }


    public void hebbianNormalization() {

        for (int l = 1; l < neurons.length; l++) {
            for (int o = 0; o < neurons[l]; o++) {
                double[] norm = norm(weights[l - 1][o]);
                if (neurons[l - 1] >= 0) System.arraycopy(norm, 0, weights[l - 1][o], 0, neurons[l - 1]);

            }
        }


    }

    @Override
    public double[] getParams() {
        return flatHebbCoef(hebbCoef, neurons);
    }

    @Override
    public void setParams(double[] params) {
        double[][][][] tmp = unflatHebbCoef(params, neurons);
        for (int i = 1; i < neurons.length; i++) {
            for (int j = 0; j < neurons[i]; j++) {
                for (int k = 0; k < neurons[i - 1] + 1; k++) {
                    for (int o = 0; o < 4; o++) {
                        hebbCoef[i-1][j][k][o] = tmp[i-1][j][k][o];
                    }
                }
            }
        }
    }

    /*
     *
     *  Etas
     *
     * */

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
        double[][][] unflatEtas= new double[neurons.length - 1][][];
        int c = 0;
        for (int i = 1; i < neurons.length; i++) {
            unflatEtas[i - 1] = new double[neurons[i]][neurons[i - 1] + 1];
            for (int j = 0; j < neurons[i]; j++) {
                for (int k = 0; k < neurons[i - 1] + 1; k++) {
                    unflatEtas[i-1][j][k] = initEta;
                    c = c + 1;
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

            if (this.toNormalize) {
                hebbianNormalization();
            }
        }

        return activationValues[neurons.length - 1];
    }

    /*
     *
     *  auxiliary
     *
     * */
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
        final HebbianPerceptron other = (HebbianPerceptron) obj;
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


}

