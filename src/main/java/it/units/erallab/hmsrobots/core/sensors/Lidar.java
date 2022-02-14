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
package it.units.erallab.hmsrobots.core.sensors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.core.snapshots.LidarReadings;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.util.DoubleRange;
import org.apache.commons.lang3.ArrayUtils;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.DetectFilter;
import org.dyn4j.world.result.RaycastResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;

public class Lidar extends AbstractSensor {
  private final static DetectFilter<Body, BodyFixture> FILTER = new DetectFilter<>(
      true,
      true,
      f -> {
        if (f == null)
          return true;
        return !(f instanceof Voxel.ParentFilter) && !(f instanceof Voxel.RobotFilter);
      }
  );
  @JsonProperty
  private final double rayLength;
  @JsonProperty
  private final double[] rayDirections;

  @JsonCreator
  public Lidar(
      @JsonProperty("rayLength") double rayLength,
      @JsonProperty("rayDirections") double... rayDirections
  ) {
    super(Collections.nCopies(rayDirections.length, DoubleRange.of(0, rayLength)).toArray(DoubleRange[]::new));
    this.rayLength = rayLength;
    this.rayDirections = rayDirections;
  }

  public Lidar(double rayLength, Map<Side, Integer> raysPerSide) {
    this(
        rayLength,
        Arrays.stream(raysPerSide.entrySet().stream()
            .map(e -> sampleRangeWithRays(e.getValue(), e.getKey().startAngle, e.getKey().endAngle))
            .reduce(new double[]{}, ArrayUtils::addAll)).distinct().toArray()
    );
  }

  public enum Side {

    N(Math.PI / 4d, Math.PI * 3d / 4d),

    E(Math.PI * -1d / 4d, Math.PI / 4d),

    S(Math.PI * 5d / 4d, Math.PI * 7d / 4d),

    W(Math.PI * 3d / 4d, Math.PI * 5d / 4d);

    private final double startAngle;
    private final double endAngle;

    Side(double startAngle, double endAngle) {
      this.startAngle = startAngle;
      this.endAngle = endAngle;
    }

    public double getEndAngle() {
      return endAngle;
    }

    public double getStartAngle() {
      return startAngle;
    }

  }

  private static double[] sampleRangeWithRays(int numberOfRays, double startAngle, double endAngle) {
    return numberOfRays == 1 ?
        new double[]{(endAngle - startAngle) / 2} :
        DoubleStream.iterate(startAngle, d -> d + (endAngle - startAngle) / (numberOfRays - 1))
            .limit(numberOfRays)
            .toArray();
  }

  @Override
  public double[] sense(double t) {
    return Arrays.stream(rayDirections).map(rayDirection -> {
      Point2 center = voxel.center();
      Ray ray = new Ray(new Vector2(center.x(), center.y()), rayDirection + voxel.getAngle());
      List<RaycastResult<Body, BodyFixture>> results = voxel.getWorld().raycast(ray, rayLength, FILTER);
      return results.stream().mapToDouble(r -> r.getRaycast().getDistance()).min().orElse(rayLength);
    }).toArray();
  }

  @Override
  public Snapshot getSnapshot() {
    return new Snapshot(
        new LidarReadings(
            Arrays.copyOf(readings, readings.length),
            Arrays.copyOf(domains, domains.length),
            voxel.getAngle(),
            Arrays.copyOf(rayDirections, rayDirections.length)
        ),
        getClass()
    );
  }

  @Override
  public String toString() {
    return "Lidar{" +
        "rayLength=" + rayLength +
        ", rayDirections=" + Arrays.toString(rayDirections) +
        '}';
  }
}
