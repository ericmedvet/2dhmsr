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
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.core.snapshots.LidarReadings;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.util.Domain;
import org.apache.commons.lang3.ArrayUtils;
import org.dyn4j.collision.Filter;
import org.dyn4j.dynamics.RaycastResult;
import org.dyn4j.geometry.Ray;

import java.io.Serializable;
import java.util.*;
import java.util.stream.DoubleStream;

public class Lidar extends AbstractSensor {
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

    public double getStartAngle() {
      return startAngle;
    }

    public double getEndAngle() {
      return endAngle;
    }

  }

  public static class RaycastFilter implements Filter, Serializable {

    @Override
    public boolean isAllowed(Filter f) {
      if (f == null) return true;
      return !(f instanceof Voxel.ParentFilter) && !(f instanceof Voxel.RobotFilter);
    }

  }

  @JsonProperty
  private final double rayLength;
  @JsonProperty
  private final double[] rayDirections;

  @JsonCreator
  public Lidar(
      @JsonProperty("rayLength") double rayLength,
      @JsonProperty("rayDirections") double... rayDirections
  ) {
    super(Collections.nCopies(rayDirections.length, Domain.of(0, rayLength)).toArray(Domain[]::new));
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

  private static double[] sampleRangeWithRays(int numberOfRays, double startAngle, double endAngle) {
    return numberOfRays == 1 ?
        new double[]{(endAngle - startAngle) / 2} :
        DoubleStream.iterate(startAngle, d -> d + (endAngle - startAngle) / (numberOfRays - 1)).limit(numberOfRays).toArray();
  }

  @Override
  public double[] sense(double t) {
    List<RaycastResult> results = new ArrayList<>();
    return Arrays.stream(rayDirections).map(rayDirection -> {
      Ray ray = new Ray(voxel.getCenter(), rayDirection + voxel.getAngle());
      results.clear();
      voxel.getWorld().raycast(ray, rayLength, new RaycastFilter(), true, false, false, results);
      return results.isEmpty() ? rayLength : results.get(0).getRaycast().getDistance();
    }).toArray();
  }

  @Override
  public Snapshot getSnapshot() {
    return new Snapshot(
        new LidarReadings(
            Arrays.copyOf(readings, readings.length),
            Arrays.stream(domains).map(d -> Domain.of(d.getMin(), d.getMax())).toArray(Domain[]::new),
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
