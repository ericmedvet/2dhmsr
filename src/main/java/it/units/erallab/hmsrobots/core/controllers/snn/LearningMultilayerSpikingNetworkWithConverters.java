package it.units.erallab.hmsrobots.core.controllers.snn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.stv.MovingAverageSpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.stv.SpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.vts.UniformWithMemoryValueToSpikeTrainConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.vts.ValueToSpikeTrainConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.learning.STDPLearningRule;

import java.util.function.BiFunction;

public class LearningMultilayerSpikingNetworkWithConverters extends MultilayerSpikingNetworkWithConverters{

  @JsonCreator
  public LearningMultilayerSpikingNetworkWithConverters(
      @JsonProperty("learningMultilayerSpikingNetwork") LearningMultilayerSpikingNetwork learningMultilayerSpikingNetwork,
      @JsonProperty("valueToSpikeTrainConverters") ValueToSpikeTrainConverter[] valueToSpikeTrainConverter,
      @JsonProperty("spikeTrainToValueConverters") SpikeTrainToValueConverter[] spikeTrainToValueConverter
  ) {
    super(learningMultilayerSpikingNetwork, valueToSpikeTrainConverter, spikeTrainToValueConverter);
  }

  public LearningMultilayerSpikingNetworkWithConverters(LearningMultilayerSpikingNetwork learningMultilayerSpikingNetwork) {
    this(learningMultilayerSpikingNetwork,
        createInputConverters(learningMultilayerSpikingNetwork.getInputDimension(), new UniformWithMemoryValueToSpikeTrainConverter()),
        createOutputConverters(learningMultilayerSpikingNetwork.getOutputDimension(), new MovingAverageSpikeTrainToValueConverter()));
  }

  public LearningMultilayerSpikingNetworkWithConverters(SpikingFunction[][] neurons, double[][][] weights, STDPLearningRule[][][] learningRules, ValueToSpikeTrainConverter valueToSpikeTrainConverter, SpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(new LearningMultilayerSpikingNetwork(neurons, weights,learningRules),
        createInputConverters(neurons[0].length, valueToSpikeTrainConverter),
        createOutputConverters(neurons[neurons.length - 1].length, spikeTrainToValueConverter));
  }

  public LearningMultilayerSpikingNetworkWithConverters(SpikingFunction[][] neurons, double[][][] weights, STDPLearningRule[][][] learningRules, ValueToSpikeTrainConverter[] valueToSpikeTrainConverters, SpikeTrainToValueConverter[] spikeTrainToValueConverters) {
    this(new LearningMultilayerSpikingNetwork(neurons, weights, learningRules), valueToSpikeTrainConverters, spikeTrainToValueConverters);
  }

  public LearningMultilayerSpikingNetworkWithConverters(SpikingFunction[][] neurons, double[][][] weights, STDPLearningRule[][][] learningRules) {
    this(new LearningMultilayerSpikingNetwork(neurons, weights,learningRules),
        createInputConverters(neurons[0].length, new UniformWithMemoryValueToSpikeTrainConverter()),
        createOutputConverters(neurons[neurons.length - 1].length, new MovingAverageSpikeTrainToValueConverter()));
  }

  public LearningMultilayerSpikingNetworkWithConverters(int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, STDPLearningRule[] learningRules, SpikingFunction spikingFunction) {
    this(new LearningMultilayerSpikingNetwork(nOfInput, innerNeurons, nOfOutput, weights, (i,j)->spikingFunction, learningRules));
  }

  public LearningMultilayerSpikingNetworkWithConverters(int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, STDPLearningRule[] learningRules, SpikingFunction spikingFunction, ValueToSpikeTrainConverter valueToSpikeTrainConverter, SpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(new LearningMultilayerSpikingNetwork(nOfInput, innerNeurons, nOfOutput, weights, (i,j)->spikingFunction, learningRules), createInputConverters(nOfInput, valueToSpikeTrainConverter), createOutputConverters(nOfOutput, spikeTrainToValueConverter));
  }

  public LearningMultilayerSpikingNetworkWithConverters(int nOfInput, int[] innerNeurons, int nOfOutput, SpikingFunction spikingFunction, ValueToSpikeTrainConverter valueToSpikeTrainConverter, SpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(new LearningMultilayerSpikingNetwork(nOfInput, innerNeurons, nOfOutput, spikingFunction), createInputConverters(nOfInput, valueToSpikeTrainConverter), createOutputConverters(nOfOutput, spikeTrainToValueConverter));
  }

  public LearningMultilayerSpikingNetworkWithConverters(int nOfInput, int[] innerNeurons, int nOfOutput, double[] weights, STDPLearningRule[] learningRules, BiFunction<Integer, Integer, SpikingFunction> neuronBuilder, ValueToSpikeTrainConverter valueToSpikeTrainConverter, SpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(new LearningMultilayerSpikingNetwork(nOfInput, innerNeurons, nOfOutput, weights, neuronBuilder, learningRules), createInputConverters(nOfInput, valueToSpikeTrainConverter), createOutputConverters(nOfOutput, spikeTrainToValueConverter));
  }

  public LearningMultilayerSpikingNetworkWithConverters(int nOfInput, int[] innerNeurons, int nOfOutput, BiFunction<Integer, Integer, SpikingFunction> neuronBuilder) {
    this(new LearningMultilayerSpikingNetwork(nOfInput, innerNeurons, nOfOutput, neuronBuilder));
  }

  public LearningMultilayerSpikingNetworkWithConverters(int nOfInput, int[] innerNeurons, int nOfOutput, BiFunction<Integer, Integer, SpikingFunction> neuronBuilder, ValueToSpikeTrainConverter valueToSpikeTrainConverter, SpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(new LearningMultilayerSpikingNetwork(nOfInput, innerNeurons, nOfOutput, neuronBuilder), createInputConverters(nOfInput, valueToSpikeTrainConverter), createOutputConverters(nOfOutput, spikeTrainToValueConverter));
  }

  public LearningMultilayerSpikingNetworkWithConverters(SpikingFunction[][] neurons, double[] weights) {
    this(new LearningMultilayerSpikingNetwork(neurons, weights));
  }

  public LearningMultilayerSpikingNetworkWithConverters(SpikingFunction[][] neurons, double[] weights, ValueToSpikeTrainConverter valueToSpikeTrainConverter, SpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(new LearningMultilayerSpikingNetwork(neurons, weights), createInputConverters(neurons[0].length, valueToSpikeTrainConverter),
        createOutputConverters(neurons[neurons.length - 1].length, spikeTrainToValueConverter));
  }

  public STDPLearningRule[][][] getLearningRules() {
    return ((LearningMultilayerSpikingNetwork)getMultilayerSpikingNetwork()).getLearningRules();
  }

}
