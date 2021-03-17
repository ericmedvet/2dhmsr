package it.units.erallab.hmsrobots.core.controllers.snn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LIFNeuron extends SpikingNeuron {

  @JsonProperty
  private final double lambdaDecay;

  @JsonCreator
  public LIFNeuron(
          @JsonProperty("restingPotential") double restingPotential,
          @JsonProperty("thresholdPotential") double thresholdPotential,
          @JsonProperty("lambdaDecay") double lambdaDecay
  ) {
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
    if (plotMode) {
      membranePotentialValues.put(spikeTime, membranePotential);
    }
    membranePotential += weightedSpike;
    if (plotMode) {
      membranePotentialValues.put(spikeTime + PLOTTING_TIME_STEP, membranePotential);
    }
    lastInputTime = spikeTime;
  }

  @Override
  protected void resetAfterSpike() {
    membranePotential = restingPotential;
    if (plotMode) {
      membranePotentialValues.put(lastInputTime + 2 * PLOTTING_TIME_STEP, membranePotential);
    }
  }

}
