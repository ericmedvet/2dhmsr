package it.units.erallab.hmsrobots.tasks.affordances;

import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.Wall;
import it.units.erallab.hmsrobots.core.objects.WorldObject;
import it.units.erallab.hmsrobots.core.snapshots.SnapshotListener;
import it.units.erallab.hmsrobots.tasks.AbstractTask;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.lang3.time.StopWatch;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

import java.util.*;
import java.util.concurrent.TimeUnit;


public class SqueezeWithAffordances extends AbstractTask<Robot, Outcome> {

  private final int bodyLength;
  private final double apertureSize;
  private final double finalT;
  private final double WALL_THICKNESS = 5.0;

  public SqueezeWithAffordances(double finalT, int bodyLength, double apertureSize, Settings settings) {
    super(settings);
    this.finalT = finalT;
    this.bodyLength = bodyLength;
    this.apertureSize = apertureSize;
  }

  @Override
  public Outcome apply(Robot robot, SnapshotListener listener) {
    StopWatch stopWatch = StopWatch.createStarted();
    //init world
    World<Body> world = new World<>();
    world.setGravity(new Vector2(0.0, 0.0));
    world.setSettings(settings);
    List<WorldObject> worldObjects = new ArrayList<>();
    Wall leftFrontWall = new Wall(- bodyLength * 1.5, bodyLength * 2, bodyLength * 3 - apertureSize / 2.0, WALL_THICKNESS);
    Wall rightFrontWall = new Wall(bodyLength * 1.5 + apertureSize / 2.0, bodyLength * 2, bodyLength * 3 - apertureSize / 2.0, WALL_THICKNESS);
    leftFrontWall.addTo(world);
    worldObjects.add(leftFrontWall);
    rightFrontWall.addTo(world);
    worldObjects.add(rightFrontWall);
    robot.reset();
    //position robot: translate
    Vector2 initialPlacement = new Vector2(0, - bodyLength * 1.5);
    BoundingBox boundingBox = robot.boundingBox();
    robot.translate(new Vector2(initialPlacement.x - boundingBox.min().x(), initialPlacement.y - boundingBox.min().y()));
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
          0.0,
          (double) stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000d
      ));
    }
    stopWatch.stop();
    //prepare outcome
    return new Outcome(observations);
  }

}
