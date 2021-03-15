package it.units.erallab.hmsrobots.core.controllers.snn.converters;

import java.util.SortedSet;

public interface SpikeTrainToValueConverter {

    double convert(SortedSet<Double> spikeTrain, double timeWindowSize);

}
