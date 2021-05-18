package it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.vts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QuantizedUniformValueToSpikeTrainConverter implements QuantizedValueToSpikeTrainConverter {

  @JsonProperty
  protected double frequency; // hertz
  @JsonProperty
  protected double minFrequency = MIN_FREQUENCY;

  @JsonCreator
  public QuantizedUniformValueToSpikeTrainConverter(
      @JsonProperty("frequency") double frequency
  ) {
    this.frequency = frequency;
  }

  public QuantizedUniformValueToSpikeTrainConverter() {
    this(DEFAULT_FREQUENCY);
  }

  @Override
  public void setFrequency(double frequency) {
    this.frequency = frequency;
  }

  @Override
  public int[] convert(double value, double timeWindowSize, double timeWindowEnd) {
    int[] spikes = new int[ARRAY_SIZE];
    value = clipInputValue(value);
    if (value == 0) {
      return spikes;
    }
    double deltaT = computeDeltaT(value);
    for (double t = deltaT; t < timeWindowSize; t += deltaT) {
      spikes[(int)Math.floor(t / timeWindowSize * ARRAY_SIZE)] += 1;
    }
    return spikes;
  }

  protected double computeDeltaT(double value) {
    double frequencyRange = frequency - minFrequency;
    double frequency = value * frequencyRange + minFrequency;
    return 1 / frequency;
  }

}
