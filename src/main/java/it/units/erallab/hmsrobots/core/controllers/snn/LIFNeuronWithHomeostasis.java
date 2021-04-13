package it.units.erallab.hmsrobots.core.controllers.snn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LIFNeuronWithHomeostasis extends LIFNeuron {

  @JsonProperty
  private double theta;
  @JsonProperty
  private final double thetaIncrementRate = 0.2;
  @JsonProperty
  private final double thetaDecayRate = 0.01;
  @JsonProperty
  private final double maxThresholdPotential = 1d;

  @JsonCreator
  public LIFNeuronWithHomeostasis(
          @JsonProperty("restingPotential") double restingPotential,
          @JsonProperty("thresholdPotential") double thresholdPotential,
          @JsonProperty("lambdaDecay") double lambdaDecay,
          @JsonProperty("theta") double theta,
          @JsonProperty("plotMode") boolean plotMode
  ) {
    super(restingPotential, thresholdPotential, lambdaDecay, plotMode);
    this.theta = theta;
  }

  public LIFNeuronWithHomeostasis(double restingPotential, double thresholdPotential, double lambdaDecay, double theta) {
    this(restingPotential, thresholdPotential, lambdaDecay, theta, false);
  }

  public LIFNeuronWithHomeostasis(double restingPotential, double thresholdPotential, double lambdaDecay) {
    this(restingPotential, thresholdPotential, lambdaDecay, 0.1, false);
  }

  public LIFNeuronWithHomeostasis(boolean plotMode) {
    this(0, 1.0, 0.01, 0.1, plotMode);
  }

  public LIFNeuronWithHomeostasis() {
    this(false);
  }

  @Override
  protected void acceptWeightedSpike(double spikeTime, double weightedSpike) {
    super.acceptWeightedSpike(spikeTime, weightedSpike);
    thresholdPotential = Math.min(thresholdPotential + theta, sumOfIncomingWeights);
    theta = theta - thetaDecayRate * (spikeTime - lastInputTime) * TO_MILLIS_MULTIPLIER;
  }

  @Override
  protected void resetAfterSpike() {
    super.resetAfterSpike();
    theta += thetaIncrementRate;
  }
}
