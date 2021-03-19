package it.units.erallab.hmsrobots.core.controllers.snn.converters.stv;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.SortedSet;

public class AverageFrequencySpikeTrainToValueConverter implements SpikeTrainToValueConverter {

  @JsonProperty
  private double frequency; // hertz

  @JsonCreator
  public AverageFrequencySpikeTrainToValueConverter(
          @JsonProperty("frequency") double frequency
  ) {
    this.frequency = frequency;
  }

  public AverageFrequencySpikeTrainToValueConverter() {
    this(DEFAULT_FREQUENCY);
  }

  @Override
  public void setFrequency(double frequency) {
    this.frequency = frequency;
  }

  @Override
  public double convert(SortedSet<Double> spikeTrain, double timeWindowSize) {
    if (timeWindowSize == 0) {
      return normalizeValue(0);
    }
    return normalizeValue(spikeTrain.size() / timeWindowSize / frequency);
  }

}
