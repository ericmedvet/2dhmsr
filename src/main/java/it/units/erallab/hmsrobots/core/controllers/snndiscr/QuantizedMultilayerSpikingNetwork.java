package it.units.erallab.hmsrobots.core.controllers.snndiscr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.erallab.hmsrobots.util.Parametrized;
import it.units.erallab.hmsrobots.util.SerializationUtils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, property="@class")
public class QuantizedMultilayerSpikingNetwork implements QuantizedMultivariateSpikingFunction, Parametrized {

  @JsonProperty
  protected final QuantizedSpikingFunction[][] neurons;    // layer + position in the layer
  @JsonProperty
  protected final double[][][] weights;           // layer + start neuron + end neuron
  protected double previousApplicationTime = 0d;

  protected boolean spikesTracker = false;
  protected final List<Double>[][] spikes;

  protected boolean weightsTracker = false;
  protected final Map<Double,double[][][]> weightsInTime;

  @SuppressWarnings("unchecked")
  @JsonCreator
  public QuantizedMultilayerSpikingNetwork(
      @JsonProperty("neurons") QuantizedSpikingFunction[][] neurons,
      @JsonProperty("weights") double[][][] weights) {
    this.neurons = neurons;
    this.weights = weights;
    if (flat(weights, neurons).length != countWeights(neurons)) {
      throw new IllegalArgumentException(String.format(
          "Wrong number of weights: %d expected, %d found",
          countWeights(neurons),
          flat(weights, neurons).length
      ));
    }
    spikes = new List[neurons.length][];
    for (int i = 0; i < neurons.length; i++) {
      spikes[i] = new List[neurons[i].length];
      for (int j = 0; j < spikes[i].length; j++) {
        spikes[i][j] = new ArrayList<>();
      }
    }
    weightsInTime = new HashMap<>();
    weightsInTime.put(previousApplicationTime,weights);
    reset();
  }

  public QuantizedMultilayerSpikingNetwork(QuantizedSpikingFunction[][] neurons, double[] weights) {
    this(neurons, unflat(weights, neurons));
  }

  public QuantizedMultilayerSpikingNetwork(int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, BiFunction<Integer, Integer, QuantizedSpikingFunction> neuronBuilder) {
    this(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), neuronBuilder), weights);
  }

  public QuantizedMultilayerSpikingNetwork(int nOfInput, int[] innerNeurons, int nOfOutput, BiFunction<Integer, Integer, QuantizedSpikingFunction> neuronBuilder) {
    this(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), neuronBuilder), new double[countWeights(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), neuronBuilder))]);
  }

  protected static QuantizedSpikingFunction[][] createNeurons(int[] neuronsPerLayer, QuantizedSpikingFunction quantizedSpikingFunction) {
    QuantizedSpikingFunction[][] quantizedSpikingFunctions = new QuantizedSpikingFunction[neuronsPerLayer.length][];
    for (int i = 0; i < neuronsPerLayer.length; i++) {
      quantizedSpikingFunctions[i] = new QuantizedSpikingFunction[neuronsPerLayer[i]];
      for (int j = 0; j < quantizedSpikingFunctions[i].length; j++) {
        quantizedSpikingFunctions[i][j] = SerializationUtils.clone(quantizedSpikingFunction, SerializationUtils.Mode.JAVA);
        quantizedSpikingFunctions[i][j].reset();
      }
    }
    return quantizedSpikingFunctions;
  }

  protected static QuantizedSpikingFunction[][] createNeurons(int[] neuronsPerLayer, BiFunction<Integer, Integer, QuantizedSpikingFunction> neuronBuilder) {
    QuantizedSpikingFunction[][] quantizedSpikingFunctions = new QuantizedSpikingFunction[neuronsPerLayer.length][];
    for (int i = 0; i < neuronsPerLayer.length; i++) {
      quantizedSpikingFunctions[i] = new QuantizedSpikingFunction[neuronsPerLayer[i]];
      for (int j = 0; j < quantizedSpikingFunctions[i].length; j++) {
        quantizedSpikingFunctions[i][j] = neuronBuilder.apply(i, j);
      }
    }
    return quantizedSpikingFunctions;
  }

  @Override
  public int[][] apply(double t, int[][] inputs) {
    double deltaT = t - previousApplicationTime;
    if (inputs.length != neurons[0].length) {
      throw new IllegalArgumentException(String.format("Expected input length is %d: found %d", neurons[0].length, inputs.length));
    }
    int[][] previousLayersOutputs = inputs;
    int[][] thisLayersOutputs = null;
    // destination neuron, array of incoming weights
    double[][] incomingWeights = new double[inputs.length][inputs.length];
    for (int i = 0; i < incomingWeights.length; i++) {
      incomingWeights[i][i] = 1;
      if (neurons[0][i] instanceof QuantizedIzhikevicNeuron) {
        incomingWeights[i][i] = 100;
      }
    }
    // iterating over layers
    for (int layerIndex = 0; layerIndex < neurons.length; layerIndex++) {
      QuantizedSpikingFunction[] layer = neurons[layerIndex];
      thisLayersOutputs = new int[layer.length][];
      for (int neuronIndex = 0; neuronIndex < layer.length; neuronIndex++) {
        double[] weightedInputSpikeTrain = createWeightedSpikeTrain(previousLayersOutputs, incomingWeights[neuronIndex]);
        layer[neuronIndex].setSumOfIncomingWeights(Arrays.stream(incomingWeights[neuronIndex]).sum());  // for homeostasis
        thisLayersOutputs[neuronIndex] = layer[neuronIndex].compute(weightedInputSpikeTrain, t);
        if (spikesTracker) {
          int arrayLength = thisLayersOutputs[neuronIndex].length;
          int finalLayerIndex = layerIndex;
          int finalNeuronIndex = neuronIndex;
          Arrays.stream(thisLayersOutputs[neuronIndex]).forEach(x -> spikes[finalLayerIndex][finalNeuronIndex].add(x / arrayLength * deltaT + previousApplicationTime));
        }
      }
      if (layerIndex == neurons.length - 1) {
        break;
      }
      incomingWeights = new double[neurons[layerIndex + 1].length][neurons[layerIndex].length];
      for (int i = 0; i < incomingWeights.length; i++) {
        for (int j = 0; j < incomingWeights[0].length; j++) {
          incomingWeights[i][j] = weights[layerIndex][j][i];
        }
      }
      previousLayersOutputs = thisLayersOutputs;
    }
    previousApplicationTime = t;
    return thisLayersOutputs;
  }

  protected double[] createWeightedSpikeTrain(int[][] inputs, double[] weights) {
    double[] weightedSpikeTrain = new double[inputs[0].length];
    for (int time = 0; time < weightedSpikeTrain.length; time++) {
      for (int neuron = 0; neuron < inputs.length; neuron++) {
        weightedSpikeTrain[time] += weights[neuron] * inputs[neuron][time];
      }
    }
    return weightedSpikeTrain;
  }

  public static int countWeights(QuantizedSpikingFunction[][] neurons) {
    int c = 0;
    for (int i = 1; i < neurons.length; i++) {
      c += neurons[i].length * neurons[i - 1].length;
    }
    return c;
  }

  public static int countWeights(int[] neurons) {
    int c = 0;
    for (int i = 1; i < neurons.length; i++) {
      c += neurons[i] * neurons[i - 1];
    }
    return c;
  }

  public static int countWeights(int nOfInput, int[] innerNeurons, int nOfOutput) {
    return countWeights(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput));
  }

  // for each layer, for each neuron, list incoming weights in order
  public static double[] flat(double[][][] unflatWeights, QuantizedSpikingFunction[][] neurons) {
    double[] flatWeights = new double[countWeights(neurons)];
    int c = 0;
    for (int i = 1; i < neurons.length; i++) {
      for (int j = 0; j < neurons[i].length; j++) {
        for (int k = 0; k < neurons[i - 1].length; k++) {
          flatWeights[c] = unflatWeights[i - 1][k][j];
          c++;
        }
      }
    }
    return flatWeights;
  }

  public static double[][][] unflat(double[] flatWeights, QuantizedSpikingFunction[][] neurons) {
    double[][][] unflatWeights = new double[neurons.length - 1][][];
    int c = 0;
    for (int i = 1; i < neurons.length; i++) {
      unflatWeights[i - 1] = new double[neurons[i - 1].length][neurons[i].length];
      for (int j = 0; j < neurons[i].length; j++) {
        for (int k = 0; k < neurons[i - 1].length; k++) {
          unflatWeights[i - 1][k][j] = flatWeights[c];
          c++;
        }
      }
    }
    return unflatWeights;
  }

  public QuantizedSpikingFunction[][] getNeurons() {
    return neurons;
  }

  public double[][][] getWeights() {
    return weights;
  }

  @Override
  public double[] getParams() {
    return flat(weights, neurons);
  }

  @Override
  public void setParams(double[] params) {
    double[][][] newWeights = unflat(params, neurons);
    for (int l = 0; l < newWeights.length; l++) {
      for (int s = 0; s < newWeights[l].length; s++) {
        System.arraycopy(newWeights[l][s], 0, weights[l][s], 0, newWeights[l][s].length);
      }
    }
    reset();
  }

  @Override
  public int getInputDimension() {
    return neurons[0].length;
  }

  @Override
  public int getOutputDimension() {
    return neurons[neurons.length - 1].length;
  }

  public void setPlotMode(boolean plotMode) {
    Stream.of(neurons)
        .flatMap(Stream::of)
        .forEach(x -> x.setPlotMode(true));
  }

  public void setSpikesTracker(boolean spikesTracker) {
    this.spikesTracker = spikesTracker;
  }

  public void setWeightsTracker(boolean weightsTracker) {
    this.weightsTracker = weightsTracker;
  }

  public List<Double>[][] getSpikes() {
    return spikes;
  }

  public Map<Double, double[][][]> getWeightsInTime() {
    return weightsInTime;
  }

  @Override
  public void reset() {
    previousApplicationTime = 0d;
    for (int i = 0; i < neurons.length; i++) {
      for (int j = 0; j < neurons[i].length; j++) {
        neurons[i][j].reset();
        spikes[i][j].clear();
      }
    }
  }
}
