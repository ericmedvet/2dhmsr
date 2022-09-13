package it.units.erallab.hmsrobots.tasks.affordances;

import it.units.erallab.hmsrobots.behavior.BehaviorUtils;
import it.units.erallab.hmsrobots.core.controllers.DistributedSensingWithAffordances;
import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;

import java.util.Map;
import java.util.Objects;


public class AffordancesOutcome extends Outcome {

  private final Point2 initialPosition;
  private final Point2 target;
  private final DistributedSensingWithAffordances controller;
  private final boolean isPassable;

  public AffordancesOutcome(Map<Double, Observation> observations, Point2 initialPosition, Point2 target,
                            DistributedSensingWithAffordances controller, boolean isPassable) {
    super(observations);
    this.initialPosition = initialPosition;
    this.target = target;
    this.controller = controller;
    this.isPassable = isPassable;
  }

  public double getLocomotionScore() {
    double maxDistance = euclideanDistance(initialPosition, target);
    Point2 finalCenter = BehaviorUtils.center(observations.get(observations.lastKey())
        .voxelPolies()
        .values()
        .stream()
        .filter(Objects::nonNull)
        .toList());
    return euclideanDistance(finalCenter, target) / maxDistance;
  }

  public double getSensingScore() {
    double sensinsScore = 0.0;
    for (int vote : controller.getVotes()) {
      if (isPassable && vote == 1) sensinsScore += 1.0;
      else if (!isPassable && vote == 0) sensinsScore += 1.0;
    }
    sensinsScore /= (controller.getVotes().size() == 0) ? 1 : controller.getVotes().size();
    return sensinsScore;
  }

  public static double euclideanDistance(Point2 a, Point2 b) {
    return Math.sqrt(Math.pow((a.x() - b.x()), 2) + Math.pow((a.y()) - b.y(), 2));
  }

}
