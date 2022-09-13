package it.units.erallab.hmsrobots.tasks.affordances;

import it.units.erallab.hmsrobots.core.controllers.DistributedSensingWithAffordances;
import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.geometry.Point2;
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

import java.util.*;
import java.util.concurrent.TimeUnit;


public class SqueezeWithAffordances extends AbstractTask<Robot, AffordancesOutcome> {

  private final int bodyLength;
  private final double apertureSize;
  private final boolean isPassable;
  private final double finalT;
  private final double WALL_THICKNESS = 5.0;

  public SqueezeWithAffordances(double finalT, int bodyLength, double apertureSize, boolean isPassable, Settings settings) {
    super(settings);
    this.bodyLength = bodyLength;
    this.apertureSize = apertureSize;
    this.isPassable = isPassable;
    this.finalT = finalT;
  }

  @Override
  public AffordancesOutcome apply(Robot robot, SnapshotListener listener) {
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
    Point2 target = new Point2(0.0, bodyLength * 4);
    //position robot: translate
    Point2 initialPlacement = new Point2(0, - bodyLength * 1.5);
    BoundingBox boundingBox = robot.boundingBox();
    robot.translate(new Vector2(initialPlacement.x() - boundingBox.min().x(), initialPlacement.y() - boundingBox.min().y()));
    //add robot to world
    robot.addTo(world);
    worldObjects.add(robot);
    initialPlacement = robot.center();
    //run
    DistributedSensingWithAffordances controller = (DistributedSensingWithAffordances) robot.getController();
    Map<Double, Outcome.Observation> observations = new HashMap<>((int) Math.ceil(finalT / settings.getStepFrequency()));
    double t = 0d;
    while (t < finalT) {
      t = AbstractTask.updateWorld(t, settings.getStepFrequency(), world, worldObjects, listener);
      observations.put(t, new Outcome.Observation(
          Grid.create(robot.getVoxels(), v -> v == null ? null : v.getVoxelPoly()),
          0.0,
          (double) stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000d
      ));
      if (!controller.getLeftFirstContact()) controller.setLeftFirstContact(robot.getVoxels().stream().anyMatch(x -> x.value() != null && isTouchingWall(x.value(), leftFrontWall)));
      if (!controller.getRightFirstContact()) controller.setRightFirstContact(robot.getVoxels().stream().anyMatch(x -> x.value() != null && isTouchingWall(x.value(), rightFrontWall)));
    }
    stopWatch.stop();
    //prepare outcome
    return new AffordancesOutcome(observations, target, initialPlacement, controller, isPassable);
  }

  public static boolean isTouchingWall(Voxel voxel, Wall wall) {
    for (Body vertexBody : voxel.getVertexBodies()) {
      List<Body> inContactBodies = voxel.getWorld().getInContactBodies(vertexBody, false);
      for (Body inContactBody : inContactBodies) {
        if (inContactBody == wall.getBodies().get(0)) {
          return true;
        }
      }
    }
    return false;
  }

}
