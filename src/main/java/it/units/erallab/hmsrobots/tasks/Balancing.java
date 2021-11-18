package it.units.erallab.hmsrobots.tasks;

import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.objects.*;
import it.units.erallab.hmsrobots.core.snapshots.SnapshotListener;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.lang3.time.StopWatch;
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
  private final double halfPlatformWidth;
  private final double platformHeight;
  private final double placement;
  private final double transientT;

  public Balancing(double finalT, double halfPlatformWidth, double platformHeight, double placement, Settings settings) {
    super(settings);
    this.finalT = finalT;
    this.halfPlatformWidth = halfPlatformWidth;
    this.platformHeight = platformHeight;
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
    Pedestal pedestal = new Pedestal(halfPlatformWidth, platformHeight);
    pedestal.addTo(world);
    worldObjects.add(pedestal);
    robot.reset();
    //position robot: translate on x
    BoundingBox boundingBox = robot.boundingBox();
    robot.translate(new Vector2(placement - boundingBox.min.x, platformHeight));
    //add robot to world
    robot.addTo(world);
    worldObjects.add(robot);
    //run
    Map<Double, Outcome.Observation> observations = new HashMap<>((int) Math.ceil(finalT / settings.getStepFrequency()));
    double t = 0d;
    while (t < finalT) {
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

}
