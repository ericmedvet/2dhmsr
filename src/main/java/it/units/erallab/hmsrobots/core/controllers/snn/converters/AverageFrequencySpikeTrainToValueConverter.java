package it.units.erallab.hmsrobots.core.controllers.snn.converters;

import java.util.SortedSet;

public class AverageFrequencySpikeTrainToValueConverter implements SpikeTrainToValueConverter {

  private static final double FREQUENCY = 50; // hertz
  private static final double LOWER_BOUND = 0;
  private static final double UPPER_BOUND = 1;

  @Override
  public double convert(SortedSet<Double> spikeTrain, double timeWindowSize) {
    if (spikeTrain.size() == 0)
      return 0;
    double frequency = spikeTrain.size() / timeWindowSize * 1000 / FREQUENCY;
    frequency = Math.max(Math.min(UPPER_BOUND, frequency), LOWER_BOUND);
    frequency = frequency * 2 - 1;
    return frequency;
  }
}
