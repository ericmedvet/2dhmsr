package it.units.erallab.hmsrobots.core.controllers.snn.converters;

import java.util.SortedSet;
import java.util.TreeSet;

public class UniformValueToSpikeTrainConverter implements ValueToSpikeTrainConverter {

  private static final double LOWER_BOUND = -1;
  private static final double UPPER_BOUND = 1;
  private double frequency = 65; // hertz

  @Override
  public void setFrequency(double frequency) {
    this.frequency = frequency;
  }

  @Override
  public SortedSet<Double> convert(double value, double timeWindowSize) {
    //if (value > UPPER_BOUND || value < LOWER_BOUND) {
    //  throw new IllegalArgumentException(String.format("Expected input in range [%f,%f]: found %f", LOWER_BOUND, UPPER_BOUND, value));
    //}
    value = Math.max(Math.min(UPPER_BOUND, value), LOWER_BOUND);
    value = value / 2 + 0.5;

    SortedSet<Double> spikes = new TreeSet<>();
    if (value == 0) {
      return spikes;
    }
    double frequency = value * this.frequency;
    double deltaT = 1 / frequency / timeWindowSize;
    for (double t = deltaT; t <= 1; t += deltaT)
      spikes.add(t);
    return spikes;
  }
}
