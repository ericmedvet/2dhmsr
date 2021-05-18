package it.units.erallab.hmsrobots.core.controllers.snndiscr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QuantizedLIFNeuron extends QuantizedSpikingNeuron {

  @JsonProperty
  private final double lambdaDecay;

  @JsonCreator
  public QuantizedLIFNeuron(
          @JsonProperty("restingPotential") double restingPotential,
          @JsonProperty("thresholdPotential") double thresholdPotential,
          @JsonProperty("lambdaDecay") double lambdaDecay,
          @JsonProperty("plotMode") boolean plotMode
  ) {
    super(restingPotential, thresholdPotential, plotMode);
    this.lambdaDecay = lambdaDecay;
  }

  public QuantizedLIFNeuron(double restingPotential, double thresholdPotential, double lambdaDecay){
    this(restingPotential,thresholdPotential,lambdaDecay,false);
  }

  public QuantizedLIFNeuron(boolean plotMode) {
    this(0, 1.0, 0.01, plotMode);
  }

  public QuantizedLIFNeuron(){
    this(false);
  }

  @Override
  protected void acceptWeightedSpike(double spikeTime, double weightedSpike) {
    double decay = TO_MILLIS_MULTIPLIER * (spikeTime - lastInputTime) * lambdaDecay * membranePotential;
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
