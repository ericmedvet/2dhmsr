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
import org.dyn4j.collision.Filter;
import org.dyn4j.dynamics.RaycastResult;
import org.dyn4j.geometry.Ray;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

public class Lidar extends AbstractSensor {
  public enum Side {

    N(Math.PI * 1d / 4d, Math.PI * 3d / 4d),

    E(Math.PI * -1d / 4d, Math.PI * 1d / 4d),

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
      // make sure the given filter is not null
      if (f == null) return true;
      // check the type
      return !(f instanceof Voxel.ParentFilter) && !(f instanceof Voxel.RobotFilter);
      // if its not of right type always return true
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
        raysPerSide.entrySet().stream()
            .map(e -> DoubleStream.iterate(
                    e.getKey().getStartAngle() + (e.getKey().getEndAngle() - e.getKey().getStartAngle()) / ((double) e.getValue()) / 2d,
                    d -> d + (e.getKey().getEndAngle() - e.getKey().getStartAngle()) / ((double) e.getValue())
                )
                .limit(e.getValue())
                .boxed()
                .collect(Collectors.toList()))
            .reduce((l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()))
            .orElse(List.of(0d))
            .stream()
            .mapToDouble(d -> d)
            .toArray()
    );
  }

  @Override
  public double[] sense(double t) {
    double[] rayHits = new double[rayDirections.length];
    // List of objects the ray intersects with
    List<RaycastResult> results = new ArrayList<>();
    for (int rayIdx = 0; rayIdx < rayDirections.length; rayIdx++) {
      double direction = rayDirections[rayIdx];
      // take into account rotation angle
      direction += voxel.getAngle();
      // Create a ray from the given start point towards the given direction
      Ray ray = new Ray(voxel.getCenter(), direction);
      results.clear();
      // if the all flag is false, the results list will contain the closest result (if any)
      voxel.getWorld().raycast(ray, rayLength, new RaycastFilter(), true, false, false, results);
      if (results.isEmpty()) {
        rayHits[rayIdx] = rayLength;
      } else {
        rayHits[rayIdx] = results.get(0).getRaycast().getDistance();
      }
    }
    return rayHits;
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
