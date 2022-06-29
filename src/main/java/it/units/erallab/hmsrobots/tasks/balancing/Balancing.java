package it.units.erallab.hmsrobots.tasks.balancing;

import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.objects.*;
import it.units.erallab.hmsrobots.core.snapshots.SnapshotListener;
import it.units.erallab.hmsrobots.tasks.AbstractTask;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.lang3.time.StopWatch;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Federico Pigozzi <pigozzife@gmail.com>
 */
public class Balancing extends AbstractTask<Robot, BalanceOutcome> {

  private static final double GROUND_HALF_LENGTH = 100.0D;
  private final double finalT;
  private final double angle;
  private final double halfPlatformWidth;
  private final double platformHeight;
  private final double placement;
  private final double impulse;

  public Balancing(
      double finalT,
      double angle,
      double halfPlatformWidth,
      double placement,
      double impulse,
      Settings settings
  ) {
    super(settings);
    this.finalT = finalT;
    this.angle = angle;
    this.halfPlatformWidth = halfPlatformWidth;
    platformHeight = Math.sin(angle * Math.PI / 180.0D) * halfPlatformWidth;
    this.placement = placement;
    this.impulse = impulse;
  }

  @Override
  public BalanceOutcome apply(Robot robot, SnapshotListener listener) {
    StopWatch stopWatch = StopWatch.createStarted();
    //init world
    World<Body> world = new World<>();
    world.setSettings(settings);
    List<WorldObject> worldObjects = new ArrayList<>();
    Ground ground = new Ground(new double[]{-GROUND_HALF_LENGTH, GROUND_HALF_LENGTH}, new double[]{0, 0});
    ground.addTo(world);
    worldObjects.add(ground);
    Swing swing = new Swing(halfPlatformWidth, platformHeight, impulse);
    swing.addTo(world);
    worldObjects.add(swing);
    robot.reset();
    //position robot: translate on x
    BoundingBox boundingBox = robot.boundingBox();
    robot.translate(new Vector2(placement - boundingBox.min().x(), 0));
    double targetHeight = platformHeight + Swing.PLATFORM_HEIGHT;
    robot.translate(new Vector2(0, Math.abs(boundingBox.min().y() - targetHeight)));
    //add robot to world
    robot.addTo(world);
    worldObjects.add(robot);
    //run
    Map<Double, Outcome.Observation> observations = new HashMap<>((int) Math.ceil(finalT / settings.getStepFrequency()));
    double t = 0d;
    Map<Double, Double> angles = new HashMap<>((int) Math.ceil(finalT / settings.getStepFrequency()));
    boolean stopped = false;
    while (t < finalT) {
      if (stopCondition(robot)) {
        stopped = true;
        break;
      }
      t = AbstractTask.updateWorld(t, settings.getStepFrequency(), world, worldObjects, listener);
      observations.put(t, new Outcome.Observation(
          Grid.create(robot.getVoxels(), v -> v == null ? null : v.getVoxelPoly()),
          platformHeight,
          (double) stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000d
      ));
      angles.put(t, Math.abs(swing.getAngle()) / angle);
    }
    if (stopped) {
      while (t < finalT) {
        observations.put(t, new Outcome.Observation(
            Grid.create(robot.getVoxels(), v -> v == null ? null : v.getVoxelPoly()),
            platformHeight,
            (double) stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000d
        ));
        angles.put(t, 1.0);
        t = t + settings.getStepFrequency();
      }
    }
    stopWatch.stop();
    //prepare outcome
    return new BalanceOutcome(observations, angles);
  }

  public boolean stopCondition(Robot robot) {
    for (Grid.Entry<Voxel> entry : robot.getVoxels()) {
      if (entry.value() == null) {
        continue;
      }
      Voxel voxel = entry.value();
      for (Body vertexBody : voxel.getVertexBodies()) {
        List<Body> inContactBodies = voxel.getWorld().getInContactBodies(vertexBody, false);
        for (Body inContactBody : inContactBodies) {
          if ((inContactBody.getUserData() != null) && (inContactBody.getUserData().equals(Ground.class))) {
            return true;
          }
        }
      }
    }
    return false;
  }

}
