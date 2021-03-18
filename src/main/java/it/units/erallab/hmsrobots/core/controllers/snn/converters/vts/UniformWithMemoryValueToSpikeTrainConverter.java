package it.units.erallab.hmsrobots.core.controllers.snn.converters.vts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.stream.Collectors;

public class UniformWithMemoryValueToSpikeTrainConverter extends UniformValueToSpikeTrainConverter {

  private static final int DEFAULT_MEMORY_LENGTH = 2;

  @JsonProperty
  private final int memoryLength;
  private SortedSet<Double> previousTrainRemains;

  @JsonCreator
  public UniformWithMemoryValueToSpikeTrainConverter(
          @JsonProperty("frequency") double frequency,
          @JsonProperty("memoryLength") int memoryLength
  ) {
    super(frequency);
    this.memoryLength = memoryLength;
    previousTrainRemains = new TreeSet<>();
  }

  public UniformWithMemoryValueToSpikeTrainConverter(int memoryLength) {
    this(DEFAULT_FREQUENCY, memoryLength);
  }

  public UniformWithMemoryValueToSpikeTrainConverter(double frequency) {
    this(frequency, DEFAULT_MEMORY_LENGTH);
  }

  public UniformWithMemoryValueToSpikeTrainConverter() {
    this(DEFAULT_FREQUENCY, DEFAULT_MEMORY_LENGTH);
  }

  @Override
  public SortedSet<Double> convert(double value, double timeWindowSize, double timeWindowEnd) {
    value = normalizeValue(value);
    SortedSet<Double> spikes = previousTrainRemains.stream().filter(x -> x <= 1).collect(Collectors.toCollection(TreeSet::new));
    previousTrainRemains = previousTrainRemains.stream().filter(x -> x > 1).map(x -> x - 1).collect(Collectors.toCollection(TreeSet::new));
    if (value == 0) {
      return spikes;
    }
    double deltaT = computeDeltaT(value, timeWindowSize);
    double t = deltaT;
    for (; t <= 1; t += deltaT) {
      spikes.add(t);
    }
    for (; t <= memoryLength; t += deltaT) {
      previousTrainRemains.add(t - 1);
    }
    return spikes;
  }

  @Override
  public void reset() {
    previousTrainRemains = new TreeSet<>();
  }
}
