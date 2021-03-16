package it.units.erallab.hmsrobots.core.controllers.snn.converters;

import java.util.SortedSet;
import java.util.TreeSet;

public class UniformValueToSpikeTrainConverter implements ValueToSpikeTrainConverter {

  private static final double LOWER_BOUND = -1;
  private static final double UPPER_BOUND = 1;
  private double frequency = 601; // hertz

  public UniformValueToSpikeTrainConverter() {
  }

  public UniformValueToSpikeTrainConverter(double frequency) {
    this.frequency = frequency;
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

  protected double normalizeValue(double value) {
    //if (value > UPPER_BOUND || value < LOWER_BOUND) {
    //  throw new IllegalArgumentException(String.format("Expected input in range [%f,%f]: found %f", LOWER_BOUND, UPPER_BOUND, value));
    //}
    value = Math.max(Math.min(UPPER_BOUND, value), LOWER_BOUND);
    return (1 + value) / 2;
  }

  protected double computeDeltaT(double value, double timeWindowSize) {
    double frequency = value * this.frequency;
    return (1 / frequency / timeWindowSize);
  }

}
