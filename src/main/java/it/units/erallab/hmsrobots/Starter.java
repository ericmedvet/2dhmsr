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
package it.units.erallab.hmsrobots;

import it.units.erallab.hmsrobots.core.controllers.CentralizedSensing;
import it.units.erallab.hmsrobots.core.controllers.DistributedSensing;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.erallab.hmsrobots.core.controllers.TimeFunctions;
import it.units.erallab.hmsrobots.core.objects.ControllableVoxel;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.core.sensors.Angle;
import it.units.erallab.hmsrobots.core.sensors.Lidar;
import it.units.erallab.hmsrobots.tasks.locomotion.Locomotion;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.SerializationUtils;
import it.units.erallab.hmsrobots.util.Utils;
import it.units.erallab.hmsrobots.viewers.FramesFileWriter;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;
import it.units.erallab.hmsrobots.viewers.GridEpisodeRunner;
import it.units.erallab.hmsrobots.viewers.GridOnlineViewer;
import it.units.erallab.hmsrobots.viewers.drawers.Ground;
import it.units.erallab.hmsrobots.viewers.drawers.SensorReading;
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
    Outcome result = locomotion.apply(robot, framesFileWriter);
    framesFileWriter.flush();
    System.out.println("Outcome: " + result);
  }

  public static void main(String[] args) {
    //bipeds();
    //rollingOne();
    //rollingBall();
    //breakingWorm();
    plainWorm();
  }

  private static void bipeds() {
    Grid<? extends SensingVoxel> body = Utils.buildSensorizingFunction("spinedTouch-f-f").apply(Utils.buildShape("biped-7x4"));
    //simple
    double f = 1d;
    Robot<ControllableVoxel> phasesRobot = new Robot<>(
        new TimeFunctions(Grid.create(
            body.getW(),
            body.getH(),
            (final Integer x, final Integer y) -> (Double t) -> Math.sin(-2 * Math.PI * f * t + Math.PI * ((double) x / (double) body.getW()))
        )),
        SerializationUtils.clone(body)
    );
    //distribute sensing
    Random random = new Random();
    DistributedSensing distributedSensing = new DistributedSensing(body, 1);
    for (Grid.Entry<? extends SensingVoxel> entry : body) {
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
        SerializationUtils.clone(body)
    );
    //centralized sensing
    CentralizedSensing centralizedSensing = new CentralizedSensing(body);
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
        SerializationUtils.clone(body)
    );
    //episode
    Locomotion locomotion = new Locomotion(
        60,
        Locomotion.createTerrain("flat"),
        new Settings()
    );
    Grid<Pair<String, Robot<?>>> namedSolutionGrid = Grid.create(1, 3);
    namedSolutionGrid.set(0, 0, Pair.of("dist-hetero", distHetero));
    namedSolutionGrid.set(0, 1, Pair.of("centralized", centralized));
    namedSolutionGrid.set(0, 2, Pair.of("phasesRobot", phasesRobot));
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

  private static void breakingWorm() {
    Grid<? extends SensingVoxel> body = Utils.buildSensorizingFunction("spinedTouch-f-t").apply(Utils.buildShape("worm-4x3"));
    double f = 1d;
    Robot<ControllableVoxel> unbreakableRobot = new Robot<>(
        new TimeFunctions(Grid.create(
            body.getW(),
            body.getH(),
            (final Integer x, final Integer y) -> (Double t) -> Math.sin(-2 * Math.PI * f * t + Math.PI * ((double) x / (double) body.getW()))
        )),
        SerializationUtils.clone(body)
    );
    Robot<?> breakableRobot = Utils.buildRobotTransformation(
        "breakable-area-1000/500-3/0.5-0"
    ).apply(SerializationUtils.clone(unbreakableRobot));
    //episode
    Locomotion locomotion = new Locomotion(
        60,
        Locomotion.createTerrain("hilly-0.5-5-0"),
        new Settings()
    );
    Grid<Pair<String, Robot<?>>> namedSolutionGrid = Grid.create(1, 2);
    namedSolutionGrid.set(0, 0, Pair.of("unbreakable", unbreakableRobot));
    namedSolutionGrid.set(0, 1, Pair.of("breakable", breakableRobot));
    ScheduledExecutorService uiExecutor = Executors.newScheduledThreadPool(4);
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    GridOnlineViewer gridOnlineViewer = new GridOnlineViewer(
        Grid.create(namedSolutionGrid, Pair::getLeft),
        uiExecutor,
        GraphicsDrawer.build().setConfigurable("drawers", List.of(
            it.units.erallab.hmsrobots.viewers.drawers.Robot.build(),
            it.units.erallab.hmsrobots.viewers.drawers.Voxel.build(),
            SensorReading.build(),
            Ground.build()
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

  private static void plainWorm() {
    Grid<? extends SensingVoxel> body = Utils.buildSensorizingFunction("uniform").apply(Utils.buildShape("worm-10x3"));
    double f = 1d;
    Robot<ControllableVoxel> robot = new Robot<>(
        new TimeFunctions(Grid.create(
            body.getW(),
            body.getH(),
            (final Integer x, final Integer y) -> (Double t) -> Math.sin(
                -2 * Math.PI * f * t + 2 * Math.PI * ((double) x / (double) body.getW()) + Math.PI * ((double) y / (double) body.getH())
            )
        )),
        SerializationUtils.clone(body)
    );
    //episode
    Locomotion locomotion = new Locomotion(
        30,
        Locomotion.createTerrain("flat"),
        new Settings()
    );
    Grid<Pair<String, Robot<?>>> namedSolutionGrid = Grid.create(1, 1);
    namedSolutionGrid.set(0, 0, Pair.of("unbreakable", robot));
    ScheduledExecutorService uiExecutor = Executors.newScheduledThreadPool(4);
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    GridOnlineViewer gridOnlineViewer = new GridOnlineViewer(
        Grid.create(namedSolutionGrid, Pair::getLeft),
        uiExecutor,
        GraphicsDrawer.build().setConfigurable("drawers", List.of(
            it.units.erallab.hmsrobots.viewers.drawers.Robot.build(),
            it.units.erallab.hmsrobots.viewers.drawers.Voxel.build(),
            SensorReading.build(),
            Ground.build()
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

  private static void rollingOne() {
    //one voxel robot
    Grid<SensingVoxel> oneBody = Grid.create(1, 1, new SensingVoxel(List.of(
        new Angle(),
        new Lidar(10, Map.of(Lidar.Side.E, 4))
    )));
    Robot<SensingVoxel> one = new Robot<>(
        new CentralizedSensing(oneBody, in -> new double[]{0d}),
        oneBody
    );
    //episode
    Locomotion locomotion = new Locomotion(
        60,
        new double[][]{new double[]{0, 10, 100, 1000, 1010}, new double[]{100, 100, 10, 0, 100}},
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

  private static void rollingBall() {
    Random random = new Random();
    Grid<? extends SensingVoxel> body = Utils.buildSensorizingFunction("uniform").apply(Utils.buildShape("ball-7"));
    //centralized sensing
    CentralizedSensing centralizedSensing = new CentralizedSensing(body);
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
    Robot<SensingVoxel> robot = new Robot<>(
        centralizedSensing,
        SerializationUtils.clone(body)
    );
    //episode
    Locomotion locomotion = new Locomotion(
        60,
        new double[][]{new double[]{0, 10, 100, 1000, 1010}, new double[]{100, 75, 10, 0, 100}},
        new Settings()
    );
    Grid<Pair<String, Robot<?>>> namedSolutionGrid = Grid.create(1, 1);
    namedSolutionGrid.set(0, 0, Pair.of("ball", robot));
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
