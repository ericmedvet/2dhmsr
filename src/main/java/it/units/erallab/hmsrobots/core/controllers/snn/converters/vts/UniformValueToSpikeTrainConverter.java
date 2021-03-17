package it.units.erallab.hmsrobots.core.controllers.snn.converters.vts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.SortedSet;
import java.util.TreeSet;

public class UniformValueToSpikeTrainConverter implements ValueToSpikeTrainConverter {

  @JsonProperty
  protected double frequency; // hertz

  @JsonCreator
  public UniformValueToSpikeTrainConverter(
          @JsonProperty("frequency") double frequency
  ) {
    this.frequency = frequency;
  }

  public UniformValueToSpikeTrainConverter() {
    this(DEFAULT_FREQUENCY);
  }

  @Override
  public void setFrequency(double frequency) {
    this.frequency = frequency;
  }

  @Override
  public SortedSet<Double> convert(double value, double timeWindowSize) {
    value = normalizeValue(value);
    SortedSet<Double> spikes = new TreeSet<>();
    if (value == 0) {
      return spikes;
    }
    double deltaT = computeDeltaT(value, timeWindowSize);
    for (double t = deltaT; t <= 1; t += deltaT)
      spikes.add(t);
    return spikes;
  }

  protected double computeDeltaT(double value, double timeWindowSize) {
    double frequency = value * this.frequency;
    return (1 / frequency / timeWindowSize);
  }

}
