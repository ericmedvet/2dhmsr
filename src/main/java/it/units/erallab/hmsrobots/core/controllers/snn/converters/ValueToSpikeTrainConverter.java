package it.units.erallab.hmsrobots.core.controllers.snn.converters;

import java.io.Serializable;
import java.util.SortedSet;

public interface ValueToSpikeTrainConverter extends Serializable {

    SortedSet<Double> convert(double value, double timeWindowSize);

    void setFrequency(double frequency);

}
