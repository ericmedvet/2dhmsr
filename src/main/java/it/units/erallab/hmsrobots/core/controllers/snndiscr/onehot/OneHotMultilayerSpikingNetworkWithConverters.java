package it.units.erallab.hmsrobots.core.controllers.snndiscr.onehot;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.controllers.Resettable;
import it.units.erallab.hmsrobots.core.controllers.StatefulNN;
import it.units.erallab.hmsrobots.core.controllers.TimedRealFunction;
import it.units.erallab.hmsrobots.core.controllers.snndiscr.QuantizedMultilayerSpikingNetwork;
import it.units.erallab.hmsrobots.core.snapshots.MLPState;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.util.Parametrized;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class OneHotMultilayerSpikingNetworkWithConverters<N extends QuantizedMultilayerSpikingNetwork> implements TimedRealFunction, Parametrized, Resettable, StatefulNN {

  @JsonProperty
  private final N multilayerSpikingNetwork;
  @JsonProperty
  private final InputConverter inputConverter;
  @JsonProperty
  private final OutputConverter outputConverter;

  @JsonCreator
  public OneHotMultilayerSpikingNetworkWithConverters(
      @JsonProperty("multilayerSpikingNetwork") N multilayerSpikingNetwork,
      @JsonProperty("inputConverter") InputConverter inputConverter,
      @JsonProperty("outputConverter") OutputConverter outputConverter
  ) {
    if (multilayerSpikingNetwork.getInputDimension() % inputConverter.getNOfBins() != 0) {
      throw new IllegalArgumentException(String.format("Input size %d not compatible with %d bins", multilayerSpikingNetwork.getInputDimension(), inputConverter.getNOfBins()));
    }
    if (multilayerSpikingNetwork.getOutputDimension() % outputConverter.getNOfBins() != 0) {
      throw new IllegalArgumentException(String.format("Output size %d not compatible with %d bins", multilayerSpikingNetwork.getOutputDimension(), outputConverter.getNOfBins()));
    }
    this.multilayerSpikingNetwork = multilayerSpikingNetwork;
    this.inputConverter = inputConverter;
    this.outputConverter = outputConverter;
    reset();
  }

  public OneHotMultilayerSpikingNetworkWithConverters(N multilayerSpikingNetwork, int inputConverterBins, int outputConverterBins) {
    this(multilayerSpikingNetwork, new InputConverter(inputConverterBins), new OutputConverter(outputConverterBins));
  }

  @Override
  public double[] apply(final double t, double[] input) {
    if (input.length != multilayerSpikingNetwork.getInputDimension() / inputConverter.getNOfBins()) {
      throw new IllegalArgumentException(String.format("Expected input length is %d: found %d", multilayerSpikingNetwork.getInputDimension() / inputConverter.getNOfBins(), input.length));
    }
    int[][] inputSpikes = Arrays.stream(inputConverter.convert(input)).mapToObj(i -> new int[]{i}).toArray(int[][]::new);
    int[] outputSpikes = Arrays.stream(multilayerSpikingNetwork.apply(t, inputSpikes)).mapToInt(arr -> arr[0]).toArray();
    return outputConverter.convert(outputSpikes);
  }

  @Override
  public int getInputDimension() {
    return multilayerSpikingNetwork.getInputDimension() / inputConverter.getNOfBins();
  }

  @Override
  public int getOutputDimension() {
    return multilayerSpikingNetwork.getOutputDimension() / outputConverter.getNOfBins();
  }

  public QuantizedMultilayerSpikingNetwork getMultilayerSpikingNetwork() {
    return multilayerSpikingNetwork;
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

  public void setWeightsTracker(boolean weightsTracker) {
    multilayerSpikingNetwork.setWeightsTracker(weightsTracker);
  }

  public Map<Double, double[]> getWeightsInTime() {
    return multilayerSpikingNetwork.getWeightsInTime();
  }

  @Override
  public MLPState getState() {
    return multilayerSpikingNetwork.getState();
  }

  @Override
  public Snapshot getSnapshot() {
    return multilayerSpikingNetwork.getSnapshot();
  }

  @Override
  public void reset() {
    multilayerSpikingNetwork.reset();
  }

  public N getSNN() {
    return multilayerSpikingNetwork;
  }

}
