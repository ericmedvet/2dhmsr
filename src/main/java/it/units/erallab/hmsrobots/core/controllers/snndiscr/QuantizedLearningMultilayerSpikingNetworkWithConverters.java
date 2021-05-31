package it.units.erallab.hmsrobots.core.controllers.snndiscr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.controllers.snn.learning.STDPLearningRule;
import it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.stv.QuantizedMovingAverageSpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.stv.QuantizedSpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.vts.QuantizedUniformWithMemoryValueToSpikeTrainConverter;
import it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.vts.QuantizedValueToSpikeTrainConverter;

import java.util.function.BiFunction;
import java.util.stream.IntStream;

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

  public QuantizedLearningMultilayerSpikingNetworkWithConverters(QuantizedLearningMultilayerSpikingNetwork learningMultilayerSpikingNetwork, QuantizedValueToSpikeTrainConverter quantizedValueToSpikeTrainConverter, QuantizedSpikeTrainToValueConverter quantizedSpikeTrainToValueConverter) {
    this(learningMultilayerSpikingNetwork,
        createInputConverters(learningMultilayerSpikingNetwork.getInputDimension(), quantizedValueToSpikeTrainConverter),
        createOutputConverters(learningMultilayerSpikingNetwork.getOutputDimension(), quantizedSpikeTrainToValueConverter));
  }

  public STDPLearningRule[][][] getLearningRules() {
    return ((QuantizedLearningMultilayerSpikingNetwork)getMultilayerSpikingNetwork()).getLearningRules();
  }

}
