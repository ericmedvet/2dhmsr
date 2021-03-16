package it.units.erallab.hmsrobots.core.controllers.snn.converters.vts;

import java.io.Serializable;
import java.util.SortedSet;

public interface ValueToSpikeTrainConverter extends Serializable {

  double LOWER_BOUND = -1;
  double UPPER_BOUND = 1;

  SortedSet<Double> convert(double value, double timeWindowSize);

  void setFrequency(double frequency);

  default double normalizeValue(double value) {
    //if (value > UPPER_BOUND || value < LOWER_BOUND) {
    //  throw new IllegalArgumentException(String.format("Expected input in range [%f,%f]: found %f", LOWER_BOUND, UPPER_BOUND, value));
    //}
    value = Math.max(Math.min(UPPER_BOUND, value), LOWER_BOUND);
    return (1 + value) / 2;
  }

}
