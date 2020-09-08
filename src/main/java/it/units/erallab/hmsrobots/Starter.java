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
import it.units.erallab.hmsrobots.core.controllers.CentralizedSensing;
import it.units.erallab.hmsrobots.core.controllers.DistributedSensing;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.erallab.hmsrobots.core.controllers.TimeFunctions;
import it.units.erallab.hmsrobots.core.objects.*;
import it.units.erallab.hmsrobots.core.sensors.*;
import it.units.erallab.hmsrobots.tasks.Locomotion;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.viewers.FramesFileWriter;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;
import it.units.erallab.hmsrobots.viewers.GridEpisodeRunner;
import it.units.erallab.hmsrobots.viewers.GridOnlineViewer;
import it.units.erallab.hmsrobots.viewers.drawers.SensorReading;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.dyn4j.dynamics.Settings;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.IntStream;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Starter {

  private static void sampleExecution() throws IOException {
    final Locomotion locomotion = new Locomotion(
        20,
        Locomotion.createTerrain("flat"),
        Lists.newArrayList(
            Locomotion.Metric.TRAVEL_X_VELOCITY,
            Locomotion.Metric.RELATIVE_CONTROL_POWER
        ),
        new Settings()
    );
    final ControllableVoxel hardMaterialVoxel = new ControllableVoxel(
        Voxel.SIDE_LENGTH,
        Voxel.MASS_SIDE_LENGTH_RATIO,
        50d,
        Voxel.SPRING_D,
        Voxel.MASS_LINEAR_DAMPING,
        Voxel.MASS_ANGULAR_DAMPING,
        Voxel.FRICTION,
        Voxel.RESTITUTION,
        Voxel.MASS,
        Voxel.LIMIT_CONTRACTION_FLAG,
        Voxel.MASS_COLLISION_FLAG,
        Voxel.AREA_RATIO_MAX_DELTA,
        Voxel.SPRING_SCAFFOLDINGS,
        ControllableVoxel.MAX_FORCE,
        ControllableVoxel.ForceMethod.DISTANCE
    );
    final ControllableVoxel softMaterialVoxel = new ControllableVoxel(
        Voxel.SIDE_LENGTH,
        Voxel.MASS_SIDE_LENGTH_RATIO,
        5d,
        Voxel.SPRING_D,
        Voxel.MASS_LINEAR_DAMPING,
        Voxel.MASS_ANGULAR_DAMPING,
        Voxel.FRICTION,
        Voxel.RESTITUTION,
        Voxel.MASS,
        Voxel.LIMIT_CONTRACTION_FLAG,
        Voxel.MASS_COLLISION_FLAG,
        Voxel.AREA_RATIO_MAX_DELTA,
        EnumSet.of(Voxel.SpringScaffolding.SIDE_EXTERNAL, Voxel.SpringScaffolding.CENTRAL_CROSS),
        ControllableVoxel.MAX_FORCE,
        ControllableVoxel.ForceMethod.DISTANCE
    );
    int w = 20;
    int h = 5;
    Robot robot = new Robot(
        new TimeFunctions(Grid.create(
            w, h,
            (x, y) -> (Double t) -> Math.sin(-2 * Math.PI * t + Math.PI * ((double) x / (double) w))
        )),
        Grid.create(
            w, h,
            (x, y) -> (y == 0) ? SerializationUtils.clone(hardMaterialVoxel) : SerializationUtils.clone(softMaterialVoxel)
        )
    );
    FramesFileWriter framesFileWriter = new FramesFileWriter(
        5, 5.5, 0.1, 300, 200, FramesFileWriter.Direction.HORIZONTAL,
        new File("/home/eric/experiments/2dhmsr/frames.v.png"),
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    );
    List<Double> result = locomotion.apply(robot, framesFileWriter);
    framesFileWriter.flush();
    System.out.println("Outcome: " + result);
  }

  public static void main(String[] args) throws IOException {

    rollingOne();
  }

  private static void bipeds() {

    final Grid<Boolean> structure = Grid.create(7, 4, (x, y) -> (x < 2) || (x >= 5) || (y > 0));
    Settings settings = new Settings();
    settings.setStepFrequency(1d / 30d);
    //simple
    double f = 1d;
    Robot<ControllableVoxel> phasesRobot = new Robot<>(
        new TimeFunctions(Grid.create(
            structure.getW(),
            structure.getH(),
            (final Integer x, final Integer y) -> (Double t) -> Math.sin(-2 * Math.PI * f * t + Math.PI * ((double) x / (double) structure.getW()))
        )),
        Grid.create(structure, b -> b ? new ControllableVoxel() : null)
    );
    //sensing
    Random random = new Random();
    Grid<SensingVoxel> voxels = Grid.create(structure.getW(), structure.getH(), (x, y) -> {
      if (structure.get(x, y)) {
        if (x == 0 && y == 0) {
          return new SensingVoxel(List.of(
              new TimeFunction(t -> Math.sin(2 * Math.PI * t), -1d, 1d)
          ));
        }
        if (y > 2) {
          return new SensingVoxel(List.of(
              new Velocity(true, 3d, Velocity.Axis.X, Velocity.Axis.Y),
              new Average(new Velocity(true, 3d, Velocity.Axis.X, Velocity.Axis.Y), 1d)
          ));
        }
        if (y == 0) {
          return new SensingVoxel(List.of(
              new Average(new Touch(), 1d)
          ));
        }
        return new SensingVoxel(List.of(
            new AreaRatio(),
            new ControlPower(settings.getStepFrequency())
        ));
      }
      return null;
    });
    DistributedSensing distributedSensing = new DistributedSensing(SerializationUtils.clone(voxels), 1);
    for (Grid.Entry<SensingVoxel> entry : voxels) {
      MultiLayerPerceptron mlp = new MultiLayerPerceptron(
          MultiLayerPerceptron.ActivationFunction.TANH,
          distributedSensing.nOfInputs(entry.getX(), entry.getY()),
          new int[0],
          distributedSensing.nOfOutputs(entry.getX(), entry.getY())
      );
      double[] ws = mlp.getParams();
      IntStream.range(0, ws.length).forEach(i -> ws[i] = random.nextGaussian());
      mlp.setParams(ws);
      distributedSensing.getFunctions().set(entry.getX(), entry.getY(), mlp);
    }
    Robot<SensingVoxel> distHetero = new Robot<>(
        distributedSensing,
        SerializationUtils.clone(voxels)
    );
    CentralizedSensing centralizedSensing = new CentralizedSensing(SerializationUtils.clone(voxels));
    MultiLayerPerceptron mlp = new MultiLayerPerceptron(
        MultiLayerPerceptron.ActivationFunction.TANH,
        centralizedSensing.nOfInputs(),
        new int[0],
        centralizedSensing.nOfOutputs()
    );
    double[] ws = mlp.getParams();
    IntStream.range(0, ws.length).forEach(i -> ws[i] = random.nextGaussian());
    mlp.setParams(ws);
    centralizedSensing.setFunction(mlp);
    Robot<SensingVoxel> centralized = new Robot<>(
        centralizedSensing,
        Grid.create(voxels, v -> v == null ? null : new BreakableVoxel(
            v.getSensors(),
            random,
            Map.of(
                BreakableVoxel.ComponentType.ACTUATOR, Set.of(BreakableVoxel.MalfunctionType.FROZEN),
                BreakableVoxel.ComponentType.SENSORS, Set.of(BreakableVoxel.MalfunctionType.ZERO)
            ),
            Map.of(BreakableVoxel.MalfunctionTrigger.AREA, 10d)
        ))
    );
    //episode
    Locomotion locomotion = new Locomotion(
        60,
        Locomotion.createTerrain("hilly-1-5-2"),
        //Locomotion.createTerrain("flat"),
        Lists.newArrayList(Locomotion.Metric.TRAVEL_X_VELOCITY),
        settings
    );
    Grid<Pair<String, Robot<?>>> namedSolutionGrid = Grid.create(1, 1);
    namedSolutionGrid.set(0, 0, Pair.of("dist-hetero", distHetero));
    /*namedSolutionGrid.set(0, 1, Pair.of("centralized", centralized));
    namedSolutionGrid.set(0, 2, Pair.of("phasesRobot", phasesRobot));*/
    ScheduledExecutorService uiExecutor = Executors.newScheduledThreadPool(4);
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    GridOnlineViewer gridOnlineViewer = new GridOnlineViewer(Grid.create(namedSolutionGrid, Pair::getLeft), uiExecutor);
    gridOnlineViewer.start(5);
    GridEpisodeRunner<Robot<?>> runner = new GridEpisodeRunner<>(
        namedSolutionGrid,
        locomotion,
        gridOnlineViewer,
        executor
    );
    runner.run();
  }

  private static void rollingOne() {
    //one voxel robot
    Grid<SensingVoxel> oneBody = Grid.create(1, 1, new SensingVoxel(List.of(
        new Angle(),
        new Lidar(10, Map.of(Lidar.Side.E, 5))
    )));
    Robot<SensingVoxel> one = new Robot<>(
        new CentralizedSensing(oneBody, in -> new double[]{0d}),
        oneBody
    );
    //episode
    Locomotion locomotion = new Locomotion(
        60,
        new double[][]{new double[]{0, 10, 100, 1000, 1010}, new double[]{100, 100, 10, 0, 100}},
        Lists.newArrayList(Locomotion.Metric.TRAVEL_X_VELOCITY),
        new Settings()
    );
    Grid<Pair<String, Robot<?>>> namedSolutionGrid = Grid.create(1, 1);
    namedSolutionGrid.set(0, 0, Pair.of("one", one));
    ScheduledExecutorService uiExecutor = Executors.newScheduledThreadPool(4);
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    GridOnlineViewer gridOnlineViewer = new GridOnlineViewer(
        Grid.create(namedSolutionGrid, Pair::getLeft),
        uiExecutor,
        GraphicsDrawer.build().setConfigurable("drawers", List.of(
            it.units.erallab.hmsrobots.viewers.drawers.Ground.build(),
            it.units.erallab.hmsrobots.viewers.drawers.Robot.build(),
            it.units.erallab.hmsrobots.viewers.drawers.Voxel.build(),
            SensorReading.build(),
            it.units.erallab.hmsrobots.viewers.drawers.Lidar.build(),
            it.units.erallab.hmsrobots.viewers.drawers.Angle.build()
        ))
    );
    gridOnlineViewer.start(5);
    GridEpisodeRunner<Robot<?>> runner = new GridEpisodeRunner<>(
        namedSolutionGrid,
        locomotion,
        gridOnlineViewer,
        executor
    );
    runner.run();
  }

}
