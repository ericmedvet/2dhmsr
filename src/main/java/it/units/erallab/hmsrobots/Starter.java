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

import com.google.common.collect.Lists;
import it.units.erallab.hmsrobots.controllers.*;
import it.units.erallab.hmsrobots.objects.Ground;
import it.units.erallab.hmsrobots.problems.Locomotion;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.viewers.OnlineViewer;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.objects.VoxelCompound;
import it.units.erallab.hmsrobots.objects.WorldObject;
import it.units.erallab.hmsrobots.objects.immutable.Snapshot;
import it.units.erallab.hmsrobots.util.TimeAccumulator;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import it.units.erallab.hmsrobots.viewers.SnapshotListener;
import java.util.stream.Collectors;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.dynamics.World;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Starter {

  public static void main(String[] args) throws IOException {
    List<WorldObject> worldObjects = new ArrayList<>();
    Grid<Boolean> structure = Grid.create(7, 5, (x, y) -> (x < 2) || (x >= 5) || (y > 2));
    //simple
    VoxelCompound vc1 = new VoxelCompound(10, 10, new VoxelCompound.Description(
            Grid.create(structure, b -> b?Voxel.Builder.create().springF(5d):null),
            new TimeFunction(Grid.create(structure.getW(), structure.getH(), t -> {
              return (Math.sin(2d * Math.PI * t * 1d));
            }))
    ));
    //centralized mlp
    Grid<List<Voxel.Sensor>> sensorGrid = Grid.create(structure.getW(), structure.getH(),
            (x, y) -> {
              List<Voxel.Sensor> sensors = new ArrayList<>();
              if (y > 2) {
                sensors.add(Voxel.Sensor.Y_ROT_VELOCITY);
              }
              if (y == 0) {
                sensors.add(Voxel.Sensor.AREA_RATIO);
              }
              return sensors;
            }
    );
    int[] innerNeurons = new int[]{10};
    int nOfWeights = CentralizedMLP.countParams(structure, sensorGrid, innerNeurons);
    double[] weights = new double[nOfWeights];
    Random random = new Random();
    for (int i = 0; i < weights.length; i++) {
      weights[i] = random.nextDouble();
    }
    VoxelCompound vc2 = new VoxelCompound(10, 10, new VoxelCompound.Description(
            Grid.create(structure, b -> b?Voxel.Builder.create().forceMethod(Voxel.ForceMethod.DISTANCE):null),
            new CentralizedMLP(structure, sensorGrid, innerNeurons, weights, t -> Math.sin(2d * Math.PI * t * 0.5d))
    ));
    //distributed mlp
    innerNeurons = new int[0];
    nOfWeights = DistributedMLP.countParams(structure, sensorGrid, 1, innerNeurons);
    weights = new double[nOfWeights];
    for (int i = 0; i < weights.length; i++) {
      weights[i] = random.nextDouble();
    }
    VoxelCompound vc3 = new VoxelCompound(10, 10, new VoxelCompound.Description(
            Grid.create(structure, b -> b?Voxel.Builder.create().massCollisionFlag(true):null),
            new DistributedMLP(
                    structure,
                    Grid.create(structure.getW(), structure.getH(), (x, y) -> {
                      if (x == 3) {
                        return t -> Math.sin(2d * Math.PI * t * 0.5d);
                      } else {
                        return t -> 0d;
                      }
                    }),
                    sensorGrid,
                    1,
                    innerNeurons,
                    weights
            )
    ));
    //world
    Ground ground = new Ground(new double[]{0, 1, 2999, 3000}, new double[]{50, 0, 0, 50});
    worldObjects.add(vc1);
    //vc2.translate(new Vector2(25, 0));
    //worldObjects.add(vc2);
    //vc3.translate(new Vector2(50, 0));
    //worldObjects.add(vc3);
    worldObjects.add(ground);
    World world = new World();
    worldObjects.forEach((worldObject) -> {
      worldObject.addTo(world);
    });
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
    OnlineViewer viewer = new OnlineViewer(executor);
    viewer.start();
    final double dt = world.getSettings().getStepFrequency();
    final TimeAccumulator t = new TimeAccumulator();
    Runnable runnable = () -> {
      try {
        t.add(dt);
        worldObjects.forEach((worldObject) -> {
          if (worldObject instanceof VoxelCompound) {
            ((VoxelCompound) worldObject).control(t.getT(), dt);
          }
        });
        world.step(1);
        Snapshot snapshot = new Snapshot(t.getT(), worldObjects.stream().map(WorldObject::getSnapshot).collect(Collectors.toList()));;
        viewer.listen(snapshot);
      } catch (Throwable ex) {
        ex.printStackTrace();
        System.exit(0);
      }
    };
    executor.scheduleAtFixedRate(runnable, 0, Math.round(dt * 1000d / 1.1d), TimeUnit.MILLISECONDS);

  }

  private static void gridStarter(double finalT) throws IOException {
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
            800, 600, 25,
            new File("/home/eric/experiments/video-grid.mp4"),
            names,
            executor,
            GraphicsDrawer.RenderingDirectives.create()
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
            Locomotion locomotion = new Locomotion(
                    finalT,
                    new double[][]{new double[]{0, 1, 999, 1000}, new double[]{50, 0, 0, 50}},
                    Lists.newArrayList(Locomotion.Metric.TRAVEL_X_VELOCITY),
                    2,
                    new Settings()
            );
            //execute
            List<Double> results = locomotion.apply(new VoxelCompound.Description(
                    Grid.create(shape.getW(), shape.getH(), Voxel.Builder.create()),
                    new PhaseSin(frequency, 1d, wormController)
            ), listener);
            System.out.printf("Result is %s%n", results);
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
