package it.units.erallab.hmsrobots.core.controllers.snndiscr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.erallab.hmsrobots.core.controllers.snn.learning.STDPLearningRule;
import it.units.erallab.hmsrobots.core.controllers.snn.learning.SymmetricAntiHebbianLearningRule;
import it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.vts.QuantizedValueToSpikeTrainConverter;

import java.util.Arrays;
import java.util.function.BiFunction;

public class QuantizedLearningMultilayerSpikingNetwork extends QuantizedMultilayerSpikingNetwork {

  private static final int ARRAY_SIZE = QuantizedValueToSpikeTrainConverter.ARRAY_SIZE;
  private static final int STDP_LEARNING_WINDOW = (int) (2.5 * ARRAY_SIZE);

  @JsonProperty
  private final STDPLearningRule[][][] learningRules;           // layer + start neuron + end neuron

  private final int[][][] previousTimeOutputSpikes; // absolute time

  @JsonCreator
  public QuantizedLearningMultilayerSpikingNetwork(
      @JsonProperty("neurons") QuantizedSpikingFunction[][] neurons,
      @JsonProperty("weights") double[][][] weights,
      @JsonProperty("learningRules") STDPLearningRule[][][] learningRules) {
    super(neurons, weights);
    this.learningRules = learningRules;
    previousTimeOutputSpikes = new int[neurons.length][][];
    for (int i = 0; i < neurons.length; i++) {
      previousTimeOutputSpikes[i] = new int[neurons[i].length][];
      for (int j = 0; j < neurons[i].length; j++) {
        previousTimeOutputSpikes[i][j] = new int[STDP_LEARNING_WINDOW - ARRAY_SIZE];
      }
    }
  }

  public QuantizedLearningMultilayerSpikingNetwork(QuantizedSpikingFunction[][] neurons, double[][][] weights) {
    this(neurons, weights, initializeLearningRules(weights));
  }

  public QuantizedLearningMultilayerSpikingNetwork(QuantizedSpikingFunction[][] neurons, double[] weights) {
    this(neurons, unflat(weights, neurons));
  }

  public QuantizedLearningMultilayerSpikingNetwork(int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, STDPLearningRule[] learningRules, BiFunction<Integer, Integer, QuantizedSpikingFunction> neuronBuilder) {
    this(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), neuronBuilder), weights, learningRules);
  }

  public QuantizedLearningMultilayerSpikingNetwork(int nOfInput, int[] innerNeurons, int nOfOutput, BiFunction<Integer, Integer, QuantizedSpikingFunction> neuronBuilder) {
    this(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), neuronBuilder), new double[countWeights(createNeurons(MultiLayerPerceptron.countNeurons(nOfInput, innerNeurons, nOfOutput), neuronBuilder))]);
  }

  public QuantizedLearningMultilayerSpikingNetwork(QuantizedSpikingFunction[][] neurons, double[] weights, STDPLearningRule[] learningRules) {
    this(neurons, unflat(weights, neurons), unflat(learningRules, neurons));
  }

  @Override
  public int[][] apply(double t, int[][] inputs) {
    double deltaT = t - previousApplicationTime;
    double deltaTF = deltaT / (double) ARRAY_SIZE;
    if (inputs.length != neurons[0].length) {
      throw new IllegalArgumentException(String.format("Expected input length is %d: found %d", neurons[0].length, inputs.length));
    }
    int[][][] outputSpikes = new int[neurons.length][][];
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
        double[] weightedInputSpikeTrain = createWeightedSpikeTrain(layerIndex > 0 ? outputSpikes[layerIndex - 1] : inputs, incomingWeights[neuronIndex]);
        layer[neuronIndex].setSumOfIncomingWeights(Arrays.stream(incomingWeights[neuronIndex]).sum());  // for homeostasis
        thisLayersOutputs[neuronIndex] = layer[neuronIndex].compute(weightedInputSpikeTrain, t);
        // learning (not on the inputs)
        if (layerIndex > 0) {
          for (int previousNeuronIndex = 0; previousNeuronIndex < neurons[layerIndex - 1].length; previousNeuronIndex++) {
            double deltaW = 0;
            for (int tOut : thisLayersOutputs[neuronIndex]) {
              if (tOut > 0) {
                for (int tIn : outputSpikes[layerIndex - 1][previousNeuronIndex]) {
                  if (tIn > 0) {
                    deltaW += learningRules[layerIndex - 1][previousNeuronIndex][neuronIndex].computeDeltaW((tOut - tIn) * deltaTF);
                  }
                }
                for (int tIn : previousTimeOutputSpikes[layerIndex - 1][previousNeuronIndex]) {
                  if (tIn > 0) {
                    deltaW += learningRules[layerIndex - 1][previousNeuronIndex][neuronIndex].computeDeltaW((tOut - tIn + ARRAY_SIZE) * deltaTF);
                  }
                }
              }
            }
            weights[layerIndex - 1][previousNeuronIndex][neuronIndex] += deltaW;
          }
        }
        if (spikesTracker) {
          int arrayLength = thisLayersOutputs[neuronIndex].length;
          int finalLayerIndex = layerIndex;
          int finalNeuronIndex = neuronIndex;
          Arrays.stream(thisLayersOutputs[neuronIndex]).forEach(x -> spikes[finalLayerIndex][finalNeuronIndex].add(x / arrayLength * deltaT + previousApplicationTime));
        }
      }
      outputSpikes[layerIndex] = thisLayersOutputs;
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
    // updating stored spikes
    for (int layerIndex = 0; layerIndex < outputSpikes.length; layerIndex++) {
      for (int neuronIndex = 0; neuronIndex < outputSpikes[layerIndex].length; neuronIndex++) {
        // shift the previous ones ARRAY_SIZE to the left and add the current ones
        System.arraycopy(previousTimeOutputSpikes[layerIndex][neuronIndex], ARRAY_SIZE, previousTimeOutputSpikes[layerIndex][neuronIndex], 0, STDP_LEARNING_WINDOW - 2 * ARRAY_SIZE);
        System.arraycopy(outputSpikes[layerIndex][neuronIndex], 0, previousTimeOutputSpikes[layerIndex][neuronIndex], STDP_LEARNING_WINDOW - 2 * ARRAY_SIZE, ARRAY_SIZE);
      }
    }
    weightsInTime.put(t,weights);
    return thisLayersOutputs;
  }

  // for each layer, for each neuron, list incoming weights in order
  public static STDPLearningRule[] flat(STDPLearningRule[][][] unflatRules, QuantizedSpikingFunction[][] neurons) {
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

  public static STDPLearningRule[][][] unflat(STDPLearningRule[] flatRules, QuantizedSpikingFunction[][] neurons) {
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

  private static STDPLearningRule[][][] initializeLearningRules(double[][][] weights) {
    STDPLearningRule[][][] learningRules = new STDPLearningRule[weights.length][][];
    for (int startingLayer = 0; startingLayer < weights.length; startingLayer++) {
      learningRules[startingLayer] = new STDPLearningRule[weights[startingLayer].length][];
      for (int startingNeuron = 0; startingNeuron < weights[startingLayer].length; startingNeuron++) {
        learningRules[startingLayer][startingNeuron] = new STDPLearningRule[weights[startingLayer][startingNeuron].length];
        for (int learningRule = 0; learningRule < weights[startingLayer][startingNeuron].length; learningRule++) {
          learningRules[startingLayer][startingNeuron][learningRule] = new SymmetricAntiHebbianLearningRule();
        }
      }
    }
    return learningRules;
  }

}
