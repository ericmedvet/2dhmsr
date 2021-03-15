package it.units.erallab.hmsrobots.core.controllers.snn;

import it.units.erallab.hmsrobots.core.controllers.Resettable;

import java.util.SortedMap;
import java.util.SortedSet;

public interface SpikingFunction extends Resettable {

    SortedSet<Double> compute(SortedMap<Double,Double> weightedSpikes, double time);

}
