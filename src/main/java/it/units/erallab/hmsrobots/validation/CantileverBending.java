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
import it.units.erallab.hmsrobots.objects.immutable.Snapshot;
import it.units.erallab.hmsrobots.objects.Ground;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.objects.VoxelCompound;
import it.units.erallab.hmsrobots.objects.WorldObject;
import it.units.erallab.hmsrobots.objects.immutable.Point2;
import it.units.erallab.hmsrobots.problems.AbstractEpisode;
import it.units.erallab.hmsrobots.problems.Episode;
import it.units.erallab.hmsrobots.util.CSVWriter;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.viewers.OnlineViewer;
import it.units.erallab.hmsrobots.viewers.SnapshotListener;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.dyn4j.dynamics.Settings;
import org.dyn4j.dynamics.World;
import org.dyn4j.dynamics.joint.WeldJoint;
import org.dyn4j.geometry.Vector2;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class CantileverBending extends AbstractEpisode<Grid<Voxel.Builder>> {

  private final static double WALL_MARGIN = 10d;

  private final double force;
  private final double forceDuration;
  private final double maxT;
  private final double epsilon;

  public CantileverBending(double force, double forceDuration, double maxT, double epsilon, Settings settings, SnapshotListener listener) {
    super(listener, settings);
    this.force = force;
    this.forceDuration = forceDuration;
    this.maxT = maxT;
    this.epsilon = epsilon;
  }

  @Override
  public Map<String, Double> apply(Grid<Voxel.Builder> builderGrid) {
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
    world.setSettings(settings);
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
    List<Double> ys = new ArrayList<>((int) Math.round(maxT / settings.getStepFrequency()));
    List<Double> realTs = new ArrayList<>((int) Math.round(maxT / settings.getStepFrequency()));
    List<Double> simTs = new ArrayList<>((int) Math.round(maxT / settings.getStepFrequency()));
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
      t = t + settings.getStepFrequency();
      world.step(1);
      if (listener != null) {
        Snapshot snapshot = new Snapshot(t, worldObjects.stream().map(WorldObject::getSnapshot).collect(Collectors.toList()));
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
    int dampingIndex = ys.size() - 2;
    while (dampingIndex > 0) {
      if (Math.abs(ys.get(dampingIndex) - ys.get(dampingIndex + 1)) > epsilon) {
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
    Map<String, Double> results = new LinkedHashMap<>();
    results.put("dampingSimTime", dampingTime);
    results.put("elapsedRealTime", elapsedSeconds);
    results.put("dampingRealTime", realTs.get(dampingIndex));
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
    OnlineViewer viewer = new OnlineViewer(Executors.newScheduledThreadPool(2));
    viewer.start();
    Settings settings = new Settings();
    settings.setStepFrequency(0.015);
    CantileverBending cb = new CantileverBending(50d, Double.POSITIVE_INFINITY, 30d, 0.01d, settings, viewer);
    Map<String, Double> results = cb.apply(Grid.create(10, 4, Voxel.Builder.create()));
    System.out.println(results);
  }

}
