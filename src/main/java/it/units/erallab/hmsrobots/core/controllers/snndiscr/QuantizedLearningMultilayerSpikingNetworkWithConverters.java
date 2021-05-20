package it.units.erallab.hmsrobots.core.controllers.snndiscr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.stv.QuantizedMovingAverageSpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.stv.QuantizedSpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.vts.QuantizedUniformWithMemoryValueToSpikeTrainConverter;
import it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.vts.QuantizedValueToSpikeTrainConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.learning.STDPLearningRule;

import java.util.function.BiFunction;

public class QuantizedLearningMultilayerSpikingNetworkWithConverters extends QuantizedMultilayerSpikingNetworkWithConverters {

  @JsonCreator
  public QuantizedLearningMultilayerSpikingNetworkWithConverters(
      @JsonProperty("learningMultilayerSpikingNetwork") QuantizedLearningMultilayerSpikingNetwork learningMultilayerSpikingNetwork,
      @JsonProperty("valueToSpikeTrainConverters") QuantizedValueToSpikeTrainConverter[] valueToSpikeTrainConverter,
      @JsonProperty("spikeTrainToValueConverters") QuantizedSpikeTrainToValueConverter[] spikeTrainToValueConverter
  ) {
    super(learningMultilayerSpikingNetwork, valueToSpikeTrainConverter, spikeTrainToValueConverter);
  }

  public QuantizedLearningMultilayerSpikingNetworkWithConverters(QuantizedLearningMultilayerSpikingNetwork learningMultilayerSpikingNetwork) {
    this(learningMultilayerSpikingNetwork,
        createInputConverters(learningMultilayerSpikingNetwork.getInputDimension(), new QuantizedUniformWithMemoryValueToSpikeTrainConverter()),
        createOutputConverters(learningMultilayerSpikingNetwork.getOutputDimension(), new QuantizedMovingAverageSpikeTrainToValueConverter()));
  }

  public QuantizedLearningMultilayerSpikingNetworkWithConverters(QuantizedSpikingFunction[][] neurons, double[][][] weights, STDPLearningRule[][][] learningRules, QuantizedValueToSpikeTrainConverter valueToSpikeTrainConverter, QuantizedSpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(new QuantizedLearningMultilayerSpikingNetwork(neurons, weights,learningRules),
        createInputConverters(neurons[0].length, valueToSpikeTrainConverter),
        createOutputConverters(neurons[neurons.length - 1].length, spikeTrainToValueConverter));
  }

  public QuantizedLearningMultilayerSpikingNetworkWithConverters(QuantizedSpikingFunction[][] neurons, double[][][] weights, STDPLearningRule[][][] learningRules, QuantizedValueToSpikeTrainConverter[] valueToSpikeTrainConverters, QuantizedSpikeTrainToValueConverter[] spikeTrainToValueConverters) {
    this(new QuantizedLearningMultilayerSpikingNetwork(neurons, weights, learningRules), valueToSpikeTrainConverters, spikeTrainToValueConverters);
  }

  public QuantizedLearningMultilayerSpikingNetworkWithConverters(QuantizedSpikingFunction[][] neurons, double[][][] weights, STDPLearningRule[][][] learningRules) {
    this(new QuantizedLearningMultilayerSpikingNetwork(neurons, weights,learningRules),
        createInputConverters(neurons[0].length, new QuantizedUniformWithMemoryValueToSpikeTrainConverter()),
        createOutputConverters(neurons[neurons.length - 1].length, new QuantizedMovingAverageSpikeTrainToValueConverter()));
  }

  public QuantizedLearningMultilayerSpikingNetworkWithConverters(int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, STDPLearningRule[] learningRules, QuantizedSpikingFunction spikingFunction) {
    this(new QuantizedLearningMultilayerSpikingNetwork(nOfInput, innerNeurons, nOfOutput, weights, (i,j)->spikingFunction, learningRules));
  }

  public QuantizedLearningMultilayerSpikingNetworkWithConverters(int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, STDPLearningRule[] learningRules, QuantizedSpikingFunction spikingFunction, QuantizedValueToSpikeTrainConverter valueToSpikeTrainConverter, QuantizedSpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(new QuantizedLearningMultilayerSpikingNetwork(nOfInput, innerNeurons, nOfOutput, weights, (i,j)->spikingFunction, learningRules), createInputConverters(nOfInput, valueToSpikeTrainConverter), createOutputConverters(nOfOutput, spikeTrainToValueConverter));
  }

  public QuantizedLearningMultilayerSpikingNetworkWithConverters(int nOfInput, int[] innerNeurons, int nOfOutput, QuantizedSpikingFunction spikingFunction, QuantizedValueToSpikeTrainConverter valueToSpikeTrainConverter, QuantizedSpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(new QuantizedLearningMultilayerSpikingNetwork(nOfInput, innerNeurons, nOfOutput, spikingFunction), createInputConverters(nOfInput, valueToSpikeTrainConverter), createOutputConverters(nOfOutput, spikeTrainToValueConverter));
  }

  public QuantizedLearningMultilayerSpikingNetworkWithConverters(int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, STDPLearningRule[] learningRules, BiFunction<Integer, Integer, QuantizedSpikingFunction> neuronBuilder, QuantizedValueToSpikeTrainConverter valueToSpikeTrainConverter, QuantizedSpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(new QuantizedLearningMultilayerSpikingNetwork(nOfInput, innerNeurons, nOfOutput, weights, neuronBuilder, learningRules), createInputConverters(nOfInput, valueToSpikeTrainConverter), createOutputConverters(nOfOutput, spikeTrainToValueConverter));
  }

  public QuantizedLearningMultilayerSpikingNetworkWithConverters(int nOfInput, int[] innerNeurons, int nOfOutput, BiFunction<Integer, Integer, QuantizedSpikingFunction> neuronBuilder) {
    this(new QuantizedLearningMultilayerSpikingNetwork(nOfInput, innerNeurons, nOfOutput, neuronBuilder));
  }

  public QuantizedLearningMultilayerSpikingNetworkWithConverters(int nOfInput, int[] innerNeurons, int nOfOutput, BiFunction<Integer, Integer, QuantizedSpikingFunction> neuronBuilder, QuantizedValueToSpikeTrainConverter valueToSpikeTrainConverter, QuantizedSpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(new QuantizedLearningMultilayerSpikingNetwork(nOfInput, innerNeurons, nOfOutput, neuronBuilder), createInputConverters(nOfInput, valueToSpikeTrainConverter), createOutputConverters(nOfOutput, spikeTrainToValueConverter));
  }

  public QuantizedLearningMultilayerSpikingNetworkWithConverters(QuantizedSpikingFunction[][] neurons, double[] weights) {
    this(new QuantizedLearningMultilayerSpikingNetwork(neurons, weights));
  }

  public QuantizedLearningMultilayerSpikingNetworkWithConverters(QuantizedSpikingFunction[][] neurons, double[] weights, QuantizedValueToSpikeTrainConverter valueToSpikeTrainConverter, QuantizedSpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(new QuantizedLearningMultilayerSpikingNetwork(neurons, weights), createInputConverters(neurons[0].length, valueToSpikeTrainConverter),
        createOutputConverters(neurons[neurons.length - 1].length, spikeTrainToValueConverter));
  }

  public STDPLearningRule[][][] getLearningRules() {
    return ((QuantizedLearningMultilayerSpikingNetwork)getMultilayerSpikingNetwork()).getLearningRules();
  }

}
