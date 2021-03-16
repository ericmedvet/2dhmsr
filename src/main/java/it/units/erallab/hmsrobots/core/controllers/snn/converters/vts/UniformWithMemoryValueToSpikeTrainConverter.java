package it.units.erallab.hmsrobots.core.controllers.snn.converters.vts;

import java.util.*;
import java.util.stream.Collectors;

public class UniformWithMemoryValueToSpikeTrainConverter extends UniformValueToSpikeTrainConverter {

  private final int memoryLength;
  private SortedSet<Double> previousTrainRemains;

  public UniformWithMemoryValueToSpikeTrainConverter(int memoryLength) {
    super();
    this.memoryLength = memoryLength;
    previousTrainRemains = new TreeSet<>();
  }

  public UniformWithMemoryValueToSpikeTrainConverter(double frequency, int memoryLength) {
    super(frequency);
    this.memoryLength = memoryLength;
    previousTrainRemains = new TreeSet<>();
  }

  public UniformWithMemoryValueToSpikeTrainConverter() {
    this(2);
  }

  public UniformWithMemoryValueToSpikeTrainConverter(double frequency) {
    this(frequency, 2);
  }

  @Override
  public SortedSet<Double> convert(double value, double timeWindowSize) {
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

}
