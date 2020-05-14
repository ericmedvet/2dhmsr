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
import it.units.erallab.hmsrobots.core.controllers.TimeFunctions;
import it.units.erallab.hmsrobots.core.objects.ControllableVoxel;
import it.units.erallab.hmsrobots.core.objects.Ground;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.WorldObject;
import it.units.erallab.hmsrobots.core.objects.immutable.Snapshot;
import it.units.erallab.hmsrobots.tasks.AbstractTask;
import it.units.erallab.hmsrobots.util.BoundingBox;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.SerializableFunction;
import it.units.erallab.hmsrobots.viewers.SnapshotListener;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class RobotControl extends AbstractTask<Grid<ControllableVoxel>, RobotControl.Result> {

  public static class Result {

    private final double realTime;
    private final long steps;
    private final double overallVoxelStepsPerSecond;
    private final double overallVoxelSimSecondsPerSecond;
    private final double overallStepsPerSecond;
    private final double avgBrokenRatio;
    private final double maxVelocityMagnitude;

    public Result(double realTime, long steps, double overallVoxelStepsPerSecond, double overallVoxelSimSecondsPerSecond, double overallStepsPerSecond, double avgBrokenRatio, double maxVelocityMagnitude) {
      this.realTime = realTime;
      this.steps = steps;
      this.overallVoxelStepsPerSecond = overallVoxelStepsPerSecond;
      this.overallVoxelSimSecondsPerSecond = overallVoxelSimSecondsPerSecond;
      this.overallStepsPerSecond = overallStepsPerSecond;
      this.avgBrokenRatio = avgBrokenRatio;
      this.maxVelocityMagnitude = maxVelocityMagnitude;
    }

    public double getRealTime() {
      return realTime;
    }

    public long getSteps() {
      return steps;
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

    public double getAvgBrokenRatio() {
      return avgBrokenRatio;
    }

    public double getMaxVelocityMagnitude() {
      return maxVelocityMagnitude;
    }

  }

  private final static int GROUND_HILLS_N = 100;
  private final static double GROUND_LENGTH = 1000d;
  private final static double INITIAL_PLACEMENT_X_GAP = 1d;
  private final static double INITIAL_PLACEMENT_Y_GAP = 1d;

  private final double finalT;
  private final double groundHillsHeight;
  private final double freq;

  public RobotControl(double finalT, double groundHillsHeight, double freq, Settings settings) {
    super(settings);
    this.finalT = finalT;
    this.groundHillsHeight = groundHillsHeight;
    this.freq = freq;
  }

  @Override
  public Result apply(Grid<ControllableVoxel> voxels, SnapshotListener listener) {
    List<WorldObject> worldObjects = new ArrayList<>();
    //build voxel compound
    Grid<SerializableFunction<Double, Double>> functionGrid = Grid.create(voxels);
    for (Grid.Entry<ControllableVoxel> entry : voxels) {
      functionGrid.set(entry.getX(), entry.getY(), t -> Math.sin(-2d * Math.PI * t * freq + 2d * Math.PI * (double) entry.getX() / (double) voxels.getW()));
    }
    Robot<ControllableVoxel> robot = new Robot<>(
        new TimeFunctions(functionGrid),
        voxels
    );
    worldObjects.add(robot);
    //build ground
    Random random = new Random(1);
    double[] groundXs = new double[GROUND_HILLS_N + 2];
    double[] groundYs = new double[GROUND_HILLS_N + 2];
    for (int i = 1; i < groundXs.length - 1; i++) {
      groundXs[i] = 1d + GROUND_LENGTH / (double) (GROUND_HILLS_N) * (double) (i - 1);
      groundYs[i] = random.nextDouble() * groundHillsHeight;
    }
    groundXs[0] = 0d;
    groundXs[GROUND_HILLS_N + 1] = GROUND_LENGTH;
    groundYs[0] = GROUND_LENGTH / 10d;
    groundYs[GROUND_HILLS_N + 1] = GROUND_LENGTH / 10d;
    Ground ground = new Ground(groundXs, groundYs);
    worldObjects.add(ground);
    double[][] groundProfile = new double[][]{groundXs, groundYs};
    //position robot: x of rightmost point is on 2nd point of profile
    BoundingBox boundingBox = robot.boundingBox();
    double xLeft = groundProfile[0][1] + INITIAL_PLACEMENT_X_GAP;
    double yGroundLeft = groundProfile[1][1];
    double xRight = xLeft + boundingBox.max.x - boundingBox.min.x;
    double yGroundRight = yGroundLeft + (groundProfile[1][2] - yGroundLeft) * (xRight - xLeft) / (groundProfile[0][2] - xLeft);
    double topmostGroundY = Math.max(yGroundLeft, yGroundRight);
    Vector2 targetPoint = new Vector2(xLeft, topmostGroundY + INITIAL_PLACEMENT_Y_GAP);
    Vector2 currentPoint = new Vector2(boundingBox.min.x, boundingBox.min.y);
    Vector2 movement = targetPoint.subtract(currentPoint);
    robot.translate(movement);
    //build world w/o gravity
    World world = new World();
    world.setSettings(settings);
    for (WorldObject worldObject : worldObjects) {
      worldObject.addTo(world);
    }
    //prepare data
    double maxVelocityMagnitude = Double.NEGATIVE_INFINITY;
    double sumOfBrokenRatio = 0d;
    //simulate
    Stopwatch stopwatch = Stopwatch.createStarted();
    double t = 0d;
    long steps = 0;
    while (t < finalT) {
      //do step
      t = t + settings.getStepFrequency();
      world.step(1);
      steps = steps + 1;
      //control
      robot.act(t);
      if (listener != null) {
        Snapshot snapshot = new Snapshot(t, worldObjects.stream().map(WorldObject::immutable).collect(Collectors.toList()));
        listener.listen(snapshot);
      }
      //collect data
      for (Grid.Entry<ControllableVoxel> entry : robot.getVoxels()) {
        if (entry.getValue() != null) {
          double velocityMagnitude = 0d; //TODO fix me! entry.getValue().getSensorReading(Voxel.Sensor.VELOCITY_MAGNITUDE);
          double brokenRatio = 0d; //TODO fix me! entry.getValue().getSensorReading(Voxel.Sensor.BROKEN_RATIO);
          sumOfBrokenRatio = sumOfBrokenRatio + brokenRatio;
          maxVelocityMagnitude = Math.max(maxVelocityMagnitude, velocityMagnitude);
        }
      }
    }
    stopwatch.stop();
    double elapsedSeconds = (double) stopwatch.elapsed(TimeUnit.MICROSECONDS) / 1000000d;
    return new Result(
        elapsedSeconds, steps,
        (double) robot.getVoxels().count(v -> v != null) * (double) steps / elapsedSeconds,
        (double) robot.getVoxels().count(v -> v != null) * finalT / elapsedSeconds,
        (double) steps / elapsedSeconds,
        sumOfBrokenRatio / (double) robot.getVoxels().count(v -> v != null) / (double) steps,
        maxVelocityMagnitude
    );
  }

}
