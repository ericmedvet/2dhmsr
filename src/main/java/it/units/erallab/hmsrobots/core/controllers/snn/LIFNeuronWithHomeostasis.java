package it.units.erallab.hmsrobots.core.controllers.snn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.SortedMap;
import java.util.TreeMap;

public class LIFNeuronWithHomeostasis extends LIFNeuron {

  @JsonProperty
  private double startingTheta;
  @JsonProperty
  private double startingThresholdPotential;
  private double theta;
  private static final double THETA_INCREMENT_RATE = 0.2;
  private static final double THETA_DECAY_RATE = 0.01;
  private static final double MAX_THRESHOLD = 10;

  private final SortedMap<Double, Double> thresholdValues;

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
    thresholdValues = new TreeMap<>();
    if (plotMode) {
      thresholdValues.put(lastInputTime, thresholdPotential);
    }
  }

  public LIFNeuronWithHomeostasis(double restingPotential, double thresholdPotential, double lambdaDecay, double theta) {
    this(restingPotential, thresholdPotential, lambdaDecay, theta, false);
  }

  public LIFNeuronWithHomeostasis(double restingPotential, double thresholdPotential, double lambdaDecay) {
    this(restingPotential, thresholdPotential, lambdaDecay, 0d, false);
  }

  public LIFNeuronWithHomeostasis(boolean plotMode) {
    this(0, 1.0, 0.01, 0d, plotMode);
  }

  public LIFNeuronWithHomeostasis() {
    this(false);
  }

  @Override
  protected void acceptWeightedSpike(double spikeTime, double weightedSpike) {
    double previousInputTime = lastInputTime;
    thresholdPotential = Math.min(Math.min(startingThresholdPotential, sumOfIncomingWeights) + theta, MAX_THRESHOLD);
    super.acceptWeightedSpike(spikeTime, weightedSpike);
    if (membranePotential < thresholdPotential) {
      theta = theta - THETA_DECAY_RATE * (spikeTime - previousInputTime) * TO_MILLIS_MULTIPLIER * theta;
    }
    if (plotMode) {
      thresholdValues.put(previousInputTime, thresholdPotential);
    }
  }

  @Override
  protected void resetAfterSpike() {
    theta += THETA_INCREMENT_RATE;
    super.resetAfterSpike();
  }

  @Override
  public void reset() {
    super.reset();
    theta = startingTheta;
    thresholdPotential = startingThresholdPotential;
  }

  public SortedMap<Double, Double> getThresholdValues() {
    return thresholdValues;
  }
}
