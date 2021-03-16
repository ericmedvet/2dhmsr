package it.units.erallab.hmsrobots.core.controllers.snn;

import java.io.Serializable;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public abstract class SpikingNeuron implements SpikingFunction, Serializable {

  protected final double restingPotential;
  protected final double thresholdPotential;
  protected double membranePotential;
  protected double lastInputTime;
  private double lastEvaluatedTime;

  protected final boolean plotMode;
  protected final SortedMap<Double, Double> membranePotentialValues;
  protected final SortedMap<Double, Double> inputSpikesValues;

  protected static final double PLOTTING_TIME_STEP = 0.000000000001;

  public SpikingNeuron(double restingPotential, double thresholdPotential) {
    this(restingPotential, thresholdPotential, false);
  }

  public SpikingNeuron(double restingPotential, double thresholdPotential, boolean plotMode) {
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

  @Override
  public SortedSet<Double> compute(SortedMap<Double, Double> weightedSpikes, double t) {
    double timeWindowSize = t - lastEvaluatedTime;
    SortedSet<Double> spikes = new TreeSet<>();
    weightedSpikes.forEach((spikeTime, weightedSpike) -> {
              double scaledSpikeTime = spikeTime * timeWindowSize + lastEvaluatedTime;
              if (plotMode) {
                inputSpikesValues.put(scaledSpikeTime, weightedSpike);
              }
              acceptWeightedSpike(scaledSpikeTime, weightedSpike);
              if (membranePotential > thresholdPotential) {
                spikes.add(spikeTime);
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
  public void reset() {
    membranePotential = restingPotential;
    lastEvaluatedTime = 0;
    lastInputTime = 0;
  }
}
