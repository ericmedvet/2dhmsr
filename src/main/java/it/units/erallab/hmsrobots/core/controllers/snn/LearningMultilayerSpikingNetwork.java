package it.units.erallab.hmsrobots.core.controllers.snn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.erallab.hmsrobots.core.controllers.snn.learning.STDPLearningRule;

import java.util.Arrays;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class LearningMultilayerSpikingNetwork extends MultilayerSpikingNetwork {

  protected static final double STDP_LEARNING_WINDOW = 0.04;

  @JsonProperty
  private final STDPLearningRule[][][] learningRules;           // layer + start neuron + end neuron

  private final SortedSet<Double>[][] previousTimeOutputSpikes; // absolute time

  @JsonCreator
  @SuppressWarnings("unchecked")
  public LearningMultilayerSpikingNetwork(
      @JsonProperty("neurons") SpikingFunction[][] neurons,
      @JsonProperty("weights") double[][][] weights,
      @JsonProperty("learningRules") STDPLearningRule[][][] learningRules) {
    super(neurons, weights);
    this.learningRules = learningRules;
    previousTimeOutputSpikes = new SortedSet[neurons.length][];
    for (int i = 0; i < neurons.length; i++) {
      previousTimeOutputSpikes[i] = new SortedSet[neurons[i].length];
      for (int j = 0; j < neurons[i].length; j++) {
        previousTimeOutputSpikes[i][j] = new TreeSet<>();
      }
    }
  }

  @SuppressWarnings("unchecked")
  public LearningMultilayerSpikingNetwork(SpikingFunction[][] neurons, double[][][] weights) {
    super(neurons, weights);
    learningRules = new STDPLearningRule[weights.length][][];
    for (int startingLayer = 0; startingLayer < weights.length; startingLayer++) {
      learningRules[startingLayer] = new STDPLearningRule[weights[startingLayer].length][];
      for (int startingNeuron = 0; startingNeuron < weights[startingLayer].length; startingNeuron++) {
        learningRules[startingLayer][startingNeuron] = new STDPLearningRule[weights[startingLayer][startingNeuron].length];
      }
    }
    previousTimeOutputSpikes = new SortedSet[neurons.length][];
    for (int layer = 0; layer < neurons.length; layer++) {
      previousTimeOutputSpikes[layer] = new SortedSet[neurons[layer].length];
      for (int neuron = 0; neuron < neurons[layer].length; neuron++) {
        previousTimeOutputSpikes[layer][neuron] = new TreeSet<>();
      }
    }
  }

  public LearningMultilayerSpikingNetwork(int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, SpikingFunction spikingFunction) {
    this(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), spikingFunction), weights);
  }

  public LearningMultilayerSpikingNetwork(int nOfInput, int[] innerNeurons, int nOfOutput, SpikingFunction spikingFunction) {
    this(nOfInput, innerNeurons, nOfOutput, new double[countWeights(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), spikingFunction))], spikingFunction);
  }

  public LearningMultilayerSpikingNetwork(int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, BiFunction<Integer, Integer, SpikingFunction> neuronBuilder, STDPLearningRule[] learningRules) {
    this(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), neuronBuilder), weights, learningRules);
  }

  public LearningMultilayerSpikingNetwork(int nOfInput, int[] innerNeurons, int nOfOutput, BiFunction<Integer, Integer, SpikingFunction> neuronBuilder) {
    this(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), neuronBuilder), new double[countWeights(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), neuronBuilder))]);
  }

  public LearningMultilayerSpikingNetwork(SpikingFunction[][] neurons, double[] weights, STDPLearningRule[] learningRules) {
    this(neurons, unflat(weights, neurons), unflat(learningRules,neurons));
  }

  public LearningMultilayerSpikingNetwork(SpikingFunction[][] neurons, double[] weights) {
    this(neurons, unflat(weights, neurons));
  }

  @SuppressWarnings("unchecked")
  @Override
  public SortedSet<Double>[] apply(double t, SortedSet<Double>[] inputs) {
    double deltaT = t - previousApplicationTime;
    if (inputs.length != neurons[0].length) {
      throw new IllegalArgumentException(String.format("Expected input length is %d: found %d", neurons[0].length, inputs.length));
    }
    SortedSet<Double>[] previousLayersOutputs = inputs;
    SortedSet<Double>[] thisLayersOutputs = null;
    // destination neuron, array of incoming weights
    double[][] incomingWeights = new double[inputs.length][inputs.length];
    for (int i = 0; i < incomingWeights.length; i++) {
      incomingWeights[i][i] = 1;
      if (neurons[0][i] instanceof IzhikevicNeuron) {
        incomingWeights[i][i] = 100;
      }
    }
    // create array to store output spikes in absolute time
    SortedSet<Double>[][] absoluteTimeOutputSpikes = new SortedSet[neurons.length][];
    for (int i = 0; i < neurons.length; i++) {
      absoluteTimeOutputSpikes[i] = new SortedSet[neurons[i].length];
    }
    // iterating over layers
    for (int layerIndex = 0; layerIndex < neurons.length; layerIndex++) {
      SpikingFunction[] layer = neurons[layerIndex];
      thisLayersOutputs = new SortedSet[layer.length];
      for (int neuronIndex = 0; neuronIndex < layer.length; neuronIndex++) {
        SortedMap<Double, Double> weightedInputSpikeTrain = createWeightedSpikeTrain(previousLayersOutputs, incomingWeights[neuronIndex]);
        layer[neuronIndex].setSumOfIncomingWeights(Arrays.stream(incomingWeights[neuronIndex]).sum());  // for homeostasis
        thisLayersOutputs[neuronIndex] = layer[neuronIndex].compute(weightedInputSpikeTrain, t);
        absoluteTimeOutputSpikes[layerIndex][neuronIndex] = thisLayersOutputs[neuronIndex].stream()
            .map(x -> previousApplicationTime + deltaT * x).collect(Collectors.toCollection(TreeSet::new));
        // learning (not on the inputs)
        if (layerIndex != 0) {
          // compute the time difference with the previous layers spikes at the previous simulation timestep
          for (int previousNeuronIndex = 0; previousNeuronIndex < previousLayersOutputs.length; previousNeuronIndex++) {
            double deltaW = 0;
            SortedSet<Double> previousOutputs = new TreeSet<>(previousTimeOutputSpikes[layerIndex - 1][previousNeuronIndex]);
            previousOutputs.addAll(absoluteTimeOutputSpikes[layerIndex-1][previousNeuronIndex]);
            for (double tOut : absoluteTimeOutputSpikes[layerIndex][neuronIndex]) {
              for (double tIn : previousOutputs) {
                if (Math.abs(tOut - tIn) <= STDP_LEARNING_WINDOW) {
                  System.out.printf("Layer: %d, previous neuron: %d, current neuron: %d\n", layerIndex, previousNeuronIndex, neuronIndex);
                  deltaW += learningRules[layerIndex-1][previousNeuronIndex][neuronIndex].computeDeltaW(tOut - tIn);
                }
              }
            }
            weights[layerIndex-1][previousNeuronIndex][neuronIndex] += deltaW;
          }
        }
        if (spikesTracker) {
          spikes[layerIndex][neuronIndex].addAll(
              thisLayersOutputs[neuronIndex].stream().map(x -> x * deltaT + previousApplicationTime).collect(Collectors.toList())
          );
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
    for (int layer = 0; layer < absoluteTimeOutputSpikes.length; layer++) {
      System.arraycopy(absoluteTimeOutputSpikes[layer], 0, previousTimeOutputSpikes[layer], 0, absoluteTimeOutputSpikes[layer].length);
    }
    return thisLayersOutputs;
  }

  // for each layer, for each neuron, list incoming weights in order
  public static STDPLearningRule[] flat(STDPLearningRule[][][] unflatRules, SpikingFunction[][] neurons) {
    STDPLearningRule[] flatRules = new STDPLearningRule[countWeights(neurons)];
    int c = 0;
    for (int i = 1; i < neurons.length; i++) {
      for (int j = 0; j < neurons[i].length; j++) {
        for (int k = 0; k < neurons[i - 1].length; k++) {
          flatRules[c] = unflatRules[i - 1][k][j];
          c++;
        }
      }
    }
    return flatRules;
  }

  public static STDPLearningRule[][][] unflat(STDPLearningRule[] flatRules, SpikingFunction[][] neurons) {
    STDPLearningRule[][][] unflatRules = new STDPLearningRule[neurons.length - 1][][];
    int c = 0;
    for (int i = 1; i < neurons.length; i++) {
      unflatRules[i - 1] = new STDPLearningRule[neurons[i - 1].length][neurons[i].length];
      for (int j = 0; j < neurons[i].length; j++) {
        for (int k = 0; k < neurons[i - 1].length; k++) {
          unflatRules[i - 1][k][j] = flatRules[c];
          c++;
        }
      }
    }
    return unflatRules;
  }

  public STDPLearningRule[][][] getLearningRules() {
    return learningRules;
  }

}
