package it.units.erallab.hmsrobots.core.controllers.snn.converters.stv;

import it.units.erallab.hmsrobots.core.controllers.Resettable;

import java.io.Serializable;
import java.util.SortedSet;

public interface SpikeTrainToValueConverter extends Serializable, Resettable {

  double LOWER_BOUND = -1;
  double UPPER_BOUND = 1;
  double DEFAULT_FREQUENCY = 500;

  double convert(SortedSet<Double> spikeTrain, double timeWindowSize);

  void setFrequency(double frequency);

  @Override
  default void reset() {
  }

  default double normalizeValue(double value) {
    value = value * 2 - 1;
    return Math.max(Math.min(UPPER_BOUND, value), LOWER_BOUND);
  }

}
