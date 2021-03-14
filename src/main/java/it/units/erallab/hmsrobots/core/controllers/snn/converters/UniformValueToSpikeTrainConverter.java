package it.units.erallab.hmsrobots.core.controllers.snn.converters;

import java.util.SortedSet;
import java.util.TreeSet;

public class UniformValueToSpikeTrainConverter implements ValueToSpikeTrainConverter {

  private static final double LOWER_BOUND = 0;
  private static final double UPPER_BOUND = 1;
  private static final double FREQUENCY = 50; // hertz

  // suppose timeWindowSize is given in milliseconds
  @Override
  public SortedSet<Double> convert(double value, double timeWindowSize) {
    if (value > UPPER_BOUND || value < LOWER_BOUND) {
      throw new IllegalArgumentException(String.format("Expected input in range [%f,%f]: found %f", LOWER_BOUND, UPPER_BOUND, value));
    }
    SortedSet<Double> spikes = new TreeSet<>();
    if (value == 0) {
      return spikes;
    }
    double frequency = value * FREQUENCY / 1000;
    double deltaT = 1 / frequency / timeWindowSize;
    for (double t = 0; t < 1; t += deltaT)
      spikes.add(t);
    return spikes;
  }
}
