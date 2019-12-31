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
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.objects.VoxelCompound;
import it.units.erallab.hmsrobots.episodes.Locomotion;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;
import it.units.erallab.hmsrobots.viewers.GridEpisodeRunner;
import it.units.erallab.hmsrobots.viewers.GridOnlineViewer;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.commons.lang3.tuple.Pair;
import org.dyn4j.dynamics.Settings;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Starter {

  public static void main(String[] args) throws IOException {
    final Grid<Boolean> structure = Grid.create(11, 5, (x, y) -> (x < 2) || (x >= 9) || (y > 0));
    Voxel.Builder builder = Voxel.Builder.create()
            .springF(25d)
            .massSideLengthRatio(0.05)
            .massLinearDamping(1d)
            .massAngularDamping(1d)
            .restitution(0)
            .friction(1000)
            .areaRatioOffset(0.2d)
            //.forceMethod(Voxel.ForceMethod.FORCE)
            //.maxForce(10)
            .ropeJointsFlag(false)
            .springScaffoldings(EnumSet.of(
                    Voxel.SpringScaffolding.SIDE_EXTERNAL,
                    //Voxel.SpringScaffolding.SIDE_INTERNAL,
                    //Voxel.SpringScaffolding.SIDE_CROSS,
                    Voxel.SpringScaffolding.CENTRAL_CROSS
            ));
    Settings settings = new Settings();
    settings.setStepFrequency(1d/30d);
    int controlInterval = 2;
    //simple
    double f = 1d;
    VoxelCompound.Description vcd1 = new VoxelCompound.Description(
            Grid.create(structure, b -> b ? builder : null),
            new TimeFunction(Grid.create(
                    structure.getW(),
                    structure.getH(),
                    (final Integer x, final Integer y) -> (Double t) -> Math.sin(-2 * Math.PI * f * t + Math.PI * ((double) x / (double) structure.getW()))
            )));
    //centralized mlp
    Grid<List<ClosedLoopController.TimedSensor>> centralizedSensorGrid = Grid.create(structure.getW(), structure.getH(),
            (x, y) -> {
              if (!structure.get(x, y)) {
                return null;
              }
              List<ClosedLoopController.TimedSensor> sensors = new ArrayList<>();
              if (y > 2) {
                sensors.add(new ClosedLoopController.TimedSensor(Voxel.Sensor.Y_ROT_VELOCITY, 0));
                sensors.add(new ClosedLoopController.TimedSensor(Voxel.Sensor.Y_ROT_VELOCITY, 1));
                sensors.add(new ClosedLoopController.TimedSensor(Voxel.Sensor.X_ROT_VELOCITY, 0));
                sensors.add(new ClosedLoopController.TimedSensor(Voxel.Sensor.X_ROT_VELOCITY, 1));
              }
              if (y == 0) {
                sensors.add(new ClosedLoopController.TimedSensor(Voxel.Sensor.TOUCHING, 0));
              }
              sensors.add(new ClosedLoopController.TimedSensor(Voxel.Sensor.AREA_RATIO, 0));
              return sensors;
            }
    );
    int[] innerNeurons = new int[]{100};
    int nOfWeights = CentralizedMLP.countParams(structure, centralizedSensorGrid, innerNeurons);
    double[] weights = new double[nOfWeights];
    Random random = new Random();
    for (int i = 0; i < weights.length; i++) {
      weights[i] = random.nextDouble() * 2d - 1d;
    }
    VoxelCompound.Description vcd2 = new VoxelCompound.Description(
            Grid.create(structure, b -> b ? builder : null),
            new CentralizedMLP(structure, centralizedSensorGrid, innerNeurons, weights, t -> 0d * Math.sin(-2d * Math.PI * t * 0.5d))
    );
    //distributed mlp
    Grid<List<ClosedLoopController.TimedSensor>> distributedSensorGrid = Grid.create(structure, b -> b ? Lists.newArrayList(
            new ClosedLoopController.TimedSensor(Voxel.Sensor.X_ROT_VELOCITY, 0, 5, ClosedLoopController.Aggregate.MEAN),
            new ClosedLoopController.TimedSensor(Voxel.Sensor.Y_ROT_VELOCITY, 0, 5, ClosedLoopController.Aggregate.MEAN),
            new ClosedLoopController.TimedSensor(Voxel.Sensor.AREA_RATIO, 0, 5, ClosedLoopController.Aggregate.MEAN),
            new ClosedLoopController.TimedSensor(Voxel.Sensor.AREA_RATIO, 0, 5, ClosedLoopController.Aggregate.DIFF),
            new ClosedLoopController.TimedSensor(Voxel.Sensor.TOUCHING, 0)
    ) : null);
    innerNeurons = new int[]{10};
    nOfWeights = DistributedMLP.countParams(structure, distributedSensorGrid, 1, innerNeurons);
    weights = new double[nOfWeights];
    for (int i = 0; i < weights.length; i++) {
      weights[i] = random.nextDouble() * 2d - 1d;
    }
    VoxelCompound.Description vcd3 = new VoxelCompound.Description(
            Grid.create(structure, b -> b ? Voxel.Builder.create().massCollisionFlag(true) : null),
            new DistributedMLP(
                    structure,
                    Grid.create(structure.getW(), structure.getH(), (x, y) -> {
                      if (x == 300) {
                        return t -> Math.sin(-2d * Math.PI * t * 0.5d);
                      } else {
                        return t -> 0d;
                      }
                    }),
                    distributedSensorGrid,
                    1,
                    innerNeurons,
                    weights
            )
    );
    //one sized centralizedMlp
    Grid<Boolean> one = Grid.create(1, 1, true);
    Grid<List<ClosedLoopController.TimedSensor>> oneSensorsGrid = Grid.create(1, 1, Lists.newArrayList(
            new ClosedLoopController.TimedSensor(Voxel.Sensor.AREA_RATIO, 0, 5, ClosedLoopController.Aggregate.MEAN),
            new ClosedLoopController.TimedSensor(Voxel.Sensor.AREA_RATIO, 0),
            new ClosedLoopController.TimedSensor(Voxel.Sensor.TOUCHING, 0)
    ));
    innerNeurons = new int[0];
    nOfWeights = CentralizedMLP.countParams(one, oneSensorsGrid, innerNeurons);
    weights = new double[nOfWeights];
    for (int i = 0; i < weights.length; i++) {
      weights[i] = random.nextDouble() * 2d - 1d;
    }
    VoxelCompound.Description vcd4 = new VoxelCompound.Description(
            Grid.create(one, b -> b ? builder : null),
            new CentralizedMLP(one, oneSensorsGrid, innerNeurons, weights, t -> 1d * Math.sin(-2d * Math.PI * t * 0.5d))
    );
    //episode
    Locomotion locomotion = new Locomotion(
            60,
            Locomotion.createTerrain("uneven25"),
            Lists.newArrayList(Locomotion.Metric.TRAVEL_X_VELOCITY),
            controlInterval,
            settings
    );
    Grid<Pair<String, VoxelCompound.Description>> namedSolutionGrid = Grid.create(1, 2);
    namedSolutionGrid.set(0, 0, Pair.of("phase", vcd1));
    //namedSolutionGrid.set(0, 0, Pair.of("distributedMLP", vcd4));
    namedSolutionGrid.set(0, 1, Pair.of("centralizedMLP", vcd2));
    //namedSolutionGrid.set(0, 2, Pair.of("distributedMLP", vcd3));
    ScheduledExecutorService uiExecutor = Executors.newScheduledThreadPool(4);
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    GridOnlineViewer gridOnlineViewer = new GridOnlineViewer(Grid.create(namedSolutionGrid, Pair::getLeft), uiExecutor, GraphicsDrawer.RenderingDirectives.create());
    gridOnlineViewer.start(5);
    GridEpisodeRunner<VoxelCompound.Description> runner = new GridEpisodeRunner<>(
            namedSolutionGrid, locomotion,
            gridOnlineViewer,
            executor
    );
    runner.run();
  }

}
