package it.units.erallab.hmsrobots.core.controllers.snn.converters.vts;

import it.units.erallab.hmsrobots.core.controllers.Resettable;

import java.io.Serializable;
import java.util.SortedSet;

public interface ValueToSpikeTrainConverter extends Serializable, Resettable {

  double LOWER_BOUND = -1;
  double UPPER_BOUND = 1;
  double DEFAULT_FREQUENCY = 600;

  SortedSet<Double> convert(double value, double timeWindowSize);

  void setFrequency(double frequency);

  @Override
  default void reset() {
  }

  default double normalizeValue(double value) {
    //if (value > UPPER_BOUND || value < LOWER_BOUND) {
    //  throw new IllegalArgumentException(String.format("Expected input in range [%f,%f]: found %f", LOWER_BOUND, UPPER_BOUND, value));
    //}
    value = Math.max(Math.min(UPPER_BOUND, value), LOWER_BOUND);
    return (1 + value) / 2;
  }

}
