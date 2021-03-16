package it.units.erallab.hmsrobots.core.controllers.snn.converters.stv;

import java.util.SortedSet;

public class AverageFrequencySpikeTrainToValueConverter implements SpikeTrainToValueConverter {

  private double frequency = 500; // hertz

  @Override
  public void setFrequency(double frequency) {
    this.frequency = frequency;
  }

  @Override
  public double convert(SortedSet<Double> spikeTrain, double timeWindowSize) {
    if (timeWindowSize == 0) {
      return 0;
    }
    return normalizeValue(spikeTrain.size() / timeWindowSize / frequency);
  }

}
