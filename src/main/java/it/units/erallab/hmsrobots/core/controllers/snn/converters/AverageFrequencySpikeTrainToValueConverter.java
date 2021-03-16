package it.units.erallab.hmsrobots.core.controllers.snn.converters;

import java.util.SortedSet;

public class AverageFrequencySpikeTrainToValueConverter implements SpikeTrainToValueConverter {

  private double frequency = 35; // hertz
  private static final double LOWER_BOUND = 0;
  private static final double UPPER_BOUND = 1;

  @Override
  public void setFrequency(double frequency) {
    this.frequency = frequency;
  }

  @Override
  public double convert(SortedSet<Double> spikeTrain, double timeWindowSize) {
    if (timeWindowSize == 0) {
      return 0;
    }
    double f = spikeTrain.size() / timeWindowSize / frequency;
    f = Math.max(Math.min(UPPER_BOUND, f), LOWER_BOUND);
    f = f * 2 - 1;
    return f;
  }


}
