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
import com.google.common.collect.Maps;
import it.units.erallab.hmsrobots.core.controllers.*;
import it.units.erallab.hmsrobots.core.objects.*;
import it.units.erallab.hmsrobots.core.sensors.*;
import it.units.erallab.hmsrobots.tasks.Locomotion;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.viewers.FramesFileWriter;
import it.units.erallab.hmsrobots.viewers.GridEpisodeRunner;
import it.units.erallab.hmsrobots.viewers.GridOnlineViewer;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.dyn4j.dynamics.Settings;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
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
    final Grid<Boolean> structure = Grid.create(7, 4, (x, y) -> (x < 2) || (x >= 5) || (y > 0));
    Settings settings = new Settings();
    settings.setStepFrequency(1d / 30d);
    int controlInterval = 1;
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
    //multimaterial
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
    Robot<ControllableVoxel> multimaterial = new Robot<>(
        new TimeFunctions(Grid.create(
            structure.getW(),
            structure.getH(),
            (x, y) -> (Double t) -> Math.sin(-2 * Math.PI * t + Math.PI * ((double) x / (double) structure.getW()))
        )),
        Grid.create(
            structure.getW(),
            structure.getH(),
            (x, y) -> (y == 0) ? SerializationUtils.clone(hardMaterialVoxel) : SerializationUtils.clone(softMaterialVoxel)
        )
    );
    //centralized mlp
    Grid<SensingVoxel> sensingVoxels = Grid.create(structure.getW(), structure.getH(), (x, y) -> {
      if (structure.get(x, y)) {
        if (y > 2) {
          return new SensingVoxel(Arrays.asList(
              new Velocity(true, 3d, Velocity.Axis.X, Velocity.Axis.Y),
              new Average(new Velocity(true, 3d, Velocity.Axis.X, Velocity.Axis.Y), 1d)
          ));
        }
        if (y == 0) {
          return new SensingVoxel(Arrays.asList(
              new Average(new Touch(), 1d)
          ));
        }
        return new SensingVoxel(Arrays.asList(
            new AreaRatio(),
            new ControlPower(settings.getStepFrequency())
        ));
      }
      return null;
    });
    Random random = new Random(1);
    Robot<SensingVoxel> centralizedMlpRobot = new Robot(
        new CentralizedMLP(sensingVoxels, new int[]{100}, t -> 1d * Math.sin(-2d * Math.PI * t * 0.5d)),
        SerializationUtils.clone(sensingVoxels)
    );
    double[] weights = ((CentralizedMLP) centralizedMlpRobot.getController()).getParams();
    for (int i = 0; i < weights.length; i++) {
      weights[i] = random.nextDouble() * 2d - 1d;
    }
    ((CentralizedMLP) centralizedMlpRobot.getController()).setParams(weights);
    //distributed
    DistributedMLP distributedMLP = new DistributedMLP(sensingVoxels, new int[0], 2);
    weights = distributedMLP.getParams();
    for (int i = 0; i < weights.length; i++) {
      weights[i] = random.nextDouble() * 2d - 1d;
    }
    distributedMLP.setParams(weights);
    Robot<SensingVoxel> distributedMlpRobot1 = new Robot<>(
        new Discontinuous<>(distributedMLP, 0d, 1d / 5d, Discontinuous.Type.IMPULSE),
        SerializationUtils.clone(sensingVoxels)
    );
    Robot<SensingVoxel> distributedMlpRobot2 = new Robot<>(
        new Discontinuous<>(SerializationUtils.clone(distributedMLP), 0d, 1d / 5d, Discontinuous.Type.STEP),
        SerializationUtils.clone(sensingVoxels)
    );
    //episode
    Locomotion locomotion = new Locomotion(
        60,
        Locomotion.createTerrain("uneven5"),
        Lists.newArrayList(Locomotion.Metric.TRAVEL_X_VELOCITY),
        settings
    );
    Grid<Pair<String, Robot<?>>> namedSolutionGrid = Grid.create(1, 2);
    namedSolutionGrid.set(0, 0, Pair.of("phases", phasesRobot));
    namedSolutionGrid.set(0, 1, Pair.of("phases-breakable", new Robot(
        new SequentialBreakingController(
            SerializationUtils.clone(phasesRobot.getController()),
            2d,
            random,
            Maps.asMap(
                EnumSet.of(BreakableVoxel.ComponentType.ACTUATOR, BreakableVoxel.ComponentType.STRUCTURE),
                c -> EnumSet.of(BreakableVoxel.MalfunctionType.FROZEN)
            )
        ),
        Grid.create(distributedMlpRobot2.getVoxels(), v -> v == null ? null : new BreakableVoxel(SerializationUtils.clone(v).getSensors(), random))
    )));
/*
    namedSolutionGrid.set(0, 0, Pair.of("dmlp", distributedMlpRobot2));
    namedSolutionGrid.set(0, 1, Pair.of("dmlp-breakable", new Robot(
        new SequentialBreakingController(
            SerializationUtils.clone(distributedMlpRobot2.getController()),
            2d,
            random,
            Maps.asMap(
                EnumSet.of(BreakableVoxel.ComponentType.ACTUATOR, BreakableVoxel.ComponentType.SENSORS),
                c -> EnumSet.of(BreakableVoxel.MalfunctionType.ZERO, BreakableVoxel.MalfunctionType.FROZEN)
            )
        ),
        Grid.create(distributedMlpRobot2.getVoxels(), v -> v == null ? null : new BreakableVoxel(SerializationUtils.clone(v).getSensors(), random))
    )));
*/
    /*namedSolutionGrid.set(0, 0, Pair.of("phases", phasesRobot));
    namedSolutionGrid.set(1, 0, Pair.of("centralized", centralizedMlpRobot));
    namedSolutionGrid.set(0, 1, Pair.of("distributedMLP-impulse", distributedMlpRobot1));
    namedSolutionGrid.set(1, 1, Pair.of("distributedMLP-step", distributedMlpRobot2));
    namedSolutionGrid.set(0, 2, Pair.of("multimaterial", multimaterial));
    namedSolutionGrid.set(1, 2, Pair.of("multimaterial", SerializationUtils.clone(multimaterial)));*/
    ScheduledExecutorService uiExecutor = Executors.newScheduledThreadPool(4);
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    GridOnlineViewer gridOnlineViewer = new GridOnlineViewer(Grid.create(namedSolutionGrid, Pair::getLeft), uiExecutor);
    gridOnlineViewer.start(5);
    GridEpisodeRunner<Robot<?>> runner = new GridEpisodeRunner<Robot<?>>(
        namedSolutionGrid,
        locomotion,
        gridOnlineViewer,
        executor
    );
    runner.run();
  }

  public static double assessOnLocomotion(double[] weights) {
    // set robot shape and sensors
    final Grid<Boolean> structure = Grid.create(7, 4, (x, y) -> (x < 2) || (x >= 5) || (y > 0));
    Grid<SensingVoxel> voxels = Grid.create(structure.getW(), structure.getH(), (x, y) -> {
      if (structure.get(x, y)) {
        if (y > 2) {
          return new SensingVoxel(Arrays.asList(
              new Velocity(true, 3d, Velocity.Axis.X, Velocity.Axis.Y),
              new Average(new Velocity(true, 3d, Velocity.Axis.X, Velocity.Axis.Y), 1d)
          ));
        }
        if (y == 0) {
          return new SensingVoxel(Arrays.asList(
              new Average(new Touch(), 1d)
          ));
        }
        return new SensingVoxel(Arrays.asList(
            new AreaRatio()
        ));
      }
      return null;
    });
    // set controller
    CentralizedMLP controller = new CentralizedMLP(
        voxels,
        new int[]{100},
        t -> 1d * Math.sin(-2d * Math.PI * t * 0.5d)
    );
    controller.setParams(weights);
    Robot<SensingVoxel> robot = new Robot(controller, voxels);
    // set task
    Settings settings = new Settings();
    settings.setStepFrequency(1d / 30d);
    Locomotion locomotion = new Locomotion(
        60,
        Locomotion.createTerrain("flat"),
        Lists.newArrayList(Locomotion.Metric.TRAVEL_X_VELOCITY),
        settings
    );
    // do task
    List<Double> results = locomotion.apply(robot);
    return results.get(0);
  }

  public static double assessOnLocomotion2(double[] phases) {
    // set robot shape and sensors
    Grid<ControllableVoxel> voxels = Grid.create(10, 4, (x, y) -> new ControllableVoxel());
    // set controller
    double f = 1d;
    Controller<ControllableVoxel> controller = new TimeFunctions(Grid.create(
        voxels.getW(),
        voxels.getH(),
        (x, y) -> (t) -> Math.sin(-2 * Math.PI * f * t + Math.PI * phases[(x + (int) Math.floor(y / voxels.getH()))]))
    );
    Robot<ControllableVoxel> robot = new Robot<>(controller, voxels);
    // set task
    Settings settings = new Settings();
    settings.setStepFrequency(1d / 30d);
    Locomotion locomotion = new Locomotion(
        60,
        Locomotion.createTerrain("flat"),
        Lists.newArrayList(Locomotion.Metric.TRAVEL_X_VELOCITY),
        settings
    );
    // do task
    List<Double> results = locomotion.apply(robot);
    return results.get(0);
  }

}
