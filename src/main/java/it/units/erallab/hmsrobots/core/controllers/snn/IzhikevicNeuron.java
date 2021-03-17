package it.units.erallab.hmsrobots.core.controllers.snn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IzhikevicNeuron extends SpikingNeuron {

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
          @JsonProperty("d") double d
  ) {
    super(restingPotential, thresholdPotential);
    this.a = a;
    this.b = b;
    this.c = c;
    this.d = d;
    membraneRecovery = b * membranePotential;
  }

  public IzhikevicNeuron() {
    this(-70, 30, 0.02, 0.2, -65, 2);
  }

  @Override
  protected void acceptWeightedSpike(double spikeTime, double weightedSpike) {
    double I = b + weightedSpike;
    double deltaV = (spikeTime - lastInputTime) * (0.04 * Math.pow(membranePotential, 2) + 5 * membranePotential + 140 - membraneRecovery + I);
    double deltaU = (spikeTime - lastInputTime) * a * (b * membranePotential - membraneRecovery);
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
