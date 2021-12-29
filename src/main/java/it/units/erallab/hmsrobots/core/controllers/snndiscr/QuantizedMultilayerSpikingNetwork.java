package it.units.erallab.hmsrobots.core.controllers.snndiscr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.erallab.hmsrobots.core.controllers.StatefulNN;
import it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.stv.QuantizedMovingAverageSpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.stv.QuantizedSpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.snapshots.SNNState;
import it.units.erallab.hmsrobots.util.Parametrized;
import it.units.erallab.hmsrobots.util.SerializationUtils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class QuantizedMultilayerSpikingNetwork implements QuantizedMultivariateSpikingFunction, Parametrized, StatefulNN {

  @JsonProperty
  protected final QuantizedSpikingFunction[][] neurons;    // layer + position in the layer
  @JsonProperty
  protected final double[][][] weights;           // layer + start neuron + end neuron
  protected double previousApplicationTime = 0d;
  protected double timeWindowSize;

  protected boolean spikesTracker = false;
  protected final List<Double>[][] spikes;
  protected final int[][][] currentSpikes;

  protected boolean weightsTracker = false;
  protected final Map<Double, double[]> weightsInTime;

  @JsonProperty
  private final QuantizedSpikeTrainToValueConverter[][] snapshotConverters;

  @SuppressWarnings("unchecked")
  @JsonCreator
  public QuantizedMultilayerSpikingNetwork(
      @JsonProperty("neurons") QuantizedSpikingFunction[][] neurons,
      @JsonProperty("weights") double[][][] weights,
      @JsonProperty("snapshotConverters") QuantizedSpikeTrainToValueConverter[][] snapshotConverters) {
    this.neurons = neurons;
    this.weights = weights;
    this.snapshotConverters = snapshotConverters;
    if (flat(weights, neurons).length != countWeights(neurons)) {
      throw new IllegalArgumentException(String.format(
          "Wrong number of weights: %d expected, %d found",
          countWeights(neurons),
          flat(weights, neurons).length
      ));
    }
    spikes = new List[neurons.length][];
    currentSpikes = new int[neurons.length][][];
    for (int i = 0; i < neurons.length; i++) {
      currentSpikes[i] = new int[neurons[i].length][];
      spikes[i] = new List[neurons[i].length];
      for (int j = 0; j < spikes[i].length; j++) {
        spikes[i][j] = new ArrayList<>();
      }
    }
    weightsInTime = new HashMap<>();
    weightsInTime.put(previousApplicationTime, flat(weights, neurons));
    innerReset();
  }

  public QuantizedMultilayerSpikingNetwork(QuantizedSpikingFunction[][] neurons, double[][][] weights) {
    this(neurons, weights, createSnapshotConverters(neurons, new QuantizedMovingAverageSpikeTrainToValueConverter()));
  }

  public QuantizedMultilayerSpikingNetwork(QuantizedSpikingFunction[][] neurons, double[] weights, QuantizedSpikeTrainToValueConverter converter) {
    this(neurons, unflat(weights, neurons), createSnapshotConverters(neurons, converter));
  }

  public QuantizedMultilayerSpikingNetwork(int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, BiFunction<Integer, Integer, QuantizedSpikingFunction> neuronBuilder, QuantizedSpikeTrainToValueConverter converter) {
    this(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), neuronBuilder), weights, converter);
  }

  public QuantizedMultilayerSpikingNetwork(int nOfInput, int[] innerNeurons, int nOfOutput, BiFunction<Integer, Integer, QuantizedSpikingFunction> neuronBuilder, QuantizedSpikeTrainToValueConverter converter) {
    this(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), neuronBuilder), new double[countWeights(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), neuronBuilder))], converter);
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

  protected static QuantizedSpikeTrainToValueConverter[][] createSnapshotConverters(QuantizedSpikingFunction[][] neurons, QuantizedSpikeTrainToValueConverter converter) {
    QuantizedSpikeTrainToValueConverter[][] converters = new QuantizedSpikeTrainToValueConverter[neurons.length][];
    IntStream.range(0, converters.length).forEach(layer -> {
      converters[layer] = new QuantizedSpikeTrainToValueConverter[neurons[layer].length];
      IntStream.range(0, neurons[layer].length).forEach(neuron -> {
            converters[layer][neuron] = SerializationUtils.clone(converter);
            converters[layer][neuron].reset();
          }
      );
    });
    return converters;
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
    timeWindowSize = t - previousApplicationTime;
    if (inputs.length != neurons[0].length) {
      throw new IllegalArgumentException(String.format("Expected input length is %d: found %d", neurons[0].length, inputs.length));
    }
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
      for (int neuronIndex = 0; neuronIndex < layer.length; neuronIndex++) {
        double[] weightedInputSpikeTrain = createWeightedSpikeTrain(layerIndex == 0 ? inputs : currentSpikes[layerIndex - 1], incomingWeights[neuronIndex]);
        layer[neuronIndex].setSumOfIncomingWeights(Arrays.stream(incomingWeights[neuronIndex]).sum());  // for homeostasis
        currentSpikes[layerIndex][neuronIndex] = layer[neuronIndex].compute(weightedInputSpikeTrain, t);
        if (spikesTracker) {
          int arrayLength = currentSpikes[layerIndex][neuronIndex].length;
          int finalLayerIndex = layerIndex;
          int finalNeuronIndex = neuronIndex;
          Arrays.stream(currentSpikes[layerIndex][neuronIndex]).forEach(x -> spikes[finalLayerIndex][finalNeuronIndex].add(x / arrayLength * timeWindowSize + previousApplicationTime));
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
    }
    previousApplicationTime = t;
    return currentSpikes[currentSpikes.length - 1];
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

  public Map<Double, double[]> getWeightsInTime() {
    return weightsInTime;
  }

  @Override
  public SNNState getState() {
    return new SNNState(getCurrentSpikes(), getWeights(), snapshotConverters, timeWindowSize);
  }

  public int[][][] getCurrentSpikes() {
    return currentSpikes;
  }

  @Override
  public void reset() {
    innerReset();
  }

  private void innerReset() {
    previousApplicationTime = 0d;
    for (int i = 0; i < neurons.length; i++) {
      for (int j = 0; j < neurons[i].length; j++) {
        neurons[i][j].reset();
        spikes[i][j].clear();
        snapshotConverters[i][j].reset();
      }
    }
  }
}
