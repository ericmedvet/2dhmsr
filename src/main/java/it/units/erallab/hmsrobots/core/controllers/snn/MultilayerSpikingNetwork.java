package it.units.erallab.hmsrobots.core.controllers.snn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.erallab.hmsrobots.core.controllers.Resettable;
import it.units.erallab.hmsrobots.core.controllers.TimedRealFunction;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.stv.MovingAverageSpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.stv.SpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.vts.UniformWithMemoryValueToSpikeTrainConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.vts.ValueToSpikeTrainConverter;
import it.units.erallab.hmsrobots.util.Parametrized;
import it.units.erallab.hmsrobots.util.SerializationUtils;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

public class MultilayerSpikingNetwork implements MultivariateSpikingFunction, TimedRealFunction, Parametrized, Serializable, Resettable {

  @JsonProperty
  private final SpikingFunction[][] neurons;    // layer + position in the layer
  @JsonProperty
  private final double[][][] weights;           // layer + start neuron + end neuron
  private double previousApplicationTime;
  @JsonProperty
  private final ValueToSpikeTrainConverter[] valueToSpikeTrainConverters;
  @JsonProperty
  private final SpikeTrainToValueConverter[] spikeTrainToValueConverters;

  @JsonCreator
  public MultilayerSpikingNetwork(
          @JsonProperty("neurons") SpikingFunction[][] neurons,
          @JsonProperty("weights") double[][][] weights,
          @JsonProperty("valueToSpikeTrainConverters") ValueToSpikeTrainConverter[] valueToSpikeTrainConverter,
          @JsonProperty("spikeTrainToValueConverters") SpikeTrainToValueConverter[] spikeTrainToValueConverter
  ) {
    this.neurons = neurons;
    this.weights = weights;
    this.valueToSpikeTrainConverters = valueToSpikeTrainConverter;
    this.spikeTrainToValueConverters = spikeTrainToValueConverter;
    if (flat(weights, neurons).length != countWeights(neurons)) {
      throw new IllegalArgumentException(String.format(
              "Wrong number of weights: %d expected, %d found",
              countWeights(neurons),
              flat(weights, neurons).length
      ));
    }
    reset();
  }

  public MultilayerSpikingNetwork(SpikingFunction[][] neurons, double[][][] weights) {
    this(neurons, weights,
            createInputConverters(neurons[0].length, new UniformWithMemoryValueToSpikeTrainConverter()),
            createOutputConverters(neurons[neurons.length-1].length,new MovingAverageSpikeTrainToValueConverter()));
  }

  public MultilayerSpikingNetwork(int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, SpikingFunction spikingFunction) {
    this(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), spikingFunction), weights);
  }

  public MultilayerSpikingNetwork(int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, SpikingFunction spikingFunction, ValueToSpikeTrainConverter valueToSpikeTrainConverter, SpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), spikingFunction), weights, valueToSpikeTrainConverter,spikeTrainToValueConverter);
  }

  public MultilayerSpikingNetwork(int nOfInput, int[] innerNeurons, int nOfOutput, SpikingFunction spikingFunction) {
    this(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), spikingFunction), new double[countWeights(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), spikingFunction))]);
  }

  public MultilayerSpikingNetwork(int nOfInput, int[] innerNeurons, int nOfOutput, SpikingFunction spikingFunction, ValueToSpikeTrainConverter valueToSpikeTrainConverter, SpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), spikingFunction), new double[countWeights(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), spikingFunction))], valueToSpikeTrainConverter, spikeTrainToValueConverter);
  }

  public MultilayerSpikingNetwork(int nOfInput, int[] innerNeurons, int nOfOutput, BiFunction<Integer, Integer, SpikingFunction> neuronBuilder) {
    this(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), neuronBuilder), new double[countWeights(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), neuronBuilder))]);
  }

  public MultilayerSpikingNetwork(int nOfInput, int[] innerNeurons, int nOfOutput, BiFunction<Integer, Integer, SpikingFunction> neuronBuilder, ValueToSpikeTrainConverter valueToSpikeTrainConverter, SpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), neuronBuilder), new double[countWeights(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), neuronBuilder))], valueToSpikeTrainConverter, spikeTrainToValueConverter);
  }

  public MultilayerSpikingNetwork(SpikingFunction[][] neurons, double[] weights) {
    this(neurons, unflat(weights, neurons));
  }

  public MultilayerSpikingNetwork(SpikingFunction[][] neurons, double[] weights, ValueToSpikeTrainConverter valueToSpikeTrainConverter, SpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(neurons, unflat(weights, neurons), createInputConverters(neurons[0].length,valueToSpikeTrainConverter),
            createOutputConverters(neurons[neurons.length-1].length,spikeTrainToValueConverter));
  }

  private static SpikingFunction[][] createNeurons(int[] neuronsPerLayer, SpikingFunction spikingFunction) {
    SpikingFunction[][] spikingFunctions = new SpikingFunction[neuronsPerLayer.length][];
    for (int i = 0; i < neuronsPerLayer.length; i++) {
      spikingFunctions[i] = new SpikingFunction[neuronsPerLayer[i]];
      for (int j = 0; j < spikingFunctions[i].length; j++) {
        spikingFunctions[i][j] = SerializationUtils.clone(spikingFunction, SerializationUtils.Mode.JAVA);
        spikingFunctions[i][j].reset();
      }
    }
    return spikingFunctions;
  }

  private static SpikingFunction[][] createNeurons(int[] neuronsPerLayer, BiFunction<Integer, Integer, SpikingFunction> neuronBuilder) {
    SpikingFunction[][] spikingFunctions = new SpikingFunction[neuronsPerLayer.length][];
    for (int i = 0; i < neuronsPerLayer.length; i++) {
      spikingFunctions[i] = new SpikingFunction[neuronsPerLayer[i]];
      for (int j = 0; j < spikingFunctions[i].length; j++) {
        spikingFunctions[i][j] = neuronBuilder.apply(i, j);
      }
    }
    return spikingFunctions;
  }

  @Override
  public SortedSet<Double>[] apply(double t, SortedSet<Double>[] inputs) {
    if (inputs.length != neurons[0].length) {
      throw new IllegalArgumentException(String.format("Expected input length is %d: found %d", neurons[0].length, inputs.length));
    }
    SortedSet<Double>[] previousLayerOutputs = inputs;
    SortedSet<Double>[] thisLayersOutputs = null;
    // destination neuron, array of incoming weights
    double[][] incomingWeights = new double[inputs.length][inputs.length];
    for (int i = 0; i < incomingWeights.length; i++) {
      incomingWeights[i][i] = 1;
    }
    // iterating over layers
    for (int layerIndex = 0; layerIndex < neurons.length; layerIndex++) {
      SpikingFunction[] layer = neurons[layerIndex];
      thisLayersOutputs = new SortedSet[layer.length];
      for (int neuronIndex = 0; neuronIndex < layer.length; neuronIndex++) {
        SortedMap<Double, Double> weightedInputSpikeTrain = createWeightedSpikeTrain(previousLayerOutputs, incomingWeights[neuronIndex]);
        layer[neuronIndex].setSumOfIncomingWeights(Arrays.stream(incomingWeights[neuronIndex]).sum());
        thisLayersOutputs[neuronIndex] = layer[neuronIndex].compute(weightedInputSpikeTrain, t);
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
      previousLayerOutputs = thisLayersOutputs;
    }
    return thisLayersOutputs;
  }

  @Override
  public double[] apply(double t, double[] input) {
    double deltaT = t - previousApplicationTime;
    SortedSet<Double>[] inputSpikes = new SortedSet[input.length];
    IntStream.range(0, input.length).forEach(i ->
            inputSpikes[i] = valueToSpikeTrainConverters[i].convert(input[i], deltaT, t));
    SortedSet<Double>[] outputSpikes = apply(t, inputSpikes);
    previousApplicationTime = t;
    double[] output = new double[outputSpikes.length];
    IntStream.range(0,outputSpikes.length).forEach(i->
            output[i] = spikeTrainToValueConverters[i].convert(outputSpikes[i],deltaT));
    return output;
  }

  private SortedMap<Double, Double> createWeightedSpikeTrain(SortedSet<Double>[] inputs, double[] weights) {
    SortedMap<Double, Double> weightedSpikeTrain = new TreeMap<>();
    for (int i = 0; i < inputs.length; i++) {
      double weight = weights[i];
      if (weight == 0)
        continue;
      inputs[i].forEach(spikeTime -> {
        if (weightedSpikeTrain.containsKey(spikeTime)) {
          weightedSpikeTrain.put(spikeTime, weightedSpikeTrain.get(spikeTime) + weight);
        } else {
          weightedSpikeTrain.put(spikeTime, weight);
        }
      });
    }
    return weightedSpikeTrain;
  }

  @Override
  public int getInputDimension() {
    return neurons[0].length;
  }

  @Override
  public int getOutputDimension() {
    return neurons[neurons.length - 1].length;
  }

  public SpikingFunction[][] getNeurons() {
    return neurons;
  }

  public double[][][] getWeights() {
    return weights;
  }

  public static int countWeights(SpikingFunction[][] neurons) {
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
  public static double[] flat(double[][][] unflatWeights, SpikingFunction[][] neurons) {
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

  private static ValueToSpikeTrainConverter[] createInputConverters(int nOfInputs, ValueToSpikeTrainConverter valueToSpikeTrainConverter){
    ValueToSpikeTrainConverter[] valueToSpikeTrainConverters = new ValueToSpikeTrainConverter[nOfInputs];
    IntStream.range(0,nOfInputs).forEach(i->{
      valueToSpikeTrainConverters[i] = SerializationUtils.clone(valueToSpikeTrainConverter);
      valueToSpikeTrainConverters[i].reset();
    });
    return valueToSpikeTrainConverters;
  }

  private static SpikeTrainToValueConverter[] createOutputConverters(int nOfOutputs, SpikeTrainToValueConverter spikeTrainToValueConverter){
    SpikeTrainToValueConverter[] spikeTrainToValueConverters = new SpikeTrainToValueConverter[nOfOutputs];
    IntStream.range(0,nOfOutputs).forEach(i->{
      spikeTrainToValueConverters[i] = SerializationUtils.clone(spikeTrainToValueConverter);
      spikeTrainToValueConverters[i].reset();
    });
    return spikeTrainToValueConverters;
  }

  public static double[][][] unflat(double[] flatWeights, SpikingFunction[][] neurons) {
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
  public void reset() {
    previousApplicationTime = 0d;
    IntStream.range(0,spikeTrainToValueConverters.length).forEach(i->
            spikeTrainToValueConverters[i].reset());
    IntStream.range(0,valueToSpikeTrainConverters.length).forEach(i->
            valueToSpikeTrainConverters[i].reset());
    for (SpikingFunction[] layer : neurons) {
      for (SpikingFunction neuron : layer) {
        neuron.reset();
      }
    }
  }

}
