/*
 * Copyright (C) 2021 Eric Medvet <eric.medvet@gmail.com> (as Eric Medvet <eric.medvet@gmail.com>)
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

import it.units.erallab.hmsrobots.behavior.PoseUtils;
import it.units.erallab.hmsrobots.core.controllers.*;
import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.objects.ControllableVoxel;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.core.sensors.Angle;
import it.units.erallab.hmsrobots.core.sensors.Lidar;
import it.units.erallab.hmsrobots.core.sensors.Trend;
import it.units.erallab.hmsrobots.core.sensors.Velocity;
import it.units.erallab.hmsrobots.core.snapshots.MLPState;
import it.units.erallab.hmsrobots.tasks.Task;
import it.units.erallab.hmsrobots.tasks.balancing.Balancing;
import it.units.erallab.hmsrobots.tasks.devolocomotion.TimeBasedDevoLocomotion;
import it.units.erallab.hmsrobots.tasks.locomotion.Locomotion;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.RobotUtils;
import it.units.erallab.hmsrobots.util.SerializationUtils;
import it.units.erallab.hmsrobots.viewers.FramesImageBuilder;
import it.units.erallab.hmsrobots.viewers.GridFileWriter;
import it.units.erallab.hmsrobots.viewers.GridOnlineViewer;
import it.units.erallab.hmsrobots.viewers.VideoUtils;
import it.units.erallab.hmsrobots.viewers.drawers.Drawer;
import it.units.erallab.hmsrobots.viewers.drawers.Drawers;
import it.units.erallab.hmsrobots.viewers.drawers.MLPDrawer;
import it.units.erallab.hmsrobots.viewers.drawers.SubtreeDrawer;
import org.apache.commons.lang3.tuple.Pair;
import org.dyn4j.dynamics.Settings;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.UnaryOperator;
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
    Robot<ControllableVoxel> robot = new Robot<>(
        new TimeFunctions(Grid.create(
            w, h,
            (x, y) -> (Double t) -> Math.sin(-2 * Math.PI * t + Math.PI * ((double) x / (double) w))
        )),
        Grid.create(
            w, h,
            (x, y) -> (y == 0) ? SerializationUtils.clone(hardMaterialVoxel) : SerializationUtils.clone(softMaterialVoxel)
        )
    );
    FramesImageBuilder framesImageBuilder = new FramesImageBuilder(
        5, 5.5, 0.1, 300, 200, FramesImageBuilder.Direction.HORIZONTAL, Drawers.basic()
    );
    Outcome result = locomotion.apply(robot, framesImageBuilder);
    BufferedImage image = framesImageBuilder.getImage();
    System.out.println("Outcome: " + result);
  }

  public static void main(String[] args) {
    //bipedWithBrain();
    bipeds();
    //rollingOne();
    //rollingBall();
    //breakingWorm();
    //plainWorm();
    //cShaped();
    //multiped();
    //bipedAndBall();
    //bipedCentralized();
    //devoComb();
    //bipedPoses();
  }

  private static void bipedWithBrain() {
    Grid<? extends SensingVoxel> body = RobotUtils.buildSensorizingFunction("spinedTouch-t-f-0").apply(RobotUtils.buildShape("biped-7x4"));
    Random random = new Random();
    //centralized sensing
    CentralizedSensing centralizedSensing = new CentralizedSensing(body);

    int nOfInputs = centralizedSensing.nOfInputs();
    int nOfOutputs = centralizedSensing.nOfOutputs();
    int[] innerNeurons = new int[]{centralizedSensing.nOfInputs() * 2 / 3, centralizedSensing.nOfInputs() * 2 / 3};
    int nOfWeights = MultiLayerPerceptron.countWeights(nOfInputs, innerNeurons, nOfOutputs);
    double[] weights = IntStream.range(0, nOfWeights).mapToDouble(i -> 2 * Math.random() - 1).toArray();
    MultiLayerPerceptron mlp = new MultiLayerPerceptron(MultiLayerPerceptron.ActivationFunction.TANH,
        nOfInputs, innerNeurons, nOfOutputs, weights);
    centralizedSensing.setFunction(mlp);
    Robot<SensingVoxel> centralized = new Robot<>(
        centralizedSensing,
        SerializationUtils.clone(body)
    );
    //episode
    Locomotion locomotion = new Locomotion(
        30,
        Locomotion.createTerrain("downhill-30"),
        new Settings()
    );
    GridOnlineViewer.run(locomotion, Grid.create(1, 1, Pair.of("", centralized)), Drawers::basicWithMiniWorldAndBrainUsage);
  }

  private static void devoComb() {
    int startingL = 3;
    double f = 1d;
    UnaryOperator<Robot<?>> devoFunction = r -> {
      int l = (r == null) ? startingL : (r.getVoxels().getW() + 1);
      Grid<? extends SensingVoxel> body = RobotUtils
          .buildSensorizingFunction("uniform-ax+t+r-0.01")
          .apply(Grid.create(l, 2, (x, y) -> y > 0 || (x % 2 == 0)));
      return new Robot<>(
          new TimeFunctions(Grid.create(
              body.getW(),
              body.getH(),
              (final Integer x, final Integer y) -> (Double t) -> Math.sin(-2 * Math.PI * f * t + Math.PI * ((double) x / (double) body.getW()))
          )),
          body
      );
    };
    //DistanceBasedDevoLocomotion devoLocomotion = new DistanceBasedDevoLocomotion(20, 20, 60, Locomotion.createTerrain("downhill-20"), new Settings());
    TimeBasedDevoLocomotion devoLocomotion = TimeBasedDevoLocomotion.uniformlyDistributedTimeBasedDevoLocomotion(10, 40d, Locomotion.createTerrain("downhill-20"), new Settings());
    GridOnlineViewer.run(devoLocomotion, devoFunction);
  }

  private static void bipeds() {
    Grid<? extends SensingVoxel> body = RobotUtils.buildSensorizingFunction("spinedTouch-t-f-0").apply(RobotUtils.buildShape("t-4x5"));
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
          new int[]{2},
          distributedSensing.nOfOutputs(entry.getX(), entry.getY())
      );
      double[] ws = mlp.getParams();
      IntStream.range(0, ws.length).forEach(i -> ws[i] = random.nextDouble() * 2d - 1d);
      mlp.setParams(ws);
      distributedSensing.getFunctions().set(entry.getX(), entry.getY(), mlp);
    }
    Robot<SensingVoxel> distHetero = new Robot<>(
        distributedSensing,
        SerializationUtils.clone(body)
    );
    //centralized sensing
    CentralizedSensing centralizedSensing = new CentralizedSensing(body);
    MultiLayerPerceptron mlp = new PruningMultiLayerPerceptron(
        MultiLayerPerceptron.ActivationFunction.TANH,
        centralizedSensing.nOfInputs(),
        new int[]{centralizedSensing.nOfInputs() * 2 / 3, centralizedSensing.nOfInputs() * 2 / 3},
        centralizedSensing.nOfOutputs(),
        10d,
        PruningMultiLayerPerceptron.Context.NETWORK,
        PruningMultiLayerPerceptron.Criterion.ABS_SIGNAL_MEAN,
        0.95
    );
    double[] ws = mlp.getParams();
    IntStream.range(0, ws.length).forEach(i -> ws[i] = random.nextDouble() * 2d - 1d);
    mlp.setParams(ws);
    centralizedSensing.setFunction(mlp);
    Robot<SensingVoxel> centralized = new Robot<>(
        centralizedSensing,
        SerializationUtils.clone(body)
    );
    //episode
    Task<Robot<?>, Outcome> locomotion = new Balancing(
        30,
        10,
            body.getW() * 3 * 0.75,
        - body.getW() * 1.5,
        10,
        new Settings()
    );

    Grid<Pair<String, Robot<?>>> namedSolutionGrid = Grid.create(1, 4);
    namedSolutionGrid.set(0, 0, Pair.of("dist-hetero", distHetero));
    namedSolutionGrid.set(0, 1, Pair.of("centralized", centralized));
    namedSolutionGrid.set(0, 2, Pair.of("phasesRobot", phasesRobot));
    namedSolutionGrid.set(0, 3, Pair.of("phasesRobot-step-0.5",
        new Robot<>(
            ((AbstractController) phasesRobot.getController()).step(0.5),
            SerializationUtils.clone(phasesRobot.getVoxels())
        )
    ));
    //GridOnlineViewer.run(locomotion, namedSolutionGrid);
    GridOnlineViewer.run(locomotion, Grid.create(1, 1, Pair.of("phasesRobot", phasesRobot)), Drawers::basic);
    /*try {
      GridFileWriter.save(
          locomotion,
          Grid.create(1, 1, Pair.of("phasesRobot", phasesRobot)),
          600, 600, 1, 24,
          VideoUtils.EncoderFacility.FFMPEG_SMALL,
          new File("/home/eric/biped-spectra.mp4"),
          Drawers::basicWithMiniWorldAndSpectra
      );
    } catch (IOException e) {
      e.printStackTrace();
    }*/
  }

  private static void bipedAndBall() {
    //simple biped
    Grid<? extends SensingVoxel> bipedBody = RobotUtils.buildSensorizingFunction("spinedTouch-t-f-0").apply(RobotUtils.buildShape("biped-7x4"));
    double f = 2d;
    Robot<ControllableVoxel> bipedRobot = new Robot<>(
        new TimeFunctions(Grid.create(
            bipedBody.getW(),
            bipedBody.getH(),
            (final Integer x, final Integer y) -> (Double t) -> Math.sin(-2 * Math.PI * f * t + Math.PI * ((double) x / (double) bipedBody.getW()))
        )),
        SerializationUtils.clone(bipedBody)
    );
    //centralized ball
    Random random = new Random();
    Grid<? extends SensingVoxel> ballBody = RobotUtils.buildSensorizingFunction("uniform-ax+t+r-0").apply(RobotUtils.buildShape("ball-7"));
    CentralizedSensing centralizedSensing = new CentralizedSensing(ballBody);
    MultiLayerPerceptron mlp = new MultiLayerPerceptron(
        MultiLayerPerceptron.ActivationFunction.TANH,
        centralizedSensing.nOfInputs(),
        new int[0],
        centralizedSensing.nOfOutputs()
    );
    mlp.setParams(IntStream.range(0, mlp.getParams().length).mapToDouble(i -> random.nextGaussian()).toArray());
    centralizedSensing.setFunction(mlp);
    Robot<SensingVoxel> ballRobot = new Robot<>(
        centralizedSensing,
        SerializationUtils.clone(ballBody)
    );
    //episode
    Locomotion locomotion = new Locomotion(
        30,
        Locomotion.createTerrain("downhill-15"),
        new Settings()
    );

    Grid<Pair<String, Robot<?>>> namedSolutionGrid = Grid.create(2, 1);
    namedSolutionGrid.set(0, 0, Pair.of("biped", bipedRobot));
    namedSolutionGrid.set(1, 0, Pair.of("ball", ballRobot));
    GridOnlineViewer.run(locomotion, namedSolutionGrid, Drawers::basicWithMiniWorldAndFootprintsAndPosture);
    if (false) {
      try {
        GridFileWriter.save(
            locomotion,
            namedSolutionGrid,
            800, 400, 1, 24,
            VideoUtils.EncoderFacility.FFMPEG_SMALL,
            new File("/home/eric/biped+ball-footprints+posture.mp4"),
            Drawers::basicWithMiniWorldAndFootprintsAndPosture
        );
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private static void bipedCentralized() {
    Random random = new Random();
    Grid<? extends SensingVoxel> body = RobotUtils.buildSensorizingFunction("spinedTouchSighted-f-f-0.05").apply(RobotUtils.buildShape("biped-5x3"));
    CentralizedSensing centralizedSensing = new CentralizedSensing(body);
    MultiLayerPerceptron mlp = new MultiLayerPerceptron(
        MultiLayerPerceptron.ActivationFunction.TANH,
        centralizedSensing.nOfInputs(),
        new int[]{10, 5},
        centralizedSensing.nOfOutputs()
    );
    mlp.setParams(IntStream.range(0, mlp.getParams().length).mapToDouble(i -> random.nextDouble() * 2d - 1d).toArray());
    centralizedSensing.setFunction(mlp);
    Robot<SensingVoxel> robot = new Robot<>(
        centralizedSensing,
        SerializationUtils.clone(body)
    );
    //episode
    Locomotion locomotion = new Locomotion(
        30,
        Locomotion.createTerrain("downhill-15"),
        new Settings()
    );
    GridOnlineViewer.run(
        locomotion,
        Grid.create(1, 1, Pair.of("robot", robot)),
        s -> Drawer.of(
            Drawer.clip(
                BoundingBox.of(0d, 0d, 1d, 0.5d),
                Drawers.basicWithMiniWorld(s)
            ),
            Drawer.clip(
                BoundingBox.of(0d, 0.5d, 1d, 1d),
                Drawer.of(
                    Drawer.clear(),
                    new MLPDrawer(
                        SubtreeDrawer.Extractor.matches(MLPState.class, null, null),
                        15d
                    )
                )
            )
        )
    );
  }

  private static void multiped() {
    double f = 1d;
    Grid<Boolean> body = Grid.create(7, 2, (x, y) -> y == 1 || (x % 2 == 0));
    Robot<?> robot = new Robot<>(
        new TimeFunctions(Grid.create(
            body.getW(),
            body.getH(),
            (final Integer x, final Integer y) -> (Double t) -> Math.sin(
                -2 * Math.PI * f * t + 2 * Math.PI * ((double) x / (double) body.getW()) + Math.PI * ((double) y / (double) body.getH())
            )
        )),
        RobotUtils.buildSensorizingFunction("uniform-a-0.01").apply(body)
    );
    Locomotion locomotion = new Locomotion(
        10,
        Locomotion.createTerrain("hilly-0.3-1-0"),
        new Settings()
    );
    GridOnlineViewer.run(locomotion, robot);
    FramesImageBuilder framesImageBuilder = new FramesImageBuilder(5, 7, .75, 300, 200, FramesImageBuilder.Direction.HORIZONTAL, Drawers.basic());
    locomotion.apply(robot, framesImageBuilder);
    try {
      ImageIO.write(framesImageBuilder.getImage(), "png", new File("/home/eric/frames-multiped.png"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void breakingWorm() {
    Grid<? extends SensingVoxel> body = RobotUtils.buildSensorizingFunction("spinedTouch-f-t-0").apply(RobotUtils.buildShape("worm-4x3"));
    double f = 1d;
    Robot<ControllableVoxel> unbreakableRobot = new Robot<>(
        new TimeFunctions(Grid.create(
            body.getW(),
            body.getH(),
            (final Integer x, final Integer y) -> (Double t) -> Math.sin(-2 * Math.PI * f * t + Math.PI * ((double) x / (double) body.getW()))
        )),
        body
    );
    Robot<?> breakableRobot = RobotUtils.buildRobotTransformation(
        "breakable-area-1000/500-3/0.5-0",
        new Random(0)
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
    GridOnlineViewer.run(locomotion, namedSolutionGrid);
  }

  private static void plainWorm() {
    Grid<? extends SensingVoxel> body = RobotUtils
        //.buildSensorizingFunction("uniform-l1-0.01")
        //.buildSensorizingFunction("uniform-l5-0")
        .buildSensorizingFunction("uniform-l5+vxy+t-0.01")
        .apply(RobotUtils.buildShape("worm-5x2"));
    double f = 0.333d;
    Robot<?> robot = new Robot<>(
        new TimeFunctions(Grid.create(
            body.getW(),
            body.getH(),
            /*(final Integer x, final Integer y) -> (Double t) -> Math.sin(
                -2 * Math.PI * f * t + 2 * Math.PI * ((double) x / (double) body.getW()) + Math.PI * ((double) y / (double) body.getH())
            )*/
            (x, y) -> t -> Math.signum(Math.sin(-2 * Math.PI * f * t + ((x <= body.getW() / 2) ? Math.PI / 2d : 0d)))
        )).smoothed(10),
        SerializationUtils.clone(body)
    );
    robot = RobotUtils.buildRobotTransformation("broken-0.0-0", new Random(0)).apply(robot);
    //episode
    Locomotion locomotion = new Locomotion(
        30,
        Locomotion.createTerrain("flatWithStart-2"),
        new Settings()
    );
    //GridOnlineViewer.run(locomotion, robot);
    GridOnlineViewer.run(locomotion, Grid.create(1, 1, Pair.of("phasesRobot", robot)), Drawers::basicWithMiniWorldAndSpectra);
  }

  private static void bipedPoses() {
    Grid<Boolean> shape = RobotUtils.buildShape("biped-8x4");
    Grid<? extends SensingVoxel> body = RobotUtils
        .buildSensorizingFunction("uniform-t-0")
        .apply(shape);
    PosesController controller = new PosesController(1d, new ArrayList<>(PoseUtils.computeCardinalPoses(shape)));
    Robot<?> robot = new Robot<>(controller, body);
    //episode
    Locomotion locomotion = new Locomotion(
        30,
        Locomotion.createTerrain("hilly-1-10-0"),
        new Settings()
    );
    GridOnlineViewer.run(locomotion, robot);
  }

  private static void cShaped() {
    Grid<Boolean> shape = Grid.create(8, 8, (x, y) -> x < 2 || y < 2 || y >= 6);
    Grid<? extends SensingVoxel> body = RobotUtils.buildSensorizingFunction("uniform-t+vxy-0.05").apply(shape);
    Robot<?> robot = new Robot<>(
        new CentralizedSensing(body),
        SerializationUtils.clone(body)
    );
    //episode
    Locomotion locomotion = new Locomotion(
        20,
        Locomotion.createTerrain("flatWithStart-2"),
        new Settings()
    );
    //GridOnlineViewer.run(locomotion, robot);
    try {
      GridFileWriter.save(locomotion, robot, 600, 400, 0, 25, VideoUtils.EncoderFacility.FFMPEG_SMALL, new File("/home/eric/cshaped.mp4"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    GridOnlineViewer.run(locomotion, robot);
  }

  private static void rollingOne() {
    //one voxel robot
    Grid<SensingVoxel> oneBody = Grid.create(1, 1, new SensingVoxel(List.of(
        new Velocity(true, 8d, Velocity.Axis.X, Velocity.Axis.Y),
        new Trend(new Velocity(true, 4d, Velocity.Axis.X, Velocity.Axis.Y), 0.25),
        new Angle(),
        new Lidar(10, Map.of(Lidar.Side.E, 4))
    )));
    Robot<SensingVoxel> robot = new Robot<>(
        new CentralizedSensing(oneBody, RealFunction.build(in -> new double[]{0d}, 1, 1)),
        oneBody
    );
    //episode
    Locomotion locomotion = new Locomotion(
        60,
        new double[][]{new double[]{0, 10, 30, 31, 100, 1000, 1010}, new double[]{100, 100, 80, 10, 10, 0, 100}},
        new Settings()
    );
    GridOnlineViewer.run(locomotion, robot);
  }

  private static void rollingBall() {
    Random random = new Random();
    Grid<Boolean> shape = RobotUtils.buildShape("ball-7");
    Grid<? extends SensingVoxel> body = RobotUtils.buildSensorizingFunction("uniform-ax+t+r-0").apply(shape);
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
        20,
        Locomotion.createTerrain("downhill-30"),
        new Settings()
    );

    GridOnlineViewer.run(
        locomotion,
        Grid.create(1, 1, Pair.of("", robot)),
        s -> Drawers.basicWithMiniWorld(s)
    );
    /*
    try {
      GridFileWriter.save(
          locomotion,
          Grid.create(1, 1, Pair.of("", robot)),
          600, 600,
          0, 30,
          VideoUtils.EncoderFacility.FFMPEG_SMALL, new File("/home/eric/ball-w-activity.mp4"),
          drawerSupplier
      );
    } catch (IOException e) {
      e.printStackTrace();
    }
     */
  }

}
