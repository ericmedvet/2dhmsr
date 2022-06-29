package it.units.erallab.hmsrobots.tasks.balancing;

import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


public class BalanceOutcome extends Outcome {

  private final SortedMap<Double, Double> angles;

  public BalanceOutcome(Map<Double, Observation> observations, Map<Double, Double> angles) {
    super(observations);
    this.angles = Collections.unmodifiableSortedMap(new TreeMap<>(angles));
  }

  public SortedMap<Double, Double> getAngles() {
    return angles;
  }

  public BalanceOutcome subOutcome(double startT, double endT) {
    return new BalanceOutcome(observations.subMap(startT, endT), angles.subMap(startT, endT));
  }

}
