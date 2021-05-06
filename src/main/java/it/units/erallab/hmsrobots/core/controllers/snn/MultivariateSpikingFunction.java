package it.units.erallab.hmsrobots.core.controllers.snn;

import it.units.erallab.hmsrobots.core.controllers.Resettable;

import java.io.Serializable;
import java.util.SortedSet;

public interface MultivariateSpikingFunction extends Resettable, Serializable {

    SortedSet<Double>[] apply(double t, SortedSet<Double>[] inputs);

}
