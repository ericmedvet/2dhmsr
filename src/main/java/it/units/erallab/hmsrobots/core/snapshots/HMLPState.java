package it.units.erallab.hmsrobots.core.snapshots;

import it.units.erallab.hmsrobots.util.Domain;

public class HMLPState extends MLPState{
    public HMLPState(double[][] activationValues, double[][][] weights, Domain activationDomain) {
        super(activationValues, weights, activationDomain);
    }
}
