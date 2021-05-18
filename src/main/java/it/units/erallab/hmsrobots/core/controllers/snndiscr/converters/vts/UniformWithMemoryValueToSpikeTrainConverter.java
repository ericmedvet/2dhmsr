package it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.vts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.SortedSet;
import java.util.TreeSet;

public class UniformWithMemoryValueToSpikeTrainConverter extends UniformValueToSpikeTrainConverter {

  private double lastSpikeTime = 0;

  @JsonCreator
  public UniformWithMemoryValueToSpikeTrainConverter(
          @JsonProperty("frequency") double frequency
  ) {
    super(frequency);
  }

  public UniformWithMemoryValueToSpikeTrainConverter() {
  }

  @Override
  public int[] convert(double value, double timeWindowSize, double timeWindowEnd) {
    value = clipInputValue(value);
    int[] spikes = new int[ARRAY_SIZE];
    if (value == 0) {
      return spikes;
    }
    double timeWindowStart = timeWindowEnd - timeWindowSize;
    double deltaT = computeDeltaT(value);
    for (double t = lastSpikeTime; t < timeWindowEnd; t += deltaT) {
      if (t >= timeWindowStart) {
        spikes[(int)Math.floor(((t - timeWindowStart) / timeWindowSize) * ARRAY_SIZE)] += 1;
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
