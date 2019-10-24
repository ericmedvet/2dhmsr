/*
 * Copyright (C) 2019 Eric Medvet <eric.medvet@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.validation;

import com.google.common.base.Stopwatch;
import it.units.erallab.hmsrobots.Snapshot;
import it.units.erallab.hmsrobots.objects.Ground;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.objects.VoxelCompound;
import it.units.erallab.hmsrobots.objects.WorldObject;
import it.units.erallab.hmsrobots.objects.immutable.Point2;
import it.units.erallab.hmsrobots.util.CSVWriter;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.viewers.OnlineViewer;
import it.units.erallab.hmsrobots.viewers.SnapshotListener;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.dyn4j.dynamics.World;
import org.dyn4j.dynamics.joint.WeldJoint;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class CantileverBending implements Function<Grid<Voxel.Builder>, Map<String, Object>> {

  private final static double WALL_MARGIN = 10d;

  private final double force;
  private final double forceDuration;
  private final double maxT;
  private final double dT;
  private final double epsilon;
  private final SnapshotListener listener;

  public CantileverBending(double force, double forceDuration, double maxT, double dT, double epsilon, SnapshotListener listener) {
    this.force = force;
    this.forceDuration = forceDuration;
    this.maxT = maxT;
    this.dT = dT;
    this.epsilon = epsilon;
    this.listener = listener;
  }

  @Override
  public Map<String, Object> apply(Grid<Voxel.Builder> builderGrid) {
    List<WorldObject> worldObjects = new ArrayList<>();
    //build voxel compound
    VoxelCompound vc = new VoxelCompound(0, 0, new VoxelCompound.Description(
            Grid.create(builderGrid.getW(), builderGrid.getH(), true), null, builderGrid
    ));
    Point2[] boundingBox = vc.boundingBox();
    worldObjects.add(vc);
    //build ground
    Ground ground = new Ground(new double[]{0, 1}, new double[]{0, boundingBox[1].y - boundingBox[0].y + 2d * WALL_MARGIN});
    worldObjects.add(ground);
    //build world w/o gravity
    World world = new World();
    world.setGravity(new Vector2(0d, 0d));
    for (WorldObject worldObject : worldObjects) {
      worldObject.addTo(world);
    }
    //attach vc to ground
    vc.translate(new Vector2(-boundingBox[0].x + 1d, (boundingBox[1].y - boundingBox[0].y + 2d * WALL_MARGIN) / 2d - 1d));
    for (int y = 0; y < vc.getVoxels().getH(); y++) {
      for (int i : new int[]{0, 3}) {
        WeldJoint joint = new WeldJoint(
                ground.getBodies().get(0),
                vc.getVoxels().get(0, y).getVertexBodies()[i],
                vc.getVoxels().get(0, y).getVertexBodies()[i].getWorldCenter()
        );
        world.addJoint(joint);
      }
    }
    //prepare data
    List<Double> ys = new ArrayList<>((int) Math.round(maxT / dT));
    List<Double> realTs = new ArrayList<>((int) Math.round(maxT / dT));
    List<Double> simTs = new ArrayList<>((int) Math.round(maxT / dT));
    double y0 = vc.getVoxels().get(vc.getVoxels().getW() - 1, vc.getVoxels().getH() / 2).getCenter().y;
    //simulate
    Stopwatch stopwatch = Stopwatch.createStarted();
    double t = 0d;
    while (t < maxT) {
      //add force
      if (t <= forceDuration) {
        for (int y = 0; y < vc.getVoxels().getH(); y++) {
          for (int i : new int[]{1, 2}) {
            vc.getVoxels().get(vc.getVoxels().getW() - 1, y).getVertexBodies()[i].applyForce(new Vector2(0d, -force / 2d));
          }
        }
      }
      //do step
      t = t + dT;
      world.update(dT);
      if (listener != null) {
        Snapshot snapshot = new Snapshot(t, worldObjects.stream().map(WorldObject::getSnapshot).collect(Collectors.toList()));;
        listener.listen(snapshot);
      }
      //get position
      double y = vc.getVoxels().get(vc.getVoxels().getW() - 1, vc.getVoxels().getH() / 2).getCenter().y;
      ys.add(y - y0);
      realTs.add((double) stopwatch.elapsed(TimeUnit.MICROSECONDS) / 1000000d);
      simTs.add(t);
    }
    stopwatch.stop();
    //compute things
    int dampingIndex = ys.size() - 1;
    while (dampingIndex >= 0) {
      if (ys.get(dampingIndex) > epsilon) {
        break;
      }
      dampingIndex--;
    }
    double dampingTime = Double.POSITIVE_INFINITY;
    if (dampingIndex < ys.size() - 1) {
      dampingTime = simTs.get(dampingIndex);
    }
    double elapsedSeconds = (double) stopwatch.elapsed(TimeUnit.MICROSECONDS) / 1000000d;
    double voxelCalcsPerSecond = (double) vc.getVoxels().count(v -> v != null) * (double) dampingIndex / realTs.get(dampingIndex);
    //fill
    Map<String, Object> results = new LinkedHashMap<>();
    results.put("dampingTime", dampingTime);
    results.put("elapsedTime", elapsedSeconds);
    results.put("voxelCalcsPerSecond", voxelCalcsPerSecond);

    Map<String, List<?>> data = new LinkedHashMap<>();
    data.put("st", simTs);
    data.put("rt", realTs);
    data.put("y", ys);

    try {
      CSVWriter.write(CSVWriter.Table.create(data), new PrintStream("/home/eric/experiments/2dhmsr/damping.txt"));
    } catch (Exception e) {
      e.printStackTrace();
    }

    return results;
  }

  public static void main(String[] args) {
    //OnlineViewer viewer = new OnlineViewer(Executors.newScheduledThreadPool(2));
    //viewer.start();
    CantileverBending cb = new CantileverBending(150d, 1d, 30d, 0.01d, 0.001d, null);
    Map<String, Object> results = cb.apply(Grid.create(10, 4, Voxel.Builder.create()));
    System.out.println(results);
  }

}
