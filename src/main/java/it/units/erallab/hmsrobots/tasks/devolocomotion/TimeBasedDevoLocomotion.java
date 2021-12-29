/*
 * Copyright (c) "Giorgia Nadizar" 2021.
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
import it.units.erallab.hmsrobots.core.snapshots.SnapshotListener;
import it.units.erallab.hmsrobots.tasks.AbstractTask;
import it.units.erallab.hmsrobots.tasks.devolocomotion.DevoOutcome.DevoStageOutcome;
import it.units.erallab.hmsrobots.tasks.locomotion.Locomotion;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.lang3.time.StopWatch;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.world.World;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.stream.DoubleStream;

/**
 * @author "Giorgia Nadizar" on 2021/11/02 for VSREvolution
 */
public class TimeBasedDevoLocomotion extends DevoLocomotion {

  private final List<Double> developmentSchedule;

  public TimeBasedDevoLocomotion(
      List<Double> developmentSchedule,
      double maxT,
      double[][] groundProfile,
      double initialPlacement,
      Settings settings
  ) {
    super(maxT, groundProfile, initialPlacement, settings);
    this.developmentSchedule = new LinkedList<>(developmentSchedule);
  }

  public TimeBasedDevoLocomotion(
      List<Double> developmentSchedule,
      double maxT,
      double[][] groundProfile,
      Settings settings
  ) {
    this(developmentSchedule, maxT, groundProfile, groundProfile[0][1] + Locomotion.INITIAL_PLACEMENT_X_GAP, settings);
  }

  public static TimeBasedDevoLocomotion fixedIntervalTimeBasedDevoLocomotion(
      double interval,
      double maxT,
      double[][] groundProfile,
      Settings settings
  ) {
    List<Double> developmentSchedule = DoubleStream.iterate(interval, d -> d + interval)
        .limit((long) Math.floor(maxT / interval)).sorted().boxed().toList();
    return new TimeBasedDevoLocomotion(developmentSchedule, maxT, groundProfile, settings);
  }

  public static TimeBasedDevoLocomotion uniformlyDistributedTimeBasedDevoLocomotion(
      int nStages,
      double maxT,
      double[][] groundProfile,
      Settings settings
  ) {
    return fixedIntervalTimeBasedDevoLocomotion(maxT / nStages, maxT, groundProfile, settings);
  }

  @Override
  public DevoOutcome apply(UnaryOperator<Robot> solution, SnapshotListener listener) {
    List<Double> copiedDevelopmentSchedule = new LinkedList<>(developmentSchedule);
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
    double stageFinalT = copiedDevelopmentSchedule.size() > 0 ? copiedDevelopmentSchedule.remove(0) : maxT;
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
      //check if develop
      if (t >= stageFinalT) {
        stageFinalT = copiedDevelopmentSchedule.size() > 0 ? copiedDevelopmentSchedule.remove(0) : maxT;
        //save outcome
        DevoStageOutcome devoStageOutcome = new DevoStageOutcome(robot, new Outcome(observations));
        devoOutcome.addDevoStageOutcome(devoStageOutcome);
        observations = new HashMap<>();
        //develop
        double minX = robot.boundingBox().min().x();
        robot = solution.apply(robot);
        //place
        world.removeAllBodies();
        rebuildWorld(ground, robot, world, minX);
        worldObjects = List.of(ground, robot);
      }
    }
    if (!observations.isEmpty()) {
      DevoStageOutcome devoStageOutcome = new DevoStageOutcome(robot, new Outcome(observations));
      devoOutcome.addDevoStageOutcome(devoStageOutcome);
    }
    stopWatch.stop();
    //prepare outcome
    return devoOutcome;
  }

}
