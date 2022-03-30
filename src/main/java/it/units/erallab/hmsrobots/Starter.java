/*
 * Copyright (C) 2022 Eric Medvet <eric.medvet@gmail.com> (as Eric Medvet <eric.medvet@gmail.com>)
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
import it.units.erallab.hmsrobots.core.objects.Ground;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.core.sensors.Angle;
import it.units.erallab.hmsrobots.core.sensors.Lidar;
import it.units.erallab.hmsrobots.core.sensors.Trend;
import it.units.erallab.hmsrobots.core.sensors.Velocity;
import it.units.erallab.hmsrobots.core.snapshots.MLPState;
import it.units.erallab.hmsrobots.tasks.devolocomotion.TimeBasedDevoLocomotion;
import it.units.erallab.hmsrobots.tasks.locomotion.Locomotion;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.RobotUtils;
import it.units.erallab.hmsrobots.util.SerializationUtils;
import it.units.erallab.hmsrobots.viewers.*;
import it.units.erallab.hmsrobots.viewers.drawers.*;
import org.dyn4j.dynamics.Settings;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Starter {

  // TODO clean up

  private static void bipedAndBall() {
    //simple biped
    Grid<Voxel> bipedBody = RobotUtils.buildSensorizingFunction("spinedTouch-t-f-0").apply(RobotUtils.buildShape(
        "biped-7x4"));
    double f = 2d;
    Robot bipedRobot = new Robot(new TimeFunctions(Grid.create(
        bipedBody.getW(),
        bipedBody.getH(),
        (final Integer x, final Integer y) -> (Double t) -> Math.sin(-2 * Math.PI * f * t + Math.PI * ((double) x / (double) bipedBody.getW()))
    )), SerializationUtils.clone(bipedBody));
    //centralized ball
    Random random = new Random();
    Grid<Voxel> ballBody = RobotUtils.buildSensorizingFunction("uniform-ax+t+r-0")
        .apply(RobotUtils.buildShape("ball-7"));
    CentralizedSensing centralizedSensing = new CentralizedSensing(ballBody);
    MultiLayerPerceptron mlp = new MultiLayerPerceptron(
        MultiLayerPerceptron.ActivationFunction.TANH,
        centralizedSensing.nOfInputs(),
        new int[0],
        centralizedSensing.nOfOutputs()
    );
    mlp.setParams(IntStream.range(0, mlp.getParams().length).mapToDouble(i -> random.nextGaussian()).toArray());
    centralizedSensing.setFunction(mlp);
    Robot ballRobot = new Robot(centralizedSensing, SerializationUtils.clone(ballBody));
    //episode
    Locomotion locomotion = new Locomotion(30, Locomotion.createTerrain("downhill-15"), new Settings());
    Grid<NamedValue<Robot>> namedSolutionGrid = Grid.create(2, 1);
    namedSolutionGrid.set(0, 0, new NamedValue<>("biped", bipedRobot));
    namedSolutionGrid.set(1, 0, new NamedValue<>("ball", ballRobot));
    GridOnlineViewer.run(locomotion, namedSolutionGrid, Drawers::basicWithMiniWorldAndFootprintsAndPosture);
  }

  private static void bipedCentralized() {
    Random random = new Random();
    Grid<Voxel> body = RobotUtils.buildSensorizingFunction("spinedTouchSighted-f-f-0.05").apply(RobotUtils.buildShape(
        "biped-5x3"));
    CentralizedSensing centralizedSensing = new CentralizedSensing(body);
    MultiLayerPerceptron mlp = new MultiLayerPerceptron(
        MultiLayerPerceptron.ActivationFunction.TANH,
        centralizedSensing.nOfInputs(),
        new int[]{10, 5},
        centralizedSensing.nOfOutputs()
    );
    mlp.setParams(IntStream.range(0, mlp.getParams().length).mapToDouble(i -> random.nextDouble() * 2d - 1d).toArray());
    centralizedSensing.setFunction(mlp);
    Robot robot = new Robot(centralizedSensing, SerializationUtils.clone(body));
    //episode
    Locomotion locomotion = new Locomotion(30, Locomotion.createTerrain("downhill-15"), new Settings());
    GridOnlineViewer.run(
        locomotion,
        Grid.create(1, 1, new NamedValue<>("robot", robot)),
        s -> Drawer.of(
            Drawer.clip(BoundingBox.of(0d, 0d, 1d, 0.5d), Drawers.basicWithMiniWorld(s)),
            Drawer.clip(
                BoundingBox.of(0d, 0.5d, 1d, 1d),
                Drawer.of(
                    Drawer.clear(),
                    new MLPDrawer(SubtreeDrawer.Extractor.matches(MLPState.class, null, null), 15d)
                )
            )
        )
    );
  }

  private static void bipedPoses() {
    Grid<Boolean> shape = RobotUtils.buildShape("biped-8x4");
    Grid<Voxel> body = RobotUtils.buildSensorizingFunction("uniform-t-0").apply(shape);
    PosesController controller = new PosesController(1d, new ArrayList<>(PoseUtils.computeCardinalPoses(shape)));
    Robot robot = new Robot(controller, body);
    //episode
    Locomotion locomotion = new Locomotion(30, Locomotion.createTerrain("hilly-1-10-0"), new Settings());
    GridOnlineViewer.run(locomotion, robot);
  }

  private static void bipedWithBrain() {
    Grid<Voxel> body = RobotUtils.buildSensorizingFunction("spinedTouch-t-f-0")
        .apply(RobotUtils.buildShape("biped-7x4"));
    Random random = new Random();
    //centralized sensing
    CentralizedSensing centralizedSensing = new CentralizedSensing(body);
    int nOfInputs = centralizedSensing.nOfInputs();
    int nOfOutputs = centralizedSensing.nOfOutputs();
    int[] innerNeurons = new int[]{centralizedSensing.nOfInputs() * 2 / 3, centralizedSensing.nOfInputs() * 2 / 3};
    int nOfWeights = MultiLayerPerceptron.countWeights(nOfInputs, innerNeurons, nOfOutputs);
    double[] weights = IntStream.range(0, nOfWeights).mapToDouble(i -> 2 * Math.random() - 1).toArray();
    MultiLayerPerceptron mlp = new MultiLayerPerceptron(
        MultiLayerPerceptron.ActivationFunction.TANH,
        nOfInputs,
        innerNeurons,
        nOfOutputs,
        weights
    );
    centralizedSensing.setFunction(mlp);
    Robot centralized = new Robot(centralizedSensing, SerializationUtils.clone(body));
    //episode
    Locomotion locomotion = new Locomotion(30, Locomotion.createTerrain("downhill-30"), new Settings());
    GridOnlineViewer.run(
        locomotion,
        Grid.create(1, 1, new NamedValue<>("", centralized)),
        Drawers::basicWithMiniWorldAndBrainUsage
    );
  }

  private static void bipeds() {
    Grid<Voxel> body = RobotUtils.buildSensorizingFunction("spinedTouch-t-f-0")
        .apply(RobotUtils.buildShape("biped-7x4"));
    //simple
    double f = 1d;
    Robot phasesRobot = new Robot(new TimeFunctions(Grid.create(
        body.getW(),
        body.getH(),
        (final Integer x, final Integer y) -> (Double t) -> Math.sin(-2 * Math.PI * f * t + Math.PI * ((double) x / (double) body.getW()))
    )), SerializationUtils.clone(body));
    //distribute sensing
    Random random = new Random();
    DistributedSensing distributedSensing = new DistributedSensing(body, 1);
    for (Grid.Entry<Voxel> entry : body) {
      MultiLayerPerceptron mlp = new MultiLayerPerceptron(
          MultiLayerPerceptron.ActivationFunction.TANH,
          distributedSensing.nOfInputs(entry.key().x(), entry.key().y()),
          new int[]{2},
          distributedSensing.nOfOutputs(entry.key().x(), entry.key().y())
      );
      double[] ws = mlp.getParams();
      IntStream.range(0, ws.length).forEach(i -> ws[i] = random.nextDouble() * 2d - 1d);
      mlp.setParams(ws);
      distributedSensing.getFunctions().set(entry.key().x(), entry.key().y(), mlp);
    }
    Robot distHetero = new Robot(distributedSensing, SerializationUtils.clone(body));
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
    Robot centralized = new Robot(centralizedSensing, SerializationUtils.clone(body));
    //episode
    Locomotion locomotion = new Locomotion(30, Locomotion.createTerrain("downhill-30"), new Settings());

    Grid<NamedValue<Robot>> namedSolutionGrid = Grid.create(1, 4);
    namedSolutionGrid.set(0, 0, new NamedValue<>("dist-hetero", distHetero));
    namedSolutionGrid.set(0, 1, new NamedValue<>("centralized", centralized));
    namedSolutionGrid.set(0, 2, new NamedValue<>("phasesRobot", phasesRobot));
    namedSolutionGrid.set(
        0,
        3,
        new NamedValue<>("phasesRobot-step-0.5", new Robot(
            ((AbstractController) phasesRobot.getController()).step(0.5),
            SerializationUtils.clone(phasesRobot.getVoxels())
        ))
    );

    Function<String, Drawer> drawerProvider = s -> Drawer.of(
        Drawer.clear(),
        Drawer.transform(
            new AllRobotFollower(1.5d, 2),
            Drawer.of(
                new GhostRobotDrawer(5, 1, 0, false),
                new PolyDrawer(PolyDrawer.TEXTURE_PAINT, SubtreeDrawer.Extractor.matches(null, Ground.class, null)),
                new VoxelDrawer()
            )
        )
    );

    FramesImageBuilder framesImageBuilder = new FramesImageBuilder(
        5,
        10,
        1,
        450,
        300,
        FramesImageBuilder.Direction.HORIZONTAL,
        drawerProvider.apply("")
    );
    locomotion.apply(phasesRobot, framesImageBuilder);
    try {
      ImageIO.write(framesImageBuilder.getImage(), "png", new File("/home/eric/ghost.png"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.exit(0);

    GridOnlineViewer.run(
        locomotion,
        Grid.create(1, 1, new NamedValue<>("phasesRobot", phasesRobot)),
        drawerProvider
    );


    //GridOnlineViewer.run(locomotion, namedSolutionGrid);
    /*GridOnlineViewer.run(
        locomotion,
        Grid.create(1, 1, new NamedValue<>("phasesRobot", phasesRobot)),
        Drawers::basicWithMiniWorldAndSpectra
    );*/
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

  private static void distBiped() {
    Grid<Voxel> body = RobotUtils.buildSensorizingFunction("spinedTouch-t-f-0")
        .apply(RobotUtils.buildShape("biped-4x3"));
    //distribute sensing
    Random random = new Random();
    DistributedSensing distributedSensing = new DistributedSensing(body, 1);
    for (Grid.Entry<Voxel> entry : body) {
      MultiLayerPerceptron mlp = new MultiLayerPerceptron(
          MultiLayerPerceptron.ActivationFunction.TANH,
          distributedSensing.nOfInputs(entry.key().x(), entry.key().y()),
          new int[]{2},
          distributedSensing.nOfOutputs(entry.key().x(), entry.key().y())
      );
      double[] ws = mlp.getParams();
      IntStream.range(0, ws.length).forEach(i -> ws[i] = random.nextDouble() * 2d - 1d);
      mlp.setParams(ws);
      distributedSensing.getFunctions().set(entry.key().x(), entry.key().y(), mlp);
    }
    Robot distHetero = new Robot(distributedSensing, SerializationUtils.clone(body));
    //episode
    Locomotion locomotion = new Locomotion(30, Locomotion.createTerrain("downhill-30"), new Settings());

    GridOnlineViewer.run(
        locomotion,
        Grid.create(1, 1, new NamedValue<>("dist", distHetero)),
        Drawers::basicDistributedWithMiniWorld
    );

  }

  private static void breakingWorm() {
    Grid<Voxel> body = RobotUtils.buildSensorizingFunction("spinedTouch-f-t-0")
        .apply(RobotUtils.buildShape("worm-4x3"));
    double f = 1d;
    Robot unbreakableRobot = new Robot(new TimeFunctions(Grid.create(
        body.getW(),
        body.getH(),
        (final Integer x, final Integer y) -> (Double t) -> Math.sin(-2 * Math.PI * f * t + Math.PI * ((double) x / (double) body.getW()))
    )), body);
    Robot breakableRobot = RobotUtils.buildRobotTransformation("breakable-area-1000/500-3/0.5-0", new Random(0)).apply(
        SerializationUtils.clone(unbreakableRobot));
    //episode
    Locomotion locomotion = new Locomotion(60, Locomotion.createTerrain("hilly-0.5-5-0"), new Settings());
    Grid<NamedValue<Robot>> namedSolutionGrid = Grid.create(1, 2);
    namedSolutionGrid.set(0, 0, new NamedValue<>("unbreakable", unbreakableRobot));
    namedSolutionGrid.set(0, 1, new NamedValue<>("breakable", breakableRobot));
    GridOnlineViewer.run(locomotion, namedSolutionGrid);
  }

  private static void cShaped() {
    Grid<Boolean> shape = Grid.create(10, 8, (x, y) -> x < 2 || y < 2 || y >= 6);
    Grid<Voxel> body = RobotUtils.buildSensorizingFunction("uniform-t+vxy-0.05").apply(shape);
    Robot robot = new Robot(new CentralizedSensing(body), SerializationUtils.clone(body));
    //episode
    Locomotion locomotion = new Locomotion(20, Locomotion.createTerrain("flatWithStart-2"), new Settings());
    GridOnlineViewer.run(locomotion, robot);
  }

  private static void critical() {
    String small = "0111-0110-1111-1010-1010";
    String big = "1111111111-1111111111-1111111111-1111110011-1111100011-1111000000-1110000000-1100000000";
    Grid<Voxel> body = RobotUtils.buildSensorizingFunction("uniform-a-0.0").apply(RobotUtils.buildShape("free-" + big));
    RandomGenerator r = new Random(1);
    Robot robot = new Robot(new PhaseSin(1, 1, Grid.create(body, v -> r.nextGaussian())), body);
    Locomotion locomotion = new Locomotion(20, Locomotion.createTerrain("hilly-3-30-0"), 100, new Settings());
    FramesImageBuilder framesImageBuilder = new FramesImageBuilder(
        15,
        17.5,
        .5,
        450,
        300,
        FramesImageBuilder.Direction.HORIZONTAL,
        Drawers.basic()
    );
    locomotion.apply(robot, framesImageBuilder);
    try {
      ImageIO.write(framesImageBuilder.getImage(), "png", new File("/home/eric/critical.png"));
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private static void devoComb() {
    int startingL = 3;
    double f = 1d;
    UnaryOperator<Robot> devoFunction = r -> {
      int l = (r == null) ? startingL : (r.getVoxels().getW() + 1);
      Grid<Voxel> body = RobotUtils.buildSensorizingFunction("uniform-ax+t+r-0.01").apply(Grid.create(
          l,
          2,
          (x, y) -> y > 0 || (x % 2 == 0)
      ));
      return new Robot(new TimeFunctions(Grid.create(
          body.getW(),
          body.getH(),
          (final Integer x, final Integer y) -> (Double t) -> Math.sin(-2 * Math.PI * f * t + Math.PI * ((double) x / (double) body.getW()))
      )), body);
    };
    //DistanceBasedDevoLocomotion devoLocomotion = new DistanceBasedDevoLocomotion(20, 20, 60, Locomotion
    // .createTerrain("downhill-20"), new Settings());
    TimeBasedDevoLocomotion devoLocomotion = TimeBasedDevoLocomotion.uniformlyDistributedTimeBasedDevoLocomotion(
        10,
        40d,
        Locomotion.createTerrain("downhill-20"),
        new Settings()
    );
    GridOnlineViewer.run(devoLocomotion, devoFunction);
  }

  public static void main(String[] args) {
    //distBiped();
    //bipedWithBrain();
    //bipeds();
    //rollingOne();
    rollingBall();
    //breakingWorm();
    //plainWorm();
    //cShaped();
    //multiped();
    //bipedAndBall();
    //bipedCentralized();
    //devoComb();
    //bipedPoses();
    //critical();
  }

  private static void multiped() {
    double f = 1d;
    Grid<Boolean> body = Grid.create(7, 2, (x, y) -> y == 1 || (x % 2 == 0));
    Robot robot = new Robot(new TimeFunctions(Grid.create(
        body.getW(),
        body.getH(),
        (final Integer x, final Integer y) -> (Double t) -> Math.sin(-2 * Math.PI * f * t + 2 * Math.PI * ((double) x / (double) body.getW()) + Math.PI * ((double) y / (double) body.getH()))
    )), RobotUtils.buildSensorizingFunction("uniform-a-0.01").apply(body));
    Locomotion locomotion = new Locomotion(10, Locomotion.createTerrain("hilly-0.3-1-0"), new Settings());
    GridOnlineViewer.run(locomotion, robot);
    FramesImageBuilder framesImageBuilder = new FramesImageBuilder(
        5,
        7,
        .75,
        300,
        200,
        FramesImageBuilder.Direction.HORIZONTAL,
        Drawers.basic()
    );
    locomotion.apply(robot, framesImageBuilder);
    try {
      ImageIO.write(framesImageBuilder.getImage(), "png", new File("/home/eric/frames-multiped.png"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void plainWorm() {
    Grid<Voxel> body = RobotUtils.buildSensorizingFunction("spinedTouchSighted-t-f-0.01").apply(RobotUtils.buildShape(
        "worm-10x4"));
    double f = 2d;
    Robot robot = new Robot(new TimeFunctions(Grid.create(body.getW(), body.getH(),
        //(x, y) -> t -> Math.signum(Math.sin(-2 * Math.PI * f * t + ((x <= body.getW() / 2) ? Math.PI / 2d : 0d)))
        (x, y) -> t -> Math.round(t / 5) % 2 == 0 ? 0 : Math.sin(-2 * Math.PI * f * t + ((x <= body.getW() / 2) ?
            Math.PI / 2d : 0d))
    )).smoothed(10), SerializationUtils.clone(body));
    robot = RobotUtils.buildRobotTransformation("broken-0.0-0", new Random(0)).apply(robot);
    //episode
    Locomotion locomotion = new Locomotion(30, Locomotion.createTerrain("flatWithStart-2"), new Settings());
    //GridOnlineViewer.run(locomotion, robot);
    GridOnlineViewer.run(
        locomotion,
        Grid.create(1, 1, new NamedValue<>("phasesRobot", robot)),
        Drawers::basicWithMiniWorldAndSpectra
    );
/*
    try {
      GridFileWriter.save(
          locomotion,
          robot,
          600,
          400,
          0,
          30,
          VideoUtils.EncoderFacility.FFMPEG_SMALL,
          new File("/home/eric/experiments/puf-vsr.mp4")
      );
    } catch (IOException e) {
      e.printStackTrace();
    }
*/
  }

  private static void rollingBall() {
    Random random = new Random();
    Grid<Boolean> shape = RobotUtils.buildShape("ball-7");
    Grid<Voxel> body = RobotUtils.buildSensorizingFunction("uniform-ax+t+r-0").apply(shape);
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
    Robot robot = new Robot(centralizedSensing, SerializationUtils.clone(body));
    //episode
    Locomotion locomotion = new Locomotion(13, Locomotion.createTerrain("downhill-30"), new Settings());

    //GridOnlineViewer.run(locomotion, Grid.create(1, 1, new NamedValue<>("", robot)), Drawers::basicWithMiniWorld);

    try {
      GridFileWriter.save(
          locomotion,
          Grid.create(1, 1, new NamedValue<>("ball", robot)),
          600, 600,
          3, 30,
          VideoUtils.EncoderFacility.FFMPEG_SMALL, new File("/home/eric/ball-w-activity.mp4")
      );
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private static void rollingOne() {
    //one voxel robot
    Grid<Voxel> oneBody = Grid.create(1, 1, new Voxel(List.of(
        new Velocity(true, 8d, Velocity.Axis.X, Velocity.Axis.Y),
        new Trend(new Velocity(true, 4d, Velocity.Axis.X, Velocity.Axis.Y), 0.25),
        new Angle(),
        new Lidar(10, Map.of(Lidar.Side.E, 4))
    )));
    Robot robot = new Robot(new CentralizedSensing(oneBody, RealFunction.build(in -> new double[]{0d}, 1, 1)), oneBody);
    //episode
    Locomotion locomotion = new Locomotion(
        60,
        new double[][]{new double[]{0, 10, 30, 31, 100, 1000, 1010}, new double[]{100, 100, 80, 10, 10, 0, 100}},
        new Settings()
    );
    GridOnlineViewer.run(locomotion, robot);
  }

}
