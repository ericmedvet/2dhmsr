package it.units.erallab.hmsrobots.core.controllers.snn;

import java.util.SortedSet;

public interface MultivariateSpikingFunction {

    SortedSet<Double>[] apply(double t, SortedSet<Double>[] inputs);

}
