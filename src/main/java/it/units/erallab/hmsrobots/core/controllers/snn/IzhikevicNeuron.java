package it.units.erallab.hmsrobots.core.controllers.snn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IzhikevicNeuron extends SpikingNeuron {

  private static final double INPUT_MULTIPLIER = 35;

  private double membraneRecovery;
  @JsonProperty
  private final double a;
  @JsonProperty
  private final double b;
  @JsonProperty
  private final double c;
  @JsonProperty
  private final double d;

  @JsonCreator
  public IzhikevicNeuron(
          @JsonProperty("restingPotential") double restingPotential,
          @JsonProperty("thresholdPotential") double thresholdPotential,
          @JsonProperty("a") double a,
          @JsonProperty("b") double b,
          @JsonProperty("c") double c,
          @JsonProperty("d") double d,
          @JsonProperty("plotMode") boolean plotMode
  ) {
    super(restingPotential, thresholdPotential, plotMode);
    this.a = a;
    this.b = b;
    this.c = c;
    this.d = d;
    membraneRecovery = b * membranePotential;
  }

  public IzhikevicNeuron(double restingPotential, double thresholdPotential, double a, double b, double c, double d) {
    this(restingPotential, thresholdPotential, a, b, c, d, false);
  }

  public IzhikevicNeuron(boolean plotMode) {
    this(-70, 30, 0.02, 0.2, -65, 2, plotMode);
  }

  public IzhikevicNeuron() {
    this(false);
  }

  @Override
  protected void acceptWeightedSpike(double spikeTime, double weightedSpike) {
    double I = b + weightedSpike * INPUT_MULTIPLIER;
    double deltaV = TO_MILLIS_MULTIPLIER * (spikeTime - lastInputTime) * (0.04 * Math.pow(membranePotential, 2) + 5 * membranePotential + 140 - membraneRecovery + I);
    double deltaU = TO_MILLIS_MULTIPLIER * (spikeTime - lastInputTime) * a * (b * membranePotential - membraneRecovery);
    membranePotential += deltaV;
    membraneRecovery += deltaU;
    if (plotMode) {
      membranePotentialValues.put(spikeTime, membranePotential);
    }
    lastInputTime = spikeTime;
  }

  @Override
  protected void resetAfterSpike() {
    membranePotential = c;
    if (plotMode) {
      membranePotentialValues.put(lastInputTime + PLOTTING_TIME_STEP, membranePotential);
    }
    membraneRecovery += d;
  }

  @Override
  public void reset() {
    super.reset();
    membraneRecovery = b * membranePotential;
  }
}
