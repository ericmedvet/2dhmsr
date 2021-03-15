package it.units.erallab.hmsrobots.core.controllers.snn;

import java.util.SortedMap;
import java.util.SortedSet;

public interface SpikingFunction {

    SortedSet<Double> compute(SortedMap<Double,Double> weightedSpikes, double time);

}
