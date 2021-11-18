package it.units.erallab.hmsrobots.tasks;

import it.units.erallab.hmsrobots.behavior.Footprint;
import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.objects.*;
import it.units.erallab.hmsrobots.core.snapshots.SnapshotListener;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.lang3.time.StopWatch;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class Balancing extends AbstractTask<Robot<?>, Outcome> {

  private static final double PLATFORM_HEIGHT = 10d;
  private static final double PLACEMENT_STD = 10d;
  private static final int FOOTPRINT_BINS = 8;
  private static final int MASK_BINS = 16;
  private final double maxT;
  private final double halfPlatformWidth;
  private final Random seed;

  public Balancing(double t, double length, Random random, Settings settings) {
    super(settings);
    maxT = t;
    halfPlatformWidth = length;
    seed = random;
  }

  @Override
  public Outcome apply(Robot<?> robot, SnapshotListener listener) {
    StopWatch stopWatch = StopWatch.createStarted();
    //init world
    World world = new World();
    world.setSettings(this.settings);
    List<WorldObject> worldObjects = new ArrayList<>();
    Pedestal pedestal = new Pedestal(halfPlatformWidth, PLATFORM_HEIGHT);
    pedestal.addTo(world);
    worldObjects.add(pedestal);
    robot.reset();
    //position robot: translate on x
    BoundingBox boundingBox = robot.boundingBox();
    double initialPlacement = -10;//seed.nextGaussian() * PLACEMENT_STD;
    robot.translate(new Vector2(initialPlacement - boundingBox.min.x, 0));
    //translate on y
    double minYGap = robot.getVoxels().values().stream()
            .filter(Objects::nonNull)
            .mapToDouble(v -> ((Voxel) v.immutable()).getShape().boundingBox().min.y - PLATFORM_HEIGHT)
            .min().orElse(0d);
    robot.translate(new Vector2(0, 20.0));
    //add robot to world
    robot.addTo(world);
    worldObjects.add(robot);
    //run
    List<Outcome.Observation> observations = new ArrayList<>((int) Math.ceil(this.maxT / this.settings.getStepFrequency()));
    double t = 0d;
    while (t < this.maxT) {
      t = AbstractTask.updateWorld(t, this.settings.getStepFrequency(), world, worldObjects, listener);
      observations.add(new Outcome.Observation(
              t,
              Point2.build(robot.getCenter()),
              PLATFORM_HEIGHT,
              footprint(robot, FOOTPRINT_BINS),
              mask(robot, MASK_BINS),
              robot.getVoxels().values().stream()
                      .filter(v -> (v instanceof ControllableVoxel))
                      .mapToDouble(ControllableVoxel::getControlEnergy)
                      .sum() - (observations.isEmpty() ? 0d : observations.get(observations.size() - 1).getControlEnergy()),
              robot.getVoxels().values().stream()
                      .filter(v -> (v instanceof ControllableVoxel))
                      .mapToDouble(ControllableVoxel::getAreaRatioEnergy)
                      .sum() - (observations.isEmpty() ? 0d : observations.get(observations.size() - 1).getAreaRatioEnergy()),
              (double) stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000d
      ));
    }
    stopWatch.stop();
    //prepare outcome
    return new Outcome(observations);
  }

  private static Grid<Boolean> mask(Robot<?> robot, int n) {
    List<BoundingBox> boxes = robot.getVoxels().values().stream()
            .filter(Objects::nonNull)
            .map(it.units.erallab.hmsrobots.core.objects.Voxel::boundingBox)
            .collect(Collectors.toList());
    double robotMinX = boxes.stream().mapToDouble(b -> b.min.x).min().orElseThrow(() -> new IllegalArgumentException("Empty robot"));
    double robotMaxX = boxes.stream().mapToDouble(b -> b.max.x).max().orElseThrow(() -> new IllegalArgumentException("Empty robot"));
    double robotMinY = boxes.stream().mapToDouble(b -> b.min.y).min().orElseThrow(() -> new IllegalArgumentException("Empty robot"));
    double robotMaxY = boxes.stream().mapToDouble(b -> b.max.y).max().orElseThrow(() -> new IllegalArgumentException("Empty robot"));
    //adjust box to make it squared
    if ((robotMaxY - robotMinY) < (robotMaxX - robotMinX)) {
      double d = (robotMaxX - robotMinX) - (robotMaxY - robotMinY);
      robotMaxY = robotMaxY + d / 2;
      robotMinY = robotMinY - d / 2;
    } else if ((robotMaxY - robotMinY) > (robotMaxX - robotMinX)) {
      double d = (robotMaxY - robotMinY) - (robotMaxX - robotMinX);
      robotMaxX = robotMaxX + d / 2;
      robotMinX = robotMinX - d / 2;
    }
    Grid<Boolean> mask = Grid.create(n, n, false);
    for (BoundingBox b : boxes) {
      int minXIndex = (int) Math.round((b.min.x - robotMinX) / (robotMaxX - robotMinX) * (double) (n - 1));
      int maxXIndex = (int) Math.round((b.max.x - robotMinX) / (robotMaxX - robotMinX) * (double) (n - 1));
      int minYIndex = (int) Math.round((b.min.y - robotMinY) / (robotMaxY - robotMinY) * (double) (n - 1));
      int maxYIndex = (int) Math.round((b.max.y - robotMinY) / (robotMaxY - robotMinY) * (double) (n - 1));
      for (int x = minXIndex; x <= maxXIndex; x++) {
        for (int y = minYIndex; y <= maxYIndex; y++) {
          mask.set(x, y, true);
        }
      }
    }
    return mask;
  }

  private static Footprint footprint(Robot<?> robot, int n) {
    double robotMinX = Double.POSITIVE_INFINITY;
    double robotMaxX = Double.NEGATIVE_INFINITY;
    List<double[]> contacts = new ArrayList<>();
    for (it.units.erallab.hmsrobots.core.objects.Voxel v : robot.getVoxels().values()) {
      if (v == null) {
        continue;
      }
      double touchMinX = Double.POSITIVE_INFINITY;
      double touchMaxX = Double.NEGATIVE_INFINITY;
      for (Body body : v.getVertexBodies()) {
        AABB box = body.createAABB();
        robotMinX = Math.min(robotMinX, box.getMinX());
        robotMaxX = Math.max(robotMaxX, box.getMaxX());
        for (Body contactBody : body.getInContactBodies(false)) {
          if (contactBody.getUserData().equals(Ground.class)) {
            touchMinX = Math.min(touchMinX, box.getMinX());
            touchMaxX = Math.max(touchMaxX, box.getMaxX());
            contacts.add(new double[]{touchMinX, touchMaxX});
          }
        }
      }
    }
    boolean[] mask = new boolean[n];
    for (double[] contact : contacts) {
      int minIndex = (int) Math.round((contact[0] - robotMinX) / (robotMaxX - robotMinX) * (double) (n - 1));
      int maxIndex = (int) Math.round((contact[1] - robotMinX) / (robotMaxX - robotMinX) * (double) (n - 1));
      for (int x = minIndex; x <= Math.min(maxIndex, n - 1); x++) {
        mask[x] = true;
      }
    }
    return new Footprint(mask);
  }

}
