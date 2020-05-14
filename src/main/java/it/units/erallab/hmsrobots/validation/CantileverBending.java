/*
 * Copyright (C) 2020 Eric Medvet <eric.medvet@gmail.com> (as eric)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.validation;

import com.google.common.base.Stopwatch;
import it.units.erallab.hmsrobots.core.objects.ControllableVoxel;
import it.units.erallab.hmsrobots.core.objects.Ground;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.WorldObject;
import it.units.erallab.hmsrobots.core.objects.immutable.Snapshot;
import it.units.erallab.hmsrobots.tasks.AbstractTask;
import it.units.erallab.hmsrobots.util.BoundingBox;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.Point2;
import it.units.erallab.hmsrobots.viewers.SnapshotListener;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.dynamics.World;
import org.dyn4j.dynamics.joint.WeldJoint;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
//TODO should be rewritten to take just a Grid<Voxel> and avoid using Robot<>
public class CantileverBending extends AbstractTask<Grid<ControllableVoxel>, CantileverBending.Result> {

  public static class Result {

    private final double realTime;
    private final double dampingRealTime;
    private final double dampingSimTime;
    private final long steps;
    private final long dampingSteps;
    private final double dampingVoxelStepsPerSecond;
    private final double dampingVoxelSimSecondsPerSecond;
    private final double dampingStepsPerSecond;
    private final double overallVoxelStepsPerSecond;
    private final double overallVoxelSimSecondsPerSecond;
    private final double overallStepsPerSecond;
    private final double yDisplacement;
    private final Map<String, List<Double>> timeEvolution;
    private final List<Point2> finalTopPositions;

    public Result(double realTime, double dampingRealTime, double dampingSimTime, long steps, long dampingSteps, double dampingVoxelStepsPerSecond, double dampingVoxelSimSecondsPerSecond, double dampingStepsPerSecond, double overallVoxelStepsPerSecond, double overallVoxelSimSecondsPerSecond, double overallStepsPerSecond, double yDisplacement, Map<String, List<Double>> timeEvolution, List<Point2> finalTopPositions) {
      this.realTime = realTime;
      this.dampingRealTime = dampingRealTime;
      this.dampingSimTime = dampingSimTime;
      this.steps = steps;
      this.dampingSteps = dampingSteps;
      this.dampingVoxelStepsPerSecond = dampingVoxelStepsPerSecond;
      this.dampingVoxelSimSecondsPerSecond = dampingVoxelSimSecondsPerSecond;
      this.dampingStepsPerSecond = dampingStepsPerSecond;
      this.overallVoxelStepsPerSecond = overallVoxelStepsPerSecond;
      this.overallVoxelSimSecondsPerSecond = overallVoxelSimSecondsPerSecond;
      this.overallStepsPerSecond = overallStepsPerSecond;
      this.yDisplacement = yDisplacement;
      this.timeEvolution = timeEvolution;
      this.finalTopPositions = finalTopPositions;
    }

    public double getRealTime() {
      return realTime;
    }

    public double getDampingRealTime() {
      return dampingRealTime;
    }

    public double getDampingSimTime() {
      return dampingSimTime;
    }

    public long getSteps() {
      return steps;
    }

    public long getDampingSteps() {
      return dampingSteps;
    }

    public double getDampingVoxelStepsPerSecond() {
      return dampingVoxelStepsPerSecond;
    }

    public double getDampingVoxelSimSecondsPerSecond() {
      return dampingVoxelSimSecondsPerSecond;
    }

    public double getDampingStepsPerSecond() {
      return dampingStepsPerSecond;
    }

    public double getOverallVoxelStepsPerSecond() {
      return overallVoxelStepsPerSecond;
    }

    public double getOverallVoxelSimSecondsPerSecond() {
      return overallVoxelSimSecondsPerSecond;
    }

    public double getOverallStepsPerSecond() {
      return overallStepsPerSecond;
    }

    public double getyDisplacement() {
      return yDisplacement;
    }

    public Map<String, List<Double>> getTimeEvolution() {
      return timeEvolution;
    }

    public List<Point2> getFinalTopPositions() {
      return finalTopPositions;
    }

  }

  private final static double WALL_MARGIN = 10d;

  private final double force;
  private final double forceDuration;
  private final double finalT;
  private final double epsilon;

  public CantileverBending(double force, double forceDuration, double finalT, double epsilon, Settings settings) {
    super(settings);
    this.force = force;
    this.forceDuration = forceDuration;
    this.finalT = finalT;
    this.epsilon = epsilon;
  }

  @Override
  public Result apply(Grid<ControllableVoxel> voxels, SnapshotListener listener) {
    List<WorldObject> worldObjects = new ArrayList<>();
    //build voxel compound
    Robot<ControllableVoxel> robot = new Robot<>(
        (t, sensorValues) -> Grid.create(voxels, v -> 0d),
        voxels
    );
    BoundingBox boundingBox = robot.boundingBox();
    worldObjects.add(robot);
    //build ground
    Ground ground = new Ground(new double[]{0, 1}, new double[]{0, boundingBox.max.y - boundingBox.min.y + 2d * WALL_MARGIN});
    worldObjects.add(ground);
    //build world w/o gravity
    World world = new World();
    world.setSettings(settings);
    world.setGravity(new Vector2(0d, 0d));
    for (WorldObject worldObject : worldObjects) {
      worldObject.addTo(world);
    }
    //attach vc to ground
    robot.translate(new Vector2(-boundingBox.min.x + 1d, (boundingBox.max.y - boundingBox.min.y + 2d * WALL_MARGIN) / 2d - 1d));
    for (int y = 0; y < robot.getVoxels().getH(); y++) {
      for (int i : new int[]{0, 3}) {
        WeldJoint joint = new WeldJoint(
            ground.getBodies().get(0),
            robot.getVoxels().get(0, y).getVertexBodies()[i],
            robot.getVoxels().get(0, y).getVertexBodies()[i].getWorldCenter()
        );
        world.addJoint(joint);
      }
    }
    //prepare data
    List<Double> ys = new ArrayList<>((int) Math.round(finalT / settings.getStepFrequency()));
    List<Double> realTs = new ArrayList<>((int) Math.round(finalT / settings.getStepFrequency()));
    List<Double> simTs = new ArrayList<>((int) Math.round(finalT / settings.getStepFrequency()));
    double y0 = robot.getVoxels().get(robot.getVoxels().getW() - 1, robot.getVoxels().getH() / 2).getCenter().y;
    //simulate
    Stopwatch stopwatch = Stopwatch.createStarted();
    double t = 0d;
    while (t < finalT) {
      //add force
      if (t <= forceDuration) {
        for (int y = 0; y < robot.getVoxels().getH(); y++) {
          for (int i : new int[]{1, 2}) {
            robot.getVoxels().get(robot.getVoxels().getW() - 1, y).getVertexBodies()[i].applyForce(new Vector2(0d, -force / 2d / robot.getVoxels().getH()));
          }
        }
      }
      //do step
      t = t + settings.getStepFrequency();
      world.step(1);
      if (listener != null) {
        Snapshot snapshot = new Snapshot(t, worldObjects.stream().map(WorldObject::immutable).collect(Collectors.toList()));
        listener.listen(snapshot);
      }
      //get position
      double y = robot.getVoxels().get(robot.getVoxels().getW() - 1, robot.getVoxels().getH() / 2).getCenter().y;
      ys.add(y - y0);
      realTs.add((double) stopwatch.elapsed(TimeUnit.MICROSECONDS) / 1000000d);
      simTs.add(t);
    }
    stopwatch.stop();
    //compute things
    int dampingIndex = ys.size() - 2;
    while (dampingIndex > 0) {
      if (Math.abs(ys.get(dampingIndex) - ys.get(dampingIndex + 1)) > epsilon) {
        break;
      }
      dampingIndex--;
    }
    double elapsedSeconds = (double) stopwatch.elapsed(TimeUnit.MICROSECONDS) / 1000000d;
    //fill
    Map<String, List<Double>> timeEvolution = new LinkedHashMap<>();
    timeEvolution.put("st", simTs);
    timeEvolution.put("rt", realTs);
    timeEvolution.put("y", ys);
    List<Point2> finalTopPositions = new ArrayList<>();
    for (int x = 0; x < robot.getVoxels().getW(); x++) {
      Vector2 center = robot.getVoxels().get(x, 0).getCenter();
      finalTopPositions.add(Point2.build(center.x, center.y - y0));
    }
    return new Result(
        elapsedSeconds,
        realTs.get(dampingIndex),
        simTs.get(dampingIndex),
        realTs.size(),
        dampingIndex,
        (double) robot.getVoxels().count(v -> v != null) * (double) dampingIndex / realTs.get(dampingIndex),
        (double) robot.getVoxels().count(v -> v != null) * simTs.get(dampingIndex) / realTs.get(dampingIndex),
        (double) dampingIndex / realTs.get(dampingIndex),
        (double) robot.getVoxels().count(v -> v != null) * (double) realTs.size() / elapsedSeconds,
        (double) robot.getVoxels().count(v -> v != null) * simTs.get(simTs.size() - 1) / elapsedSeconds,
        (double) realTs.size() / elapsedSeconds,
        finalTopPositions.get(finalTopPositions.size() - 1).y,
        timeEvolution,
        finalTopPositions
    );
  }

}
