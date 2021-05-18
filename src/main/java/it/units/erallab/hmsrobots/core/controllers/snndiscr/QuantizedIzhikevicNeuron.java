package it.units.erallab.hmsrobots.core.controllers.snndiscr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.SortedMap;
import java.util.TreeMap;

public class QuantizedIzhikevicNeuron extends QuantizedSpikingNeuron {

  public enum IzhikevicParameters {

    REGULAR_SPIKING_PARAMS(30, 0.02, 0.2, -65, 8);

    private final double threshold;
    private final double a;
    private final double b;
    private final double c;
    private final double d;

    IzhikevicParameters(double threshold, double a, double b, double c, double d) {
      this.threshold = threshold;
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
    }

  }

  private static final double INPUT_MULTIPLIER = 15;

  private double membraneRecovery;
  @JsonProperty
  private final double a;
  @JsonProperty
  private final double b;
  @JsonProperty
  private final double c;
  @JsonProperty
  private final double d;

  private final SortedMap<Double, Double> membraneRecoveryValues;

  @JsonCreator
  public QuantizedIzhikevicNeuron(
          @JsonProperty("thresholdPotential") double thresholdPotential,
          @JsonProperty("a") double a,
          @JsonProperty("b") double b,
          @JsonProperty("c") double c,
          @JsonProperty("d") double d,
          @JsonProperty("plotMode") boolean plotMode
  ) {
    super(c, thresholdPotential, plotMode);
    this.a = a;
    this.b = b;
    this.c = c;
    this.d = d;
    membraneRecovery = b * membranePotential;
    membraneRecoveryValues = new TreeMap<>();
    if (plotMode) {
      membraneRecoveryValues.put(lastInputTime, membraneRecovery);
    }
  }

  public QuantizedIzhikevicNeuron(double thresholdPotential, double a, double b, double c, double d) {
    this(thresholdPotential, a, b, c, d, false);
  }

  public QuantizedIzhikevicNeuron(IzhikevicParameters parameters, boolean plotMode) {
    this(parameters.threshold, parameters.a, parameters.b, parameters.c, parameters.d, plotMode);
  }

  public QuantizedIzhikevicNeuron(IzhikevicParameters parameters) {
    this(parameters, false);
  }

  public QuantizedIzhikevicNeuron(boolean plotMode) {
    this(IzhikevicParameters.REGULAR_SPIKING_PARAMS, plotMode);
  }

  public QuantizedIzhikevicNeuron() {
    this(false);
  }

  @Override
  protected void acceptWeightedSpike(double spikeTime, double weightedSpike) {
    double I = b + weightedSpike * INPUT_MULTIPLIER;
    double deltaV = TO_MILLIS_MULTIPLIER * (spikeTime - lastInputTime) * (0.04 * Math.pow(membranePotential, 2) + 5 * membranePotential + 140 - membraneRecovery + I);
    double deltaU = TO_MILLIS_MULTIPLIER * (spikeTime - lastInputTime) * a * (b * membranePotential - membraneRecovery);
    membranePotential += deltaV;
    //if (membranePotential < c) {
    //  membranePotential = c;
    //}
    membraneRecovery += deltaU;
    if (plotMode) {
      membranePotentialValues.put(spikeTime, membranePotential);
      membraneRecoveryValues.put(spikeTime, membraneRecovery);
    }
    lastInputTime = spikeTime;
  }

  @Override
  protected void resetAfterSpike() {
    membranePotential = c;
    membraneRecovery += d;
    if (plotMode) {
      membranePotentialValues.put(lastInputTime + PLOTTING_TIME_STEP, membranePotential);
      membraneRecoveryValues.put(lastInputTime + PLOTTING_TIME_STEP, membraneRecovery);
    }
  }

  @Override
  public void reset() {
    super.reset();
    membraneRecovery = b * membranePotential;
  }

  public SortedMap<Double, Double> getMembraneRecoveryValues() {
    return membraneRecoveryValues;
  }
}
