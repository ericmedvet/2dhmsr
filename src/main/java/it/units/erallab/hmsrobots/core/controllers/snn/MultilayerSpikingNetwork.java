package it.units.erallab.hmsrobots.core.controllers.snn;

import it.units.erallab.hmsrobots.core.controllers.TimedRealFunction;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.AverageFrequencySpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.SpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.UniformValueToSpikeTrainConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.ValueToSpikeTrainConverter;

import java.util.Arrays;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

// we suppose it's fully connected, just assign 0 to the weight to trim the connection
public class MultilayerSpikingNetwork implements MultivariateSpikingFunction, TimedRealFunction {

  // layer + position in the layer
  private final SpikingFunction[][] neurons;
  // layer (from which it's exiting)
  // + position of the neuron from which is exiting
  // + position of the neuron to which it's going (beware 0 could be the bias)
  private final double[][][] weights;
  private double previousApplicationTime;
  private ValueToSpikeTrainConverter valueToSpikeTrainConverter;
  private SpikeTrainToValueConverter spikeTrainToValueConverter;

  public MultilayerSpikingNetwork(SpikingFunction[][] neurons, double[][][] weights) {
    this.neurons = neurons;
    this.weights = weights;
    valueToSpikeTrainConverter = new UniformValueToSpikeTrainConverter();
    spikeTrainToValueConverter = new AverageFrequencySpikeTrainToValueConverter();
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
    SortedSet<Double>[] inputSpikes = (SortedSet<Double>[]) Arrays.stream(input).mapToObj(d -> valueToSpikeTrainConverter.convert(d, deltaT)).toArray();
    SortedSet<Double>[] outputSpikes = apply(t, inputSpikes);
    return Arrays.stream(outputSpikes).mapToDouble(x -> spikeTrainToValueConverter.convert(x, deltaT)).toArray();
  }

  private SortedMap<Double, Double> createWeightedSpikeTrain(SortedSet<Double>[] inputs, double[] weights) {
    SortedMap<Double, Double> weightedSpikeTrain = new TreeMap<>();
    for (int i = 0; i < inputs.length; i++) {
      double weight = weights[i];
      if(weight == 0)
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
  public int getInputDimension(){
    return neurons[0].length;
  }

  @Override
  public int getOutputDimension(){
    return neurons[neurons.length-1].length;
  }

}
