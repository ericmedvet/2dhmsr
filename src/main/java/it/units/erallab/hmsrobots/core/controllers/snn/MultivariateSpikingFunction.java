package it.units.erallab.hmsrobots.core.controllers.snn;

import it.units.erallab.hmsrobots.core.controllers.Resettable;

import java.util.SortedSet;

public interface MultivariateSpikingFunction {

    SortedSet<Double>[] apply(double t, SortedSet<Double>[] inputs);

}
