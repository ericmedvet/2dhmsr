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
package it.units.erallab.hmsrobots;

import it.units.erallab.hmsrobots.controllers.*;
import it.units.erallab.hmsrobots.objects.Ground;
import it.units.erallab.hmsrobots.problems.Locomotion;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.viewers.OnlineViewer;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.objects.VoxelCompound;
import it.units.erallab.hmsrobots.objects.WorldObject;
import it.units.erallab.hmsrobots.util.SerializableFunction;
import it.units.erallab.hmsrobots.util.TimeAccumulator;
import it.units.erallab.hmsrobots.viewers.VideoFileWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import it.units.erallab.hmsrobots.viewers.SnapshotListener;
import java.util.stream.Collectors;
import org.dyn4j.dynamics.World;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Starter {
  
  public static void main(String[] args) throws IOException {
    
    List<WorldObject> worldObjects = new ArrayList<>();
    VoxelCompound vc1 = new VoxelCompound(110, 10, new VoxelCompound.Description(
            Grid.create(5, 5, true),
            new TimeFunction(Grid.create(5, 5, t -> {return Math.signum(Math.sin(2d*Math.PI*t*0.5d));})),
            Voxel.Builder.create().forceMethod(Voxel.ForceMethod.DISTANCE)
    ));
    VoxelCompound vc2 = new VoxelCompound(140, 10, new VoxelCompound.Description(
            Grid.create(2, 10, true),
            null,
            Voxel.Builder.create().mass(25d).springScaffoldings(EnumSet.of(Voxel.SpringScaffolding.SIDE_EXTERNAL, Voxel.SpringScaffolding.CENTRAL_CROSS))
    ));
    Ground ground = new Ground(new double[]{0, 300}, new double[]{0, 0});
    //worldObjects.add(vc1);
    worldObjects.add(vc2);
    worldObjects.add(ground);
    World world = new World();
    worldObjects.forEach((worldObject) -> {
      worldObject.addTo(world);
    });
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
    OnlineViewer viewer = new OnlineViewer(executor);
    viewer.start();
    final double dt = 0.01d;
    final TimeAccumulator t = new TimeAccumulator();
    Runnable runnable = () -> {
      try {
        t.add(dt);
        worldObjects.forEach((worldObject) -> {
          if (worldObject instanceof VoxelCompound) {
            ((VoxelCompound) worldObject).control(t.getT(), dt);
          }          
        });
        world.update(dt);
        Snapshot snapshot = snapshot = new Snapshot(t.getT(), worldObjects.stream().map(WorldObject::getSnapshot).collect(Collectors.toList()));;
        viewer.listen(snapshot);
      } catch (Throwable ex) {
        ex.printStackTrace();
        System.exit(0);
      }
    };
    executor.scheduleAtFixedRate(runnable, 0, Math.round(dt * 1000d / 1.1d), TimeUnit.MILLISECONDS);
    
  }
  
  private static void gridStarter(double finalT, double dt) throws IOException {
    final List<Grid<Boolean>> shapes = new ArrayList<>();
    shapes.add(Grid.create(10, 5, true));
    shapes.add(Grid.create(6, 3, true));
    shapes.add(Grid.create(4, 1, true));
    final List<Double> frequencies = new ArrayList<>();
    frequencies.add(-1d);
    frequencies.add(-.5d);
    Grid<String> names = Grid.create(shapes.size(), frequencies.size());
    for (int x = 0; x < names.getW(); x++) {
      for (int y = 0; y < names.getH(); y++) {
        names.set(x, y, String.format("shape=%d, freq=%f", x, frequencies.get(y)));
      }
    }
    ExecutorService executor = Executors.newFixedThreadPool(3);
    VideoFileWriter videoFileWriter = new VideoFileWriter(
            new File("/home/eric/experiments/video-grid.mp4"),
            names,
            executor
    );
    List<Future<String>> futures = new ArrayList<>();
    for (int x = 0; x < names.getW(); x++) {
      for (int y = 0; y < names.getH(); y++) {
        final Grid<Boolean> shape = shapes.get(x);
        final double frequency = frequencies.get(y);
        final SnapshotListener listener = videoFileWriter.listener(x, y);
        final String name = names.get(x, y);
        futures.add(executor.submit(() -> {
          try {
            //prepare controller
            Grid<Double> wormController = Grid.create(shape.getW(), shape.getH(), 0d);
            for (int lx = 0; lx < shape.getW(); lx++) {
              for (int ly = 0; ly < shape.getH(); ly++) {
                wormController.set(lx, ly, (double) lx / (double) shape.getW() * 1 * Math.PI);
              }
            }
            //prepare
            Locomotion locomotion = new Locomotion(finalT, new double[][]{new double[]{0, 1, 999, 1000}, new double[]{50, 0, 0, 50}}, new Locomotion.Metric[]{Locomotion.Metric.TRAVEL_X_VELOCITY});
            //execute
            locomotion.init(new VoxelCompound.Description(shape, new PhaseSin(frequency, 1d, wormController), Voxel.Builder.create()));
            while (!locomotion.isDone()) {
              Snapshot snapshot = locomotion.step(dt, true);
              listener.listen(snapshot);
            }
            double[] metrics = locomotion.getMetrics();
            System.out.printf("Result is %s%n", Arrays.toString(metrics));
          } catch (Throwable t) {
            t.printStackTrace();
          }
          return name;
        }));
      }
    }
    //wait for end
    for (Future<String> future : futures) {
      try {
        System.out.printf("World %s done%n", future.get());
      } catch (InterruptedException | ExecutionException ex) {
        Logger.getLogger(Starter.class.getName()).log(Level.SEVERE, String.format("Exception: %s", ex), ex);
      }
    }
    videoFileWriter.flush();
  }
  
}
