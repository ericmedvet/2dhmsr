package it.units.erallab.hmsrobots.core.controllers.snn.converters;

import java.util.SortedSet;

public interface ValueToSpikeTrainConverter {

    SortedSet<Double> convert(double value, double timeWindowSize);

}
