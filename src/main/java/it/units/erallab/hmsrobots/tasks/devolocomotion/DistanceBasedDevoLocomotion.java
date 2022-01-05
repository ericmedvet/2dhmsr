/*
 * Copyright (c) "Eric Medvet" 2021.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.units.erallab.hmsrobots.tasks.devolocomotion;

import it.units.erallab.hmsrobots.core.objects.Ground;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.WorldObject;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.SnapshotListener;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import it.units.erallab.hmsrobots.tasks.AbstractTask;
import it.units.erallab.hmsrobots.tasks.locomotion.Locomotion;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.lang3.time.StopWatch;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

/**
 * @author "Eric Medvet" on 2021/09/27 for VSREvolution
 */
public class DistanceBasedDevoLocomotion extends DevoLocomotion {

  private final double stageMinDistance;
  private final double stageMaxT;

  public DistanceBasedDevoLocomotion(
      double stageMinDistance,
      double stageMaxT,
      double maxT,
      double[][] groundProfile,
      double initialPlacement,
      Settings settings
  ) {
    super(maxT, groundProfile, initialPlacement, settings);
    this.stageMinDistance = stageMinDistance;
    this.stageMaxT = stageMaxT;
  }

  public DistanceBasedDevoLocomotion(
      double stageMinDistance,
      double stageMaxT,
      double maxT,
      double[][] groundProfile,
      Settings settings
  ) {
    this(
        stageMinDistance,
        stageMaxT,
        maxT,
        groundProfile,
        groundProfile[0][1] + Locomotion.INITIAL_PLACEMENT_X_GAP,
        settings
    );
  }

  public static class CurrentTarget implements Snapshottable {
    private final List<Double> targets;

    public CurrentTarget(List<Double> targets) {
      this.targets = targets;
    }

    @Override
    public Snapshot getSnapshot() {
      return new Snapshot(targets, getClass());
    }
  }

  @Override
  public DevoOutcome apply(UnaryOperator<Robot> solution, SnapshotListener listener) {
    StopWatch stopWatch = StopWatch.createStarted();
    //init world
    World<Body> world = new World<>();
    world.setSettings(settings);
    Ground ground = new Ground(groundProfile[0], groundProfile[1]);
    Robot robot = solution.apply(null);
    rebuildWorld(ground, robot, world, initialPlacement);
    List<WorldObject> worldObjects = List.of(ground, robot);
    //run
    DevoOutcome devoOutcome = new DevoOutcome();
    Map<Double, Outcome.Observation> observations = new HashMap<>();
    double t = 0d;
    double stageT = t;
    double stageX = robot.boundingBox().min().x();
    List<Double> targetXs = new ArrayList<>();
    CurrentTarget currentTarget = new CurrentTarget(targetXs);
    targetXs.add(stageX);
    targetXs.add(stageX + stageMinDistance);
    //modify listener
    if (listener != null) {
      final SnapshotListener originalListener = listener;
      listener = (sT, s) -> {
        s.getChildren().add(currentTarget.getSnapshot());
        originalListener.listen(sT, s);
      };
    }
    while (t < maxT) {
      t = AbstractTask.updateWorld(
          t, settings.getStepFrequency(), world, worldObjects,
          listener
      );
      observations.put(t, new Outcome.Observation(
          Grid.create(robot.getVoxels(), v -> v == null ? null : v.getVoxelPoly()),
          ground.yAt(robot.center().x()),
          (double) stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000d
      ));
      //check if stage ended
      if (t - stageT > stageMaxT) {
        break;
      }
      //check if develop
      if (robot.boundingBox().min().x() - stageX > stageMinDistance) {
        stageT = t;
        //save outcome
        DevoOutcome.DevoStageOutcome devoStageOutcome = new DevoOutcome.DevoStageOutcome(
            robot,
            new Outcome(observations)
        );
        devoOutcome.addDevoStageOutcome(devoStageOutcome);
        observations = new HashMap<>();
        //develop
        double minX = robot.boundingBox().min().x();
        robot = solution.apply(robot);
        //place
        world.removeAllBodies();
        rebuildWorld(ground, robot, world, minX);
        worldObjects = List.of(ground, robot);
        stageX = robot.center().x();
        targetXs.add(stageX + stageMinDistance);
      }
    }
    if (!observations.isEmpty()) {
      DevoOutcome.DevoStageOutcome devoStageOutcome = new DevoOutcome.DevoStageOutcome(
          robot,
          new Outcome(observations)
      );
      devoOutcome.addDevoStageOutcome(devoStageOutcome);
    }
    stopWatch.stop();
    //prepare outcome
    return devoOutcome;
  }

}
