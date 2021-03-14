package it.units.erallab.hmsrobots.core.controllers.snn;

public class LIFNeuron extends SpikingNeuron{

    private final double lambdaDecay;

    public LIFNeuron(double restingPotential, double thresholdPotential, double lambdaDecay) {
        super(restingPotential, thresholdPotential);
        this.lambdaDecay = lambdaDecay;
    }

    public LIFNeuron() {
        this(0, 1, 0.5);
    }

    @Override
    protected void acceptWeightedSpike(double spikeTime, double weightedSpike) {
        double decay = (spikeTime - lastInputTime) * lambdaDecay * membranePotential;
        membranePotential -= decay;
        membranePotentialValues.put(spikeTime, membranePotential);
        membranePotential += weightedSpike;
        membranePotentialValues.put(spikeTime + PLOTTING_TIME_STEP, membranePotential);
        lastInputTime = spikeTime;
    }

    @Override
    protected void resetAfterSpike() {
        membranePotential = restingPotential;
    }
}
