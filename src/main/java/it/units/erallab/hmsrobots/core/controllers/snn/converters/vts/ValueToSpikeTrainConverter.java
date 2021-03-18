package it.units.erallab.hmsrobots.core.controllers.snn.converters.vts;

import it.units.erallab.hmsrobots.core.controllers.Resettable;

import java.io.Serializable;
import java.util.SortedSet;

public interface ValueToSpikeTrainConverter extends Serializable, Resettable {

  double LOWER_BOUND = 0;
  double UPPER_BOUND = 1;
  double DEFAULT_FREQUENCY = 600;

  SortedSet<Double> convert(double value, double timeWindowSize, double timeWindowEnd);

  void setFrequency(double frequency);

  @Override
  default void reset() {
  }

  default double normalizeValue(double value) {
    return Math.max(Math.min(UPPER_BOUND, value), LOWER_BOUND);
    // return (1 + value) / 2;
  }

}
