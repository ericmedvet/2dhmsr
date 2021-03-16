package it.units.erallab.hmsrobots.core.controllers.snn.converters;

import java.io.Serializable;
import java.util.SortedSet;

public interface SpikeTrainToValueConverter extends Serializable {

    double convert(SortedSet<Double> spikeTrain, double timeWindowSize);

    void setFrequency(double frequency);

}
