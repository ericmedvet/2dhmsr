package it.units.erallab.hmsrobots.core.controllers.snn.converters.vts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

public class UniformWithMemoryValueToSpikeTrainConverter extends UniformValueToSpikeTrainConverter {

  private double lastSpikeTime = 0;

  @JsonCreator
  public UniformWithMemoryValueToSpikeTrainConverter(
          @JsonProperty("frequency") double frequency
  ) {
    super(frequency);
  }


  @Override
  public SortedSet<Double> convert(double value, double timeWindowSize, double timeWindowEnd) {
    value = normalizeValue(value);
    SortedSet<Double> spikes = new TreeSet<>();
    if (value == 0) {
      return spikes;
    }
    double timeWindowStart = timeWindowEnd - timeWindowSize;
    double deltaT = computeDeltaT(value);
    for (double t = lastSpikeTime; t <= timeWindowEnd; t += deltaT) {
      if (t >= timeWindowStart) {
        spikes.add(t / timeWindowSize);
        lastSpikeTime = t;
      }
    }
    return spikes;
  }

  @Override
  public void reset() {
    lastSpikeTime = 0;
  }
}
