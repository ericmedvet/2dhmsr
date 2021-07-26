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
package it.units.erallab.hmsrobots.tasks.locomotion;

import it.units.erallab.hmsrobots.core.controllers.CentralizedSensing;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.erallab.hmsrobots.core.objects.ControllableVoxel;
import it.units.erallab.hmsrobots.core.objects.Ground;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.WorldObject;
import it.units.erallab.hmsrobots.core.objects.immutable.Voxel;
import it.units.erallab.hmsrobots.tasks.AbstractTask;
import it.units.erallab.hmsrobots.util.BoundingBox;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.Point2;
import it.units.erallab.hmsrobots.util.Utils;
import it.units.erallab.hmsrobots.viewers.SnapshotListener;
import org.apache.commons.lang3.time.StopWatch;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.AABB;
import org.dyn4j.geometry.Vector2;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Locomotion extends AbstractTask<Robot<?>, Outcome> {

    private final static double INITIAL_PLACEMENT_X_GAP = 1d;
    private final static double INITIAL_PLACEMENT_Y_GAP = 1d;
    private final static double TERRAIN_BORDER_HEIGHT = 100d;
    public static final int TERRAIN_LENGTH = 2000;
    private static final int FOOTPRINT_BINS = 8;
    private static final int MASK_BINS = 16;
    public static final double TERRAIN_BORDER_WIDTH = 10d;

    private final double finalT;
    private final double[][] groundProfile;
    private final double initialPlacement;

    public Locomotion(double finalT, double[][] groundProfile, Settings settings) {
        this(finalT, groundProfile, groundProfile[0][1] + INITIAL_PLACEMENT_X_GAP, settings);
    }

    public Locomotion(double finalT, double[][] groundProfile, double initialPlacement, Settings settings) {
        super(settings);
        this.finalT = finalT;
        this.groundProfile = groundProfile;
        this.initialPlacement = initialPlacement;
    }

    @Override
    public Outcome apply(Robot<?> robot, SnapshotListener listener) {
        StopWatch stopWatch = StopWatch.createStarted();
        //init world
        World world = new World();
        world.setSettings(settings);
        List<WorldObject> worldObjects = new ArrayList<>();
        Ground ground = new Ground(groundProfile[0], groundProfile[1]);
        ground.addTo(world);
        worldObjects.add(ground);
        robot.reset();
        //position robot: translate on x
        BoundingBox boundingBox = robot.boundingBox();
        robot.translate(new Vector2(initialPlacement - boundingBox.min.x, 0));
        //translate on y
        double minYGap = robot.getVoxels().values().stream()
                .filter(Objects::nonNull)
                .mapToDouble(v -> ((Voxel) v.immutable()).getShape().boundingBox().min.y - ground.yAt(v.getCenter().x))
                .min().orElse(0d);
        robot.translate(new Vector2(0, INITIAL_PLACEMENT_Y_GAP - minYGap));
        //get initial x
        double initCenterX = robot.getCenter().x;
        //add robot to world
        robot.addTo(world);
        worldObjects.add(robot);
        //run
        List<Outcome.Observation> observations = new ArrayList<>((int) Math.ceil(finalT / settings.getStepFrequency()));
        double t = 0d;
        //System.out.println("pre start");
        while (t < finalT) {
            //System.out.println("pre upd "+t);
            t = AbstractTask.updateWorld(t, settings.getStepFrequency(), world, worldObjects, listener);
            //System.out.println("pre obs "+t);
            ;
            double[] lastInput = {0d};
            if ( robot.getController() instanceof CentralizedSensing && ((CentralizedSensing) robot.getController()).getFunction() instanceof MultiLayerPerceptron){
                lastInput = ((MultiLayerPerceptron)((CentralizedSensing) robot.getController()).getFunction()).lastInput;
            }
            observations.add(new Outcome.Observation(
                    lastInput,
                    t,
                    Point2.build(robot.getCenter()),
                    ground.yAt(robot.getCenter().x),
                    footprint(robot, FOOTPRINT_BINS),
                    mask(robot, MASK_BINS),
                    robot.getVoxels().values().stream()
                            .filter(v -> (v instanceof ControllableVoxel))
                            .mapToDouble(ControllableVoxel::getControlEnergy)
                            .sum() - (observations.isEmpty() ? 0d : observations.get(observations.size() - 1).getControlEnergy()),
                    robot.getVoxels().values().stream()
                            .filter(v -> (v instanceof ControllableVoxel))
                            .mapToDouble(ControllableVoxel::getAreaRatioEnergy)
                            .sum() - (observations.isEmpty() ? 0d : observations.get(observations.size() - 1).getAreaRatioEnergy()),
                    (double) stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000d
            ));
            //System.out.println("post obs "+t);
        }
        stopWatch.stop();
        //prepare outcome
        return new Outcome(observations);
    }

    private static Grid<Boolean> mask(Robot<?> robot, int n) {
        List<BoundingBox> boxes = robot.getVoxels().values().stream()
                .filter(Objects::nonNull)
                .map(it.units.erallab.hmsrobots.core.objects.Voxel::boundingBox)
                .collect(Collectors.toList());
        double robotMinX = boxes.stream().mapToDouble(b -> b.min.x).min().orElseThrow(() -> new IllegalArgumentException("Empty robot"));
        double robotMaxX = boxes.stream().mapToDouble(b -> b.max.x).max().orElseThrow(() -> new IllegalArgumentException("Empty robot"));
        double robotMinY = boxes.stream().mapToDouble(b -> b.min.y).min().orElseThrow(() -> new IllegalArgumentException("Empty robot"));
        double robotMaxY = boxes.stream().mapToDouble(b -> b.max.y).max().orElseThrow(() -> new IllegalArgumentException("Empty robot"));
        //adjust box to make it squared
        if ((robotMaxY - robotMinY) < (robotMaxX - robotMinX)) {
            double d = (robotMaxX - robotMinX) - (robotMaxY - robotMinY);
            robotMaxY = robotMaxY + d / 2;
            robotMinY = robotMinY - d / 2;
        } else if ((robotMaxY - robotMinY) > (robotMaxX - robotMinX)) {
            double d = (robotMaxY - robotMinY) - (robotMaxX - robotMinX);
            robotMaxX = robotMaxX + d / 2;
            robotMinX = robotMinX - d / 2;
        }
        Grid<Boolean> mask = Grid.create(n, n, false);
        for (BoundingBox b : boxes) {
            int minXIndex = (int) Math.round((b.min.x - robotMinX) / (robotMaxX - robotMinX) * (double) (n - 1));
            int maxXIndex = (int) Math.round((b.max.x - robotMinX) / (robotMaxX - robotMinX) * (double) (n - 1));
            int minYIndex = (int) Math.round((b.min.y - robotMinY) / (robotMaxY - robotMinY) * (double) (n - 1));
            int maxYIndex = (int) Math.round((b.max.y - robotMinY) / (robotMaxY - robotMinY) * (double) (n - 1));
            for (int x = minXIndex; x <= maxXIndex; x++) {
                for (int y = minYIndex; y <= maxYIndex; y++) {
                    mask.set(x, y, true);
                }
            }
        }
        return mask;
    }

    private static Footprint footprint(Robot<?> robot, int n) {
        double robotMinX = Double.POSITIVE_INFINITY;
        double robotMaxX = Double.NEGATIVE_INFINITY;
        List<double[]> contacts = new ArrayList<>();
        for (it.units.erallab.hmsrobots.core.objects.Voxel v : robot.getVoxels().values()) {
            if (v == null) {
                continue;
            }
            double touchMinX = Double.POSITIVE_INFINITY;
            double touchMaxX = Double.NEGATIVE_INFINITY;
            for (Body body : v.getVertexBodies()) {
                AABB box = body.createAABB();
                robotMinX = Math.min(robotMinX, box.getMinX());
                robotMaxX = Math.max(robotMaxX, box.getMaxX());
                for (Body contactBody : body.getInContactBodies(false)) {
                    if (contactBody.getUserData().equals(Ground.class)) {
                        touchMinX = Math.min(touchMinX, box.getMinX());
                        touchMaxX = Math.max(touchMaxX, box.getMaxX());
                        contacts.add(new double[]{touchMinX, touchMaxX});
                    }
                }
            }
        }
        boolean[] mask = new boolean[n];
        for (double[] contact : contacts) {
            int minIndex = (int) Math.round((contact[0] - robotMinX) / (robotMaxX - robotMinX) * (double) (n - 1));
            int maxIndex = (int) Math.round((contact[1] - robotMinX) / (robotMaxX - robotMinX) * (double) (n - 1));
            for (int x = minIndex; x <= Math.min(maxIndex, n - 1); x++) {
                mask[x] = true;
            }
        }
        return new Footprint(mask);
    }

    private static double[][] randomTerrain(int n, double length, double peak, double borderHeight, Random random) {
        double[] xs = new double[n + 2];
        double[] ys = new double[n + 2];
        xs[0] = 0d;
        xs[n + 1] = length;
        ys[0] = borderHeight;
        ys[n + 1] = borderHeight;
        for (int i = 1; i < n + 1; i++) {
            xs[i] = 1 + (double) (i - 1) * (length - 2d) / (double) n;
            ys[i] = random.nextDouble() * peak;
        }
        return new double[][]{xs, ys};
    }

    public static double[][] createTerrain(String name) {
        String flat = "flat";
        String flatWithStart = "flatWithStart-(?<seed>[0-9]+)";
        String hilly = "hilly-(?<h>[0-9]+(\\.[0-9]+)?)-(?<w>[0-9]+(\\.[0-9]+)?)-(?<seed>[0-9]+)";
        String steppy = "steppy-(?<h>[0-9]+(\\.[0-9]+)?)-(?<w>[0-9]+(\\.[0-9]+)?)-(?<seed>[0-9]+)";
        String downhill = "downhill-(?<angle>[0-9]+(\\.[0-9]+)?)";
        String uphill = "uphill-(?<angle>[0-9]+(\\.[0-9]+)?)";
        Map<String, String> params;
        if ((params = Utils.params(flat, name)) != null) {
            return new double[][]{
                    new double[]{0, TERRAIN_BORDER_WIDTH, TERRAIN_LENGTH - TERRAIN_BORDER_WIDTH, TERRAIN_LENGTH},
                    new double[]{TERRAIN_BORDER_HEIGHT, 5, 5, TERRAIN_BORDER_HEIGHT}
            };
        }
        if ((params = Utils.params(flatWithStart, name)) != null) {
            Random random = new Random(Integer.parseInt(params.get("seed")));
            IntStream.range(0, random.nextInt(10) + 10).forEach(i -> random.nextDouble()); //it looks like that otherwise the 1st double of nextDouble() is always around 0.73...
            double angle = Math.PI / 18d * (random.nextDouble() * 2d - 1d);
            double startLength = it.units.erallab.hmsrobots.core.objects.Voxel.SIDE_LENGTH * 8d;
            return new double[][]{
                    new double[]{
                            0, TERRAIN_BORDER_WIDTH,
                            TERRAIN_BORDER_WIDTH + startLength, TERRAIN_LENGTH - TERRAIN_BORDER_WIDTH, TERRAIN_LENGTH
                    },
                    new double[]{
                            TERRAIN_BORDER_HEIGHT, 5 + startLength * Math.sin(angle),
                            5, 5, TERRAIN_BORDER_HEIGHT
                    }
            };
        }
        if ((params = Utils.params(hilly, name)) != null) {
            double h = Double.parseDouble(params.get("h"));
            double w = Double.parseDouble(params.get("w"));
            Random random = new Random(Integer.parseInt(params.get("seed")));
            List<Double> xs = new ArrayList<>(List.of(0d, TERRAIN_BORDER_WIDTH));
            List<Double> ys = new ArrayList<>(List.of(TERRAIN_BORDER_HEIGHT, 0d));
            while (xs.get(xs.size() - 1) < TERRAIN_LENGTH - TERRAIN_BORDER_WIDTH) {
                xs.add(xs.get(xs.size() - 1) + Math.max(1d, (random.nextGaussian() * 0.25 + 1) * w));
                ys.add(ys.get(ys.size() - 1) + random.nextGaussian() * h);
            }
            xs.addAll(List.of(xs.get(xs.size() - 1) + TERRAIN_BORDER_WIDTH));
            ys.addAll(List.of(TERRAIN_BORDER_HEIGHT));
            return new double[][]{
                    xs.stream().mapToDouble(d -> d).toArray(),
                    ys.stream().mapToDouble(d -> d).toArray()
            };
        }
        if ((params = Utils.params(steppy, name)) != null) {
            double h = Double.parseDouble(params.get("h"));
            double w = Double.parseDouble(params.get("w"));
            Random random = new Random(Integer.parseInt(params.get("seed")));
            List<Double> xs = new ArrayList<>(List.of(0d, TERRAIN_BORDER_WIDTH));
            List<Double> ys = new ArrayList<>(List.of(TERRAIN_BORDER_HEIGHT, 0d));
            while (xs.get(xs.size() - 1) < TERRAIN_LENGTH - TERRAIN_BORDER_WIDTH) {
                xs.add(xs.get(xs.size() - 1) + Math.max(1d, (random.nextGaussian() * 0.25 + 1) * w));
                xs.add(xs.get(xs.size() - 1) + 0.5d);
                ys.add(ys.get(ys.size() - 1));
                ys.add(ys.get(ys.size() - 1) + random.nextGaussian() * h);
            }
            xs.addAll(List.of(xs.get(xs.size() - 1) + TERRAIN_BORDER_WIDTH));
            ys.addAll(List.of(TERRAIN_BORDER_HEIGHT));
            return new double[][]{
                    xs.stream().mapToDouble(d -> d).toArray(),
                    ys.stream().mapToDouble(d -> d).toArray()
            };
        }
        if ((params = Utils.params(downhill, name)) != null) {
            double angle = Double.parseDouble(params.get("angle"));
            double dY = (TERRAIN_LENGTH - 2 * TERRAIN_BORDER_WIDTH) * Math.sin(angle / 180 * Math.PI);
            return new double[][]{
                    new double[]{0, TERRAIN_BORDER_WIDTH, TERRAIN_LENGTH - TERRAIN_BORDER_WIDTH, TERRAIN_LENGTH},
                    new double[]{TERRAIN_BORDER_HEIGHT + dY, 5 + dY, 5, TERRAIN_BORDER_HEIGHT}
            };
        }
        if ((params = Utils.params(uphill, name)) != null) {
            double angle = Double.parseDouble(params.get("angle"));
            double dY = (TERRAIN_LENGTH - 2 * TERRAIN_BORDER_WIDTH) * Math.sin(angle / 180 * Math.PI);
            return new double[][]{
                    new double[]{0, TERRAIN_BORDER_WIDTH, TERRAIN_LENGTH - TERRAIN_BORDER_WIDTH, TERRAIN_LENGTH},
                    new double[]{TERRAIN_BORDER_HEIGHT, 5, 5 + dY, TERRAIN_BORDER_HEIGHT + dY}
            };
        }
        throw new IllegalArgumentException(String.format("Unknown terrain name: %s", name));
    }
}
