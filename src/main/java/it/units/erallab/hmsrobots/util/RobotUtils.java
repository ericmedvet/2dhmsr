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

package it.units.erallab.hmsrobots.util;

import it.units.erallab.hmsrobots.core.objects.BreakableVoxel;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.core.sensors.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static it.units.erallab.hmsrobots.util.Utils.params;

/**
 * @author eric on 2020/12/22 for 2dhmsr
 */
public class RobotUtils {

  private final static Map<String, BiFunction<Double, Double, Sensor>> PREDEFINED_SENSORS = new TreeMap<>(Map.ofEntries(
      Map.entry("t", (x, y) -> new Average(new Touch(), 0.25)),
      Map.entry("a", (x, y) -> new SoftNormalization(new AreaRatio())),
      Map.entry("r", (x, y) -> new SoftNormalization(new Angle())),
      Map.entry("vx", (x, y) -> new SoftNormalization(new Average(new Velocity(true, 8d, Velocity.Axis.X), 0.5))),
      Map.entry("vy", (x, y) -> new SoftNormalization(new Average(new Velocity(true, 8d, Velocity.Axis.Y), 0.5))),
      Map.entry("vxy", (x, y) -> new SoftNormalization(new Average(new Velocity(true, 8d, Velocity.Axis.X, Velocity.Axis.Y), 0.5))),
      Map.entry("ax", (x, y) -> new SoftNormalization(new Average(new Derivative(new Velocity(true, 4d, Velocity.Axis.X)), 0.5))),
      Map.entry("ay", (x, y) -> new SoftNormalization(new Average(new Derivative(new Velocity(true, 4d, Velocity.Axis.Y)), 0.5))),
      Map.entry("axy", (x, y) -> new SoftNormalization(new Average(new Derivative(new Velocity(true, 4d, Velocity.Axis.X, Velocity.Axis.Y)), 0.5))),
      Map.entry("px", (x, y) -> new Constant(x)),
      Map.entry("py", (x, y) -> new Constant(y)),
      Map.entry("m", (x, y) -> new Malfunction()),
      Map.entry("cpg", (x, y) -> new Normalization(new TimeFunction(t -> Math.sin(2 * Math.PI * -1 * t), -1, 1))),
      Map.entry("l5", (x, y) -> new Lidar(10d, Map.of(lidarSide(x, y), 5))),
      Map.entry("l1", (x, y) -> new Lidar(10d, Map.of(lidarSide(x, y), 1)))
  ));

  private static Lidar.Side lidarSide(double x, double y) {
    if (y > x && y > (1 - x)) {
      return Lidar.Side.N;
    }
    if (y >= x && y <= (1 - x)) {
      return Lidar.Side.W;
    }
    if (y < x && y < (1 - x)) {
      return Lidar.Side.S;
    }
    return Lidar.Side.E;
  }


  private RobotUtils() {
  }

  public static UnaryOperator<Robot<?>> buildRobotTransformation(String name, Random externalRandom) {
    String breakable = "breakable-(?<triggerType>time|area)-(?<thresholdMean>\\d+(\\.\\d+)?)/(?<thresholdStDev>\\d+(\\.\\d+)?)-(?<restTimeMean>\\d+(\\.\\d+)?)/(?<restTimeStDev>\\d+(\\.\\d+)?)-(?<seed>\\d+|rnd)";
    String broken = "broken-(?<ratio>\\d+(\\.\\d+)?)-(?<seed>\\d+|rnd)";
    String identity = "identity";
    Map<String, String> params;
    if ((params = params(identity, name)) != null) {
      return UnaryOperator.identity();
    }
    if ((params = params(breakable, name)) != null) {
      String type = params.get("triggerType");
      double thresholdMean = Double.parseDouble(params.get("thresholdMean"));
      double thresholdStDev = Double.parseDouble(params.get("thresholdStDev"));
      double restoreTimeMean = Double.parseDouble(params.get("restTimeMean"));
      double restoreTimeStDev = Double.parseDouble(params.get("restTimeStDev"));
      Random random;
      if (!params.get("seed").equals("rnd")) {
        random = new Random(Long.parseLong(params.get("seed")));
      } else {
        random = externalRandom;
      }
      return new UnaryOperator<Robot<?>>() {
        @Override
        @SuppressWarnings("unchecked")
        public Robot<?> apply(Robot<?> robot) {
          return new Robot<>(
              ((Robot<SensingVoxel>) robot).getController(),
              Grid.create(SerializationUtils.clone((Grid<SensingVoxel>) robot.getVoxels()), v -> v == null ? null : new BreakableVoxel(
                  v.getSensors(),
                  random.nextInt(),
                  Map.of(
                      BreakableVoxel.ComponentType.ACTUATOR, Set.of(BreakableVoxel.MalfunctionType.FROZEN)
                  ),
                  Map.of(
                      BreakableVoxel.MalfunctionTrigger.valueOf(type.toUpperCase()),
                      random.nextGaussian() * thresholdStDev + thresholdMean
                  ),
                  random.nextGaussian() * restoreTimeStDev + restoreTimeMean
              ))
          );
        }
      };
    }
    if ((params = params(broken, name)) != null) {
      double ratio = Double.parseDouble(params.get("ratio"));
      Random random;
      if (!params.get("seed").equals("rnd")) {
        random = new Random(Long.parseLong(params.get("seed")));
      } else {
        random = externalRandom;
      }
      return new UnaryOperator<Robot<?>>() {
        @Override
        @SuppressWarnings("unchecked")
        public Robot<?> apply(Robot<?> robot) {
          return new Robot<>(
              ((Robot<SensingVoxel>) robot).getController(),
              Grid.create(SerializationUtils.clone((Grid<SensingVoxel>) robot.getVoxels()), v -> v == null ? null : random.nextDouble() > ratio ? v : new BreakableVoxel(
                  v.getSensors(),
                  random.nextInt(),
                  Map.of(BreakableVoxel.ComponentType.ACTUATOR, Set.of(BreakableVoxel.MalfunctionType.FROZEN)),
                  Map.of(BreakableVoxel.MalfunctionTrigger.TIME, 0d),
                  Double.POSITIVE_INFINITY
              ))
          );
        }
      };
    }
    throw new IllegalArgumentException(String.format("Unknown transformation name: %s", name));
  }

  public static Function<Grid<Boolean>, Grid<? extends SensingVoxel>> buildSensorizingFunction(String name) {
    String spineTouch = "spinedTouch-(?<cpg>[tf])-(?<malfunction>[tf])-(?<noiseSigma>\\d+(\\.\\d+)?)";
    String spineTouchSighted = "spinedTouchSighted-(?<cpg>[tf])-(?<malfunction>[tf])-(?<noiseSigma>\\d+(\\.\\d+)?)";
    String uniform = "uniform-(?<sensors>(" + String.join("|", PREDEFINED_SENSORS.keySet()) + ")(\\+(" + String.join("|", PREDEFINED_SENSORS.keySet()) + "))*)-(?<noiseSigma>\\d+(\\.\\d+)?)";
    String uniformAll = "uniformAll-(?<noiseSigma>\\d+(\\.\\d+)?)";
    String empty = "empty";
    Map<String, String> params;
    if ((params = params(spineTouch, name)) != null) {
      final Map<String, String> pars = params;
      double noiseSigma = Double.parseDouble(params.get("noiseSigma"));
      return body -> Grid.create(body.getW(), body.getH(),
          (x, y) -> {
            if (!body.get(x, y)) {
              return null;
            }
            return new SensingVoxel(
                Utils.ofNonNull(
                    sensor("a", x, y, body),
                    sensor("m", x, y, body, pars.get("malfunction").equals("t")),
                    sensor("t", x, y, body, y == 0),
                    sensor("vxy", x, y, body, y == body.getH() - 1),
                    sensor("cpg", x, y, body, x == body.getW() - 1 && y == body.getH() - 1 && pars.get("cpg").equals("t"))
                ).stream()
                    .map(s -> noiseSigma == 0 ? s : new Noisy(s, noiseSigma, 0))
                    .collect(Collectors.toList())
            );
          }
      );
    }
    if ((params = params(spineTouchSighted, name)) != null) {
      final Map<String, String> pars = params;
      double noiseSigma = Double.parseDouble(params.get("noiseSigma"));
      return body -> Grid.create(body.getW(), body.getH(),
          (x, y) -> {
            if (!body.get(x, y)) {
              return null;
            }
            return new SensingVoxel(
                Utils.ofNonNull(
                    sensor("a", x, y, body),
                    sensor("m", x, y, body, pars.get("malfunction").equals("t")),
                    sensor("t", x, y, body, y == 0),
                    sensor("vxy", x, y, body, y == body.getH() - 1),
                    sensor("cpg", x, y, body, x == body.getW() - 1 && y == body.getH() - 1 && pars.get("cpg").equals("t")),
                    sensor("l5", x, y, body, x == body.getW() - 1)
                ).stream()
                    .map(s -> noiseSigma == 0 ? s : new Noisy(s, noiseSigma, 0))
                    .collect(Collectors.toList())
            );
          }
      );
    }
    if ((params = params(uniform, name)) != null) {
      final Map<String, String> pars = params;
      double noiseSigma = Double.parseDouble(params.get("noiseSigma"));
      return body -> Grid.create(body.getW(), body.getH(), (x, y) -> !body.get(x, y) ? null : new SensingVoxel(
          Arrays.stream(pars.get("sensors").split("\\+"))
              .map(n -> sensor(n, x, y, body))
              .map(s -> noiseSigma == 0 ? s : new Noisy(s, noiseSigma, 0))
              .collect(Collectors.toList())
      ));
    }
    if ((params = params(uniformAll, name)) != null) {
      final Map<String, String> pars = params;
      double noiseSigma = Double.parseDouble(params.get("noiseSigma"));
      return body -> Grid.create(body.getW(), body.getH(), (x, y) -> !body.get(x, y) ? null : new SensingVoxel(
          PREDEFINED_SENSORS.keySet().stream()
              .map(n -> sensor(n, x, y, body))
              .map(s -> noiseSigma == 0 ? s : new Noisy(s, noiseSigma, 0))
              .collect(Collectors.toList())
      ));
    }
    if ((params = params(empty, name)) != null) {
      return body -> Grid.create(body.getW(), body.getH(), (x, y) -> !body.get(x, y) ? null : new SensingVoxel(List.of()));
    }
    throw new IllegalArgumentException(String.format("Unknown sensorizing function name: %s", name));
  }

  public static Sensor sensor(String name, int x, int y, Grid<Boolean> body) {
    return sensor(name, x, y, body, true);
  }

  public static Sensor sensor(String name, int x, int y, Grid<Boolean> body, boolean condition) {
    if (!condition) {
      return null;
    }
    return PREDEFINED_SENSORS.get(name).apply((double) x / ((double) body.getW() - 1d), (double) y / ((double) body.getH() - 1d));
  }

  public static Grid<Boolean> buildShape(String name) {
    String box = "(box|worm)-(?<w>\\d+)x(?<h>\\d+)";
    String biped = "biped-(?<w>\\d+)x(?<h>\\d+)";
    String tripod = "tripod-(?<w>\\d+)x(?<h>\\d+)";
    String ball = "ball-(?<d>\\d+)";
    Map<String, String> params;
    if ((params = params(box, name)) != null) {
      int w = Integer.parseInt(params.get("w"));
      int h = Integer.parseInt(params.get("h"));
      return Grid.create(w, h, true);
    }
    if ((params = params(biped, name)) != null) {
      int w = Integer.parseInt(params.get("w"));
      int h = Integer.parseInt(params.get("h"));
      return Grid.create(w, h, (x, y) -> !(y < h / 2 && x >= w / 4 && x < w * 3 / 4));
    }
    if ((params = params(tripod, name)) != null) {
      int w = Integer.parseInt(params.get("w"));
      int h = Integer.parseInt(params.get("h"));
      return Grid.create(w, h, (x, y) -> !(y < h / 2 && x != 0 && x != w - 1 && x != w / 2));
    }
    if ((params = params(ball, name)) != null) {
      int d = Integer.parseInt(params.get("d"));
      return Grid.create(
          d, d,
          (x, y) -> Math.round(Math.sqrt((x - (d - 1) / 2d) * (x - (d - 1) / 2d) + (y - (d - 1) / 2d) * (y - (d - 1) / 2d))) <= (int) Math.floor(d / 2d)
      );
    }
    throw new IllegalArgumentException(String.format("Unknown body name: %s", name));
  }

}
