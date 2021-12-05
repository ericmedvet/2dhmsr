package it.units.erallab.hmsrobots.tasks;

import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.objects.*;
import it.units.erallab.hmsrobots.core.snapshots.SnapshotListener;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.lang3.time.StopWatch;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Federico Pigozzi <pigozzife@gmail.com>
 */
public class Balancing extends AbstractTask<Robot<?>, Outcome> {

  private final double finalT;
  private final double angle;
  private final double halfPlatformWidth;
  private final double placement;
  private final double transientT;
  private static final double GROUND_HALF_LENGTH = 100;

  public Balancing(double finalT, double angle, double halfPlatformWidth, double placement, Settings settings) {
    super(settings);
    this.finalT = finalT;
    this.angle = angle;
    this.halfPlatformWidth = halfPlatformWidth;
    this.placement = placement;
    this.transientT = 2.0D;
  }

  @Override
  public Outcome apply(Robot<?> robot, SnapshotListener listener) {
    StopWatch stopWatch = StopWatch.createStarted();
    //init world
    World world = new World();
    world.setSettings(settings);
    List<WorldObject> worldObjects = new ArrayList<>();
    Ground ground = new Ground(new double[]{-GROUND_HALF_LENGTH, GROUND_HALF_LENGTH}, new double[]{0, 0});
    ground.addTo(world);
    worldObjects.add(ground);
    double platformHeight = Math.sin(angle * Math.PI / 180.0D) * halfPlatformWidth;
    Pedestal pedestal = new Pedestal(halfPlatformWidth, platformHeight);
    pedestal.addTo(world);
    worldObjects.add(pedestal);
    robot.reset();
    //position robot: translate on x
    BoundingBox boundingBox = robot.boundingBox();
    robot.translate(new Vector2(placement - boundingBox.min.x, platformHeight + Pedestal.PLATFORM_HEIGHT / 2));
    //add robot to world
    robot.addTo(world);
    worldObjects.add(robot);
    //run
    Map<Double, Outcome.Observation> observations = new HashMap<>((int) Math.ceil(finalT / settings.getStepFrequency()));
    double t = 0d;
    while (t < finalT && !stopCondition(robot)) {
      t = AbstractTask.updateWorld(t, settings.getStepFrequency(), world, worldObjects, listener);
      observations.put(t, new Outcome.Observation(
              Grid.create(robot.getVoxels(), v -> v == null ? null : v.getVoxelPoly()),
              0.0d,
              (double) stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000d
      ));
      if (t >= transientT) {
        pedestal.setMovable();
      }
    }
    stopWatch.stop();
    //prepare outcome
    return new Outcome(observations);
  }

  public boolean stopCondition(Robot<?> robot) {
    for (Grid.Entry<?> voxel : robot.getVoxels()) {
      if (voxel.getValue() == null) {
        continue;
      }
      for (Body vertexBody : ((SensingVoxel) voxel.getValue()).getVertexBodies()) {
        List<Body> inContactBodies = vertexBody.getInContactBodies(false);
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
