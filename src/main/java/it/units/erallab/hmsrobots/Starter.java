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

import it.units.erallab.hmsrobots.controllers.CentralizedMLP;
import it.units.erallab.hmsrobots.controllers.Controller;
import it.units.erallab.hmsrobots.util.TimeAccumulator;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.viewers.OnlineViewer;
import it.units.erallab.hmsrobots.controllers.PhaseSin;
import it.units.erallab.hmsrobots.objects.Ground;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.objects.VoxelCompound;
import it.units.erallab.hmsrobots.objects.WorldObject;
import it.units.erallab.hmsrobots.viewers.Listener;
import it.units.erallab.hmsrobots.viewers.VideoFileWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.dyn4j.dynamics.World;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Starter {

  public static void main(String[] args) throws IOException {

    gridStarter(30d, 0.01d);
    System.exit(0);

    World world = new World();
    List<WorldObject> worldObjects = new ArrayList<>();

    Ground ground = new Ground(new double[]{0, 1, 100, 400, 999, 1000}, new double[]{25, 0, 4, 10, 0, 25});
    ground.addTo(world);
    worldObjects.add(ground);

    int wormW = 10;
    int wormH = 4;
    Random random = new Random(1);
    Grid<Double> wormController = Grid.create(wormW, wormH, 0d);
    for (int x = 0; x < wormW; x++) {
      for (int y = 0; y < wormH; y++) {
        wormController.set(x, y, (double) x / (double) wormH * 1 * Math.PI);
      }
    }
    Grid<Boolean> wormShape = Grid.create(wormW, wormH, true);
    for (int x = 2; x < 8; x++) {
      for (int y = 0; y < 2; y++) {
        wormShape.set(x, y, false);
      }
    }

    Controller controller = new PhaseSin(-1d, 1d, wormController);

    EnumSet<CentralizedMLP.Input> inputs = EnumSet.of(CentralizedMLP.Input.AREA_RATIO);
    int[] innerNeurons = new int[]{20};
    int params = CentralizedMLP.countParams(wormShape, inputs, innerNeurons);
    double[] weights = new double[params];
    for (int i = 0; i < weights.length; i++) {
      weights[i] = random.nextDouble();
    }
    //controller = new CentralizedMLP(wormShape, inputs, innerNeurons, weights, t -> Math.sin(2d * Math.PI * -1d * t));
    //controller = new TimeFunction(Grid.create(wormShape.getW(), wormShape.getH(), t -> Math.sin(2d * Math.PI * 0.2d * t)));
    //controller = new TimeFunction(Grid.create(wormShape.getW(), wormShape.getH(), t -> -1d + 2 * (Math.round(t / 2d) % 2)));

    VoxelCompound vc2 = new VoxelCompound(
            50, 10,
            wormShape,
            controller,
            Voxel.Builder.create()
    );
    vc2.addTo(world);
    worldObjects.add(vc2);

    ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);

    OnlineViewer viewer = new OnlineViewer(executor);
    viewer.start();

    double dt = 0.01d;
    TimeAccumulator t = new TimeAccumulator();
    Runnable runnable = () -> {
      try {
        t.add(dt);
        vc2.control(t.getT(), dt);
        world.update(dt);
        viewer.listen(new Snapshot(t.getT(), worldObjects.stream().map(WorldObject::getSnapshot).collect(Collectors.toList())));
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
        final Listener listener = videoFileWriter.listener(x, y);
        final String name = names.get(x, y);
        futures.add(executor.submit(() -> {
          try {
            //prepare world
            World world = new World();
            List<WorldObject> worldObjects = new ArrayList<>();
            Ground ground = new Ground(new double[]{0, 1, 100, 400, 999, 1000}, new double[]{25, 0, 4, 10, 0, 25});
            ground.addTo(world);
            worldObjects.add(ground);
            //prepare controller
            Grid<Double> wormController = Grid.create(shape.getW(), shape.getH(), 0d);
            for (int lx = 0; lx < shape.getW(); lx++) {
              for (int ly = 0; ly < shape.getH(); ly++) {
                wormController.set(lx, ly, (double) lx / (double) shape.getW() * 1 * Math.PI);
              }
            }
            //prepare robot
            VoxelCompound robot = new VoxelCompound(
                    50, 10,
                    shape,
                    new PhaseSin(frequency, 1d, wormController),
                    Voxel.Builder.create()
            );
            robot.addTo(world);
            worldObjects.add(robot);
            //execute
            double t = 0;
            while (t < finalT) {
              t = t + dt;
              robot.control(t, dt);
              world.update(dt);
              listener.listen(new Snapshot(t, worldObjects.stream().map(WorldObject::getSnapshot).collect(Collectors.toList())));
            }
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
