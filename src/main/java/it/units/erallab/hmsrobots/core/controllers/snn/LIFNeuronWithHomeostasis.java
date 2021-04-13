package it.units.erallab.hmsrobots.core.controllers.snn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LIFNeuronWithHomeostasis extends LIFNeuron {

  @JsonProperty
  private double startingTheta;
  @JsonProperty
  private double startingThresholdPotential;
  private double theta;
  private static final double THETA_INCREMENT_RATE = 0.2;
  private static final double THETA_DECAY_RATE = 0.01;

  @JsonCreator
  public LIFNeuronWithHomeostasis(
          @JsonProperty("restingPotential") double restingPotential,
          @JsonProperty("thresholdPotential") double thresholdPotential,
          @JsonProperty("lambdaDecay") double lambdaDecay,
          @JsonProperty("theta") double theta,
          @JsonProperty("plotMode") boolean plotMode
  ) {
    super(restingPotential, thresholdPotential, lambdaDecay, plotMode);
    startingTheta = theta;
    startingThresholdPotential = thresholdPotential;
    this.theta = startingTheta;
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
    theta = theta - THETA_DECAY_RATE * (spikeTime - lastInputTime) * TO_MILLIS_MULTIPLIER;
  }

  @Override
  protected void resetAfterSpike() {
    super.resetAfterSpike();
    theta += THETA_INCREMENT_RATE;
  }

  @Override
  public void reset() {
    super.reset();
    theta = startingTheta;
    thresholdPotential = startingThresholdPotential;
  }
}
