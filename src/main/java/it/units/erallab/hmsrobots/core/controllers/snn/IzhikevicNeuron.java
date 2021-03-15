package it.units.erallab.hmsrobots.core.controllers.snn;

public class IzhikevicNeuron extends SpikingNeuron {

  private double membraneRecovery;
  private final double a;
  private final double b;
  private final double c;
  private final double d;

  public IzhikevicNeuron(double restingPotential, double thresholdPotential, double a, double b, double c, double d) {
    super(restingPotential, thresholdPotential);
    this.a = a;
    this.b = b;
    this.c = c;
    this.d = d;
    membraneRecovery = b * membranePotential;
  }

  public IzhikevicNeuron() {
    super(-70, 30);
    a = 0.02;
    b = 0.2;
    c = -65;
    d = 2;
    membraneRecovery = b * membranePotential;
  }

  @Override
  protected void acceptWeightedSpike(double spikeTime, double weightedSpike) {
    double I = b + weightedSpike;
    double deltaV = (spikeTime - lastInputTime) * (0.04 * Math.pow(membranePotential, 2) + 5 * membranePotential + 140 - membraneRecovery + I);
    double deltaU = (spikeTime - lastInputTime) * a * (b * membranePotential - membraneRecovery);
    membranePotential += deltaV;
    membraneRecovery += deltaU;
    membranePotentialValues.put(spikeTime, membranePotential);
    lastInputTime = spikeTime;
  }

  @Override
  protected void resetAfterSpike() {
    membranePotential = c;
    membraneRecovery += d;
  }

  @Override
  public void reset() {
    super.reset();
    membraneRecovery = b * membranePotential;
  }
}
