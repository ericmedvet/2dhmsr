package it.units.erallab.hmsrobots.core.controllers.snndiscr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.IntStream;

public abstract class QuantizedSpikingNeuron implements QuantizedSpikingFunction {

  protected static final double TO_MILLIS_MULTIPLIER = 1000;

  @JsonProperty
  protected final double restingPotential;
  @JsonProperty
  protected double thresholdPotential;
  protected double membranePotential;
  protected double lastInputTime = 0;
  private double lastEvaluatedTime = 0;
  protected double sumOfIncomingWeights = 0;

  @JsonProperty
  protected boolean plotMode;
  protected final SortedMap<Double, Double> membranePotentialValues;
  protected final SortedMap<Double, Double> inputSpikesValues;

  protected static final double PLOTTING_TIME_STEP = 0.000000000001;

  @JsonCreator
  public QuantizedSpikingNeuron(
      @JsonProperty("restingPotential") double restingPotential,
      @JsonProperty("thresholdPotential") double thresholdPotential,
      @JsonProperty("plotMode") boolean plotMode
  ) {
    this.restingPotential = restingPotential;
    this.thresholdPotential = thresholdPotential;
    this.plotMode = plotMode;
    membranePotential = restingPotential;
    membranePotentialValues = new TreeMap<>();
    inputSpikesValues = new TreeMap<>();
    if (plotMode) {
      membranePotentialValues.put(lastInputTime, membranePotential);
    }
  }

  public QuantizedSpikingNeuron(double restingPotential, double thresholdPotential) {
    this(restingPotential, thresholdPotential, false);
  }

  @Override
  public int[] compute(double[] weightedSpikes, double t) {
    int[] spikes = new int[weightedSpikes.length];
    double timeWindowSize = t - lastEvaluatedTime;
    if (timeWindowSize == 0) {
      return spikes;
    }
    IntStream.range(0, weightedSpikes.length).forEach(spikeTime -> {
          double absoluteSpikeTime = spikeTime * timeWindowSize / (double) weightedSpikes.length + lastEvaluatedTime;
          double weightedSpike = weightedSpikes[spikeTime];
          if (plotMode && weightedSpike != 0) {
            inputSpikesValues.put(absoluteSpikeTime, weightedSpike);
          }
          acceptWeightedSpike(absoluteSpikeTime, weightedSpike);
          if (membranePotential >= thresholdPotential) {
            spikes[spikeTime] = 1;
            resetAfterSpike();
          }
        }
    );
    lastEvaluatedTime = t;
    return spikes;
  }

  protected abstract void acceptWeightedSpike(double spikeTime, double weightedSpike);

  protected abstract void resetAfterSpike();

  public SortedMap<Double, Double> getMembranePotentialValues() {
    return membranePotentialValues;
  }

  public SortedMap<Double, Double> getInputSpikesValues() {
    return inputSpikesValues;
  }

  public double getRestingPotential() {
    return restingPotential;
  }

  public double getThresholdPotential() {
    return thresholdPotential;
  }

  public double getMembranePotential() {
    return membranePotential;
  }

  public double getLastEvaluatedTime() {
    return lastEvaluatedTime;
  }

  @Override
  public void setPlotMode(boolean plotMode) {
    this.plotMode = plotMode;
    reset();
  }

  @Override
  public void setSumOfIncomingWeights(double sumOfIncomingWeights) {
    this.sumOfIncomingWeights = sumOfIncomingWeights;
  }

  @Override
  public void reset() {
    membranePotential = restingPotential;
    lastEvaluatedTime = 0;
    lastInputTime = 0;
  }
}
