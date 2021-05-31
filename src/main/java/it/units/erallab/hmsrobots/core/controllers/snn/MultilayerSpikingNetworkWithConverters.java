package it.units.erallab.hmsrobots.core.controllers.snn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.controllers.Resettable;
import it.units.erallab.hmsrobots.core.controllers.TimedRealFunction;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.stv.MovingAverageSpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.stv.SpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.vts.UniformWithMemoryValueToSpikeTrainConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.vts.ValueToSpikeTrainConverter;
import it.units.erallab.hmsrobots.util.Parametrized;
import it.units.erallab.hmsrobots.util.SerializationUtils;

import java.util.List;
import java.util.SortedSet;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

public class MultilayerSpikingNetworkWithConverters implements TimedRealFunction, Parametrized, Resettable {

  @JsonProperty
  private final MultilayerSpikingNetwork multilayerSpikingNetwork;
  @JsonProperty
  private final ValueToSpikeTrainConverter[] valueToSpikeTrainConverters;
  @JsonProperty
  private final SpikeTrainToValueConverter[] spikeTrainToValueConverters;

  private double previousApplicationTime = 0d;

  @JsonCreator
  public MultilayerSpikingNetworkWithConverters(
      @JsonProperty("multilayerSpikingNetwork") MultilayerSpikingNetwork multilayerSpikingNetwork,
      @JsonProperty("valueToSpikeTrainConverters") ValueToSpikeTrainConverter[] valueToSpikeTrainConverter,
      @JsonProperty("spikeTrainToValueConverters") SpikeTrainToValueConverter[] spikeTrainToValueConverter
  ) {
    this.multilayerSpikingNetwork = multilayerSpikingNetwork;
    this.valueToSpikeTrainConverters = valueToSpikeTrainConverter;
    this.spikeTrainToValueConverters = spikeTrainToValueConverter;
    reset();
  }

  public MultilayerSpikingNetworkWithConverters(MultilayerSpikingNetwork multilayerSpikingNetwork) {
    this(multilayerSpikingNetwork,
        createInputConverters(multilayerSpikingNetwork.getInputDimension(), new UniformWithMemoryValueToSpikeTrainConverter()),
        createOutputConverters(multilayerSpikingNetwork.getOutputDimension(), new MovingAverageSpikeTrainToValueConverter()));
  }

  public MultilayerSpikingNetworkWithConverters(MultilayerSpikingNetwork multilayerSpikingNetwork, ValueToSpikeTrainConverter valueToSpikeTrainConverter, SpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(multilayerSpikingNetwork,
        createInputConverters(multilayerSpikingNetwork.getInputDimension(), valueToSpikeTrainConverter),
        createOutputConverters(multilayerSpikingNetwork.getOutputDimension(), spikeTrainToValueConverter));
  }

  @SuppressWarnings("unchecked")
  @Override
  public double[] apply(double t, double[] input) {
    double deltaT = t - previousApplicationTime;
    SortedSet<Double>[] inputSpikes = new SortedSet[input.length];
    IntStream.range(0, input.length).forEach(i ->
        inputSpikes[i] = valueToSpikeTrainConverters[i].convert(input[i], deltaT, t));
    SortedSet<Double>[] outputSpikes = multilayerSpikingNetwork.apply(t, inputSpikes);
    previousApplicationTime = t;
    double[] output = new double[outputSpikes.length];
    IntStream.range(0, outputSpikes.length).forEach(i ->
        output[i] = spikeTrainToValueConverters[i].convert(outputSpikes[i], deltaT));
    return output;
  }

  @Override
  public int getInputDimension() {
    return multilayerSpikingNetwork.getInputDimension();
  }

  @Override
  public int getOutputDimension() {
    return multilayerSpikingNetwork.getOutputDimension();
  }

  public MultilayerSpikingNetwork getMultilayerSpikingNetwork() {
    return multilayerSpikingNetwork;
  }

  protected static ValueToSpikeTrainConverter[] createInputConverters(int nOfInputs, ValueToSpikeTrainConverter valueToSpikeTrainConverter) {
    ValueToSpikeTrainConverter[] valueToSpikeTrainConverters = new ValueToSpikeTrainConverter[nOfInputs];
    IntStream.range(0, nOfInputs).forEach(i -> {
      valueToSpikeTrainConverters[i] = SerializationUtils.clone(valueToSpikeTrainConverter);
      valueToSpikeTrainConverters[i].reset();
    });
    return valueToSpikeTrainConverters;
  }

  protected static SpikeTrainToValueConverter[] createOutputConverters(int nOfOutputs, SpikeTrainToValueConverter spikeTrainToValueConverter) {
    SpikeTrainToValueConverter[] spikeTrainToValueConverters = new SpikeTrainToValueConverter[nOfOutputs];
    IntStream.range(0, nOfOutputs).forEach(i -> {
      spikeTrainToValueConverters[i] = SerializationUtils.clone(spikeTrainToValueConverter);
      spikeTrainToValueConverters[i].reset();
    });
    return spikeTrainToValueConverters;
  }

  @Override
  public double[] getParams() {
    return multilayerSpikingNetwork.getParams();
  }

  @Override
  public void setParams(double[] params) {
    multilayerSpikingNetwork.setParams(params);
    reset();
  }

  public void setPlotMode(boolean plotMode) {
    multilayerSpikingNetwork.setPlotMode(plotMode);
  }

  public void setSpikesTracker(boolean spikesTracker) {
    multilayerSpikingNetwork.setSpikesTracker(spikesTracker);
  }

  public List<Double>[][] getSpikes() {
    return multilayerSpikingNetwork.getSpikes();
  }

  @Override
  public void reset() {
    multilayerSpikingNetwork.reset();
    previousApplicationTime = 0d;
    IntStream.range(0, spikeTrainToValueConverters.length).forEach(i ->
        spikeTrainToValueConverters[i].reset());
    IntStream.range(0, valueToSpikeTrainConverters.length).forEach(i ->
        valueToSpikeTrainConverters[i].reset());
  }

}
