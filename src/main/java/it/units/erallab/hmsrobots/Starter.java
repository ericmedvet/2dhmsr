/*
 * Copyright (C) 2020 Eric Medvet <eric.medvet@gmail.com> (as eric)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots;

import com.google.common.collect.Lists;
import it.units.erallab.hmsrobots.controllers.CentralizedMLP;
import it.units.erallab.hmsrobots.controllers.DistributedMLP;
import it.units.erallab.hmsrobots.controllers.TimeFunctions;
import it.units.erallab.hmsrobots.objects.Robot;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.sensors.*;
import it.units.erallab.hmsrobots.tasks.Locomotion;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.validation.CantileverBending;
import it.units.erallab.hmsrobots.validation.RobotControl;
import it.units.erallab.hmsrobots.viewers.FramesFileWriter;
import it.units.erallab.hmsrobots.viewers.GridEpisodeRunner;
import it.units.erallab.hmsrobots.viewers.GridOnlineViewer;
import org.apache.commons.lang3.tuple.Pair;
import org.dyn4j.dynamics.Settings;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Starter {

  private static void sampleExecution() throws IOException {
    Locomotion locomotion = new Locomotion(
        60,
        Locomotion.createTerrain("flat"),
        Lists.newArrayList(
            Locomotion.Metric.TRAVEL_X_VELOCITY,
            Locomotion.Metric.AVG_SUM_OF_SQUARED_CONTROL_SIGNALS
        ),
        new Settings()
    );
    final Voxel.Description hardMaterial = Voxel.Description.build()
        .setConfigurable("springF", 50)
        .setConfigurable("springScaffoldings", EnumSet.allOf(Voxel.SpringScaffolding.class));
    final Voxel.Description softMaterial = Voxel.Description.build()
        .setConfigurable("springF", 5)
        .setConfigurable("springScaffoldings", EnumSet.of(
            Voxel.SpringScaffolding.SIDE_EXTERNAL,
            Voxel.SpringScaffolding.CENTRAL_CROSS));
    int w = 4;
    int h = 2;
    Robot.Description description = new Robot.Description(
        Grid.create(
            w, h,
            (x, y) -> (y == 0) ? hardMaterial : softMaterial
        ),
        new TimeFunctions(Grid.create(
            w, h,
            (x, y) -> (Double t) -> Math.sin(-2 * Math.PI * t + Math.PI * ((double) x / (double) w))
        ))
    );
    FramesFileWriter framesFileWriter = new FramesFileWriter(
        5, 5.5, 0.1, 300, 200, FramesFileWriter.Direction.VERTICAL,
        new File("/home/eric/experiments/2dhmsr/frames.v.png"),
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    );
    List<Double> result = locomotion.apply(description);
    framesFileWriter.flush();
  }

  public static void main(String[] args) throws IOException {
    final Grid<Boolean> structure = Grid.create(7, 4, (x, y) -> (x < 2) || (x >= 5) || (y > 0));
    final Voxel.Description voxelDescription = Voxel.Description.build();
    Settings settings = new Settings();
    settings.setStepFrequency(1d / 30d);
    int controlInterval = 1;
    //simple
    double f = 1d;
    Robot.Description phases1 = new Robot.Description(
        Grid.create(structure, b -> b ? voxelDescription : null),
        new TimeFunctions(Grid.create(
            structure.getW(),
            structure.getH(),
            (final Integer x, final Integer y) -> (Double t) -> Math.sin(-2 * Math.PI * f * t + Math.PI * ((double) x / (double) structure.getW()))
        )));
    Robot.Description phases2 = new Robot.Description(
        Grid.create(structure, b -> b ? voxelDescription : null),
        new TimeFunctions(Grid.create(
            structure.getW(),
            structure.getH(),
            (final Integer x, final Integer y) -> (Double t) -> Math.sin(-2 * Math.PI * f * t + 2 * Math.PI * ((double) x / (double) structure.getW()))
        )));
    //multimaterial
    final Voxel.Description hardMaterial = Voxel.Description.build()
        .setConfigurable("springF", 50)
        .setConfigurable("springScaffoldings", EnumSet.allOf(Voxel.SpringScaffolding.class));
    final Voxel.Description softMaterial = Voxel.Description.build()
        .setConfigurable("springF", 5)
        .setConfigurable("springScaffoldings", EnumSet.of(
            Voxel.SpringScaffolding.SIDE_EXTERNAL,
            Voxel.SpringScaffolding.CENTRAL_CROSS));
    int w = 4;
    int h = 2;
    Robot.Description multimat = new Robot.Description(
        Grid.create(
            w, h,
            (x, y) -> (y == 0) ? hardMaterial : softMaterial
        ),
        new TimeFunctions(Grid.create(
            w, h,
            (x, y) -> (Double t) -> Math.sin(-2 * Math.PI * t + Math.PI * ((double) x / (double) w))
        ))
    );
    //centralized mlp
    Grid<Voxel.Description> centRobotWithSensors = Grid.create(structure.getW(), structure.getH(), (x, y) -> {
      if (structure.get(x, y)) {
        Voxel.Description d = Voxel.Description.build();
        if (y > 2) {
          //d.getSensors().add(new Velocity(true, d.getSideLength(), Velocity.Axis.X, Velocity.Axis.Y));
          d.getSensors().add(new Average(new Velocity(true, 2d * d.getSideLength(), Velocity.Axis.X, Velocity.Axis.Y), 1d));
        }
        if (y == 0) {
          d.getSensors().add(new Touch());
        }
        d.getSensors().add(new AreaRatio());
        return d;
      }
      return null;
    });
    Random random = new Random(1);
    Robot.Description centralizedMlpRobot = new Robot.Description(
        centRobotWithSensors,
        new CentralizedMLP(centRobotWithSensors, new int[]{100}, t -> 1d * Math.sin(-2d * Math.PI * t * 0.5d))
    );
    double[] weights = ((CentralizedMLP) centralizedMlpRobot.getController()).getParams();
    for (int i = 0; i < weights.length; i++) {
      weights[i] = random.nextDouble() * 2d - 1d;
    }
    ((CentralizedMLP) centralizedMlpRobot.getController()).setParams(weights);
    //distributed
    Grid<Voxel.Description> distRobotWithSensors = Grid.create(structure.getW(), structure.getH(), (x, y) -> {
      if (structure.get(x, y)) {
        Voxel.Description d = Voxel.Description.build();
        if (y > 2) {
          //d.getSensors().add(new Velocity(true, d.getSideLength(), Velocity.Axis.X, Velocity.Axis.Y));
          d.getSensors().add(new Average(new Velocity(true, d.getSideLength(), Velocity.Axis.X, Velocity.Axis.Y), 0.5d));
        }
        if (y == 0) {
          d.getSensors().add(new Touch());
        }
        d.getSensors().add(new AreaRatio());
        d.getSensors().add(new Average(new Angle(), 1d));
        if ((x == structure.getW() - 1) && (y == structure.getH() - 1)) {
          d.getSensors().add(new TimeFunction(t -> 1d * Math.sin(-2d * Math.PI * t * 0.5d), -1, +1));
        }
        if (x == 0 && y == 0) {
          double rayLength = 10d;
          LinkedHashMap<Lidar.Side, Integer> sides = new LinkedHashMap<>();
          sides.put(Lidar.Side.N, 5);
          sides.put(Lidar.Side.E, 5);
          sides.put(Lidar.Side.S, 5);
          sides.put(Lidar.Side.W, 5);
          d.getSensors().add(new Lidar(sides, rayLength));
        }
        return d;
      }
      return null;
    });
    Robot.Description distributedMlpRobot = new Robot.Description(
        distRobotWithSensors,
        new DistributedMLP(distRobotWithSensors, new int[0], 1)
    );
    weights = ((DistributedMLP) distributedMlpRobot.getController()).getParams();
    for (int i = 0; i < weights.length; i++) {
      weights[i] = random.nextDouble() * 2d - 1d;
    }
    ((DistributedMLP) distributedMlpRobot.getController()).setParams(weights);
    //episode
    Locomotion locomotion = new Locomotion(
        30,
        Locomotion.createTerrain("uneven5"),
        Lists.newArrayList(Locomotion.Metric.TRAVEL_X_VELOCITY),
        settings
    );
    Grid<Pair<String, Robot.Description>> namedSolutionGrid = Grid.create(2, 2);
    namedSolutionGrid.set(0, 0, Pair.of("phase-1", phases1));
    namedSolutionGrid.set(0, 1, Pair.of("phase-2", phases2));
    //namedSolutionGrid.set(0, 0, Pair.of("centralizedMLP", centralizedMlpRobot));
    namedSolutionGrid.set(1, 0, Pair.of("distributedMLP", distributedMlpRobot));
    namedSolutionGrid.set(1, 1, Pair.of("multimat", multimat));
    ScheduledExecutorService uiExecutor = Executors.newScheduledThreadPool(4);
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    GridOnlineViewer gridOnlineViewer = new GridOnlineViewer(Grid.create(namedSolutionGrid, Pair::getLeft), uiExecutor);
    gridOnlineViewer.start(5);
    GridEpisodeRunner<Robot.Description> runner = new GridEpisodeRunner<>(
        namedSolutionGrid,
        locomotion,
        gridOnlineViewer,
        executor
    );
    runner.run();
  }

  public static void cantileverValidation() {
    CantileverBending cb = new CantileverBending(
        30d,
        Double.POSITIVE_INFINITY,
        30d,
        0.01d,
        new Settings()
    );
    Grid<Pair<String, Grid<Voxel.Description>>> namedSolutionGrid = Grid.create(1, 1);
    namedSolutionGrid.set(0, 0, Pair.of("10x4", Grid.create(10, 4, Voxel.Description.build())));
    ScheduledExecutorService uiExecutor = Executors.newScheduledThreadPool(4);
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    GridOnlineViewer gridOnlineViewer = new GridOnlineViewer(Grid.create(namedSolutionGrid, Pair::getLeft), uiExecutor);
    gridOnlineViewer.start(5);
    GridEpisodeRunner<Grid<Voxel.Description>> runner = new GridEpisodeRunner<>(
        namedSolutionGrid,
        cb,
        gridOnlineViewer,
        executor
    );
    runner.run();
  }

  public static void controlValidation() {
    RobotControl vcc = new RobotControl(
        30d,
        5d,
        1,
        new Settings()
    );
    Grid<Pair<String, Grid<Voxel.Description>>> namedSolutionGrid = Grid.create(1, 1);
    namedSolutionGrid.set(0, 0, Pair.of("10x4", Grid.create(10, 4, Voxel.Description.build())));
    ScheduledExecutorService uiExecutor = Executors.newScheduledThreadPool(4);
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    GridOnlineViewer gridOnlineViewer = new GridOnlineViewer(Grid.create(namedSolutionGrid, Pair::getLeft), uiExecutor);
    gridOnlineViewer.start(5);
    GridEpisodeRunner<Grid<Voxel.Description>> runner = new GridEpisodeRunner<>(
        namedSolutionGrid,
        vcc,
        gridOnlineViewer,
        executor
    );
    runner.run();
  }

}
