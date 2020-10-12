/*
 * Copyright (C) 2020 Eric Medvet <eric.medvet@gmail.com> (as Eric Medvet <eric.medvet@gmail.com>)
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
package it.units.erallab.hmsrobots.tasks.locomotion;

import it.units.erallab.hmsrobots.core.objects.ControllableVoxel;
import it.units.erallab.hmsrobots.core.objects.Ground;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.WorldObject;
import it.units.erallab.hmsrobots.core.objects.immutable.Voxel;
import it.units.erallab.hmsrobots.tasks.AbstractTask;
import it.units.erallab.hmsrobots.util.BoundingBox;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.Point2;
import it.units.erallab.hmsrobots.viewers.SnapshotListener;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.AABB;
import org.dyn4j.geometry.Vector2;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Locomotion extends AbstractTask<Robot<?>, Outcome> {

  private final static double INITIAL_PLACEMENT_X_GAP = 1d;
  private final static double INITIAL_PLACEMENT_Y_GAP = 1d;
  private final static double TERRAIN_BORDER_HEIGHT = 100d;
  public static final int TERRAIN_LENGTH = 2000;
  private static final int FOOTPRINT_BINS = 8;
  private static final int MASK_BINS = 16;

  private final double finalT;
  private final double[][] groundProfile;
  private final double initialPlacement;

  public Locomotion(double finalT, double[][] groundProfile, Settings settings) {
    this(finalT, groundProfile, groundProfile[0][1] + INITIAL_PLACEMENT_X_GAP, settings);
  }

  public Locomotion(double finalT, double[][] groundProfile, double initialPlacement, Settings settings) {
    super(settings);
    this.finalT = finalT;
    this.groundProfile = groundProfile;
    this.initialPlacement = initialPlacement;
  }

  @Override
  public Outcome apply(Robot<?> robot, SnapshotListener listener) {
    //init world
    World world = new World();
    world.setSettings(settings);
    List<WorldObject> worldObjects = new ArrayList<>();
    Ground ground = new Ground(groundProfile[0], groundProfile[1]);
    ground.addTo(world);
    worldObjects.add(ground);
    //position robot: translate on x
    BoundingBox boundingBox = robot.boundingBox();
    robot.translate(new Vector2(initialPlacement - boundingBox.min.x, 0));
    //translate on y
    double minYGap = robot.getVoxels().values().stream()
        .filter(Objects::nonNull)
        .mapToDouble(v -> ((Voxel) v.immutable()).getShape().boundingBox().min.y - ground.yAt(v.getCenter().x))
        .min().orElse(0d);
    robot.translate(new Vector2(0, INITIAL_PLACEMENT_Y_GAP - minYGap));
    //get initial x
    double initCenterX = robot.getCenter().x;
    //add robot to world
    robot.addTo(world);
    worldObjects.add(robot);
    Map<Double, Point2> centerTrajectory = new HashMap<>();
    Map<Double, Footprint> footprints = new HashMap<>();
    Map<Double, Grid<Boolean>> masks = new HashMap<>();
    //run
    double t = 0d;
    while (t < finalT) {
      t = AbstractTask.updateWorld(t, settings.getStepFrequency(), world, worldObjects, listener);
      centerTrajectory.put(t, Point2.build(robot.getCenter()));
      footprints.put(t, footprint(robot, FOOTPRINT_BINS));
      masks.put(t, mask(robot, MASK_BINS));
    }
    //prepare outcome
    return new Outcome(
        robot.getCenter().x - initCenterX,
        t,
        Math.max(boundingBox.max.x - boundingBox.min.x, boundingBox.max.y - boundingBox.min.y),
        robot.getVoxels().values().stream()
            .filter(v -> (v instanceof ControllableVoxel))
            .mapToDouble(ControllableVoxel::getControlEnergy)
            .sum() / t,
        robot.getVoxels().values().stream()
            .filter(v -> (v instanceof ControllableVoxel))
            .mapToDouble(ControllableVoxel::getAreaRatioEnergy)
            .sum() / t,
        new TreeMap<>(centerTrajectory),
        new TreeMap<>(footprints),
        new TreeMap<>(masks)
    );
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

  private static double[][] randomTerrain(int n, double length, double peak, double borderHeight, Random random) {
    double[] xs = new double[n + 2];
    double[] ys = new double[n + 2];
    xs[0] = 0d;
    xs[n + 1] = length;
    ys[0] = borderHeight;
    ys[n + 1] = borderHeight;
    for (int i = 1; i < n + 1; i++) {
      xs[i] = 1 + (double) (i - 1) * (length - 2d) / (double) n;
      ys[i] = random.nextDouble() * peak;
    }
    return new double[][]{xs, ys};
  }

  public static double[][] createTerrain(String name) {
    String flat = "flat";
    String hilly = "hilly-(?<h>[0-9]+(\\.[0-9]+)?)-(?<w>[0-9]+(\\.[0-9]+)?)-(?<seed>[0-9]+)";
    String steppy = "steppy-(?<h>[0-9]+(\\.[0-9]+)?)-(?<w>[0-9]+(\\.[0-9]+)?)-(?<seed>[0-9]+)";
    if (name.matches(flat)) {
      return new double[][]{
          new double[]{0, 10, TERRAIN_LENGTH - 10, TERRAIN_LENGTH},
          new double[]{TERRAIN_BORDER_HEIGHT, 5, 5, TERRAIN_BORDER_HEIGHT}
      };
    }
    if (name.matches(hilly)) {
      double h = Double.parseDouble(paramValue(hilly, name, "h"));
      double w = Double.parseDouble(paramValue(hilly, name, "w"));
      Random random = new Random(Integer.parseInt(paramValue(hilly, name, "seed")));
      List<Double> xs = new ArrayList<>(List.of(0d, 10d));
      List<Double> ys = new ArrayList<>(List.of(TERRAIN_BORDER_HEIGHT, 0d));
      while (xs.get(xs.size() - 1) < TERRAIN_LENGTH) {
        xs.add(xs.get(xs.size() - 1) + Math.max(1d, (random.nextGaussian() * 0.25 + 1) * w));
        ys.add(ys.get(ys.size() - 1) + random.nextGaussian() * h);
      }
      xs.addAll(List.of(xs.get(xs.size() - 1) + 10, xs.get(xs.size() - 1) + 20));
      ys.addAll(List.of(0d, TERRAIN_BORDER_HEIGHT));
      return new double[][]{
          xs.stream().mapToDouble(d -> d).toArray(),
          ys.stream().mapToDouble(d -> d).toArray()
      };
    }
    if (name.matches(steppy)) {
      double h = Double.parseDouble(paramValue(steppy, name, "h"));
      double w = Double.parseDouble(paramValue(steppy, name, "w"));
      Random random = new Random(Integer.parseInt(paramValue(steppy, name, "seed")));
      List<Double> xs = new ArrayList<>(List.of(0d, 10d));
      List<Double> ys = new ArrayList<>(List.of(TERRAIN_BORDER_HEIGHT, 0d));
      while (xs.get(xs.size() - 1) < TERRAIN_LENGTH) {
        xs.add(xs.get(xs.size() - 1) + Math.max(1d, (random.nextGaussian() * 0.25 + 1) * w));
        xs.add(xs.get(xs.size() - 1) + 0.5d);
        ys.add(ys.get(ys.size() - 1));
        ys.add(ys.get(ys.size() - 1) + random.nextGaussian() * h);
      }
      xs.addAll(List.of(xs.get(xs.size() - 1) + 10, xs.get(xs.size() - 1) + 20));
      ys.addAll(List.of(0d, TERRAIN_BORDER_HEIGHT));
      return new double[][]{
          xs.stream().mapToDouble(d -> d).toArray(),
          ys.stream().mapToDouble(d -> d).toArray()
      };
    }
    throw new IllegalArgumentException(String.format("Unknown terrain name: %s", name));
  }

  private static String paramValue(String pattern, String string, String paramName) {
    Matcher matcher = Pattern.compile(pattern).matcher(string);
    if (matcher.matches()) {
      return matcher.group(paramName);
    }
    throw new IllegalStateException(String.format("Param %s not found in %s with pattern %s", paramName, string, pattern));
  }

}
