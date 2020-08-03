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
package it.units.erallab.hmsrobots.core.sensors;

import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.core.sensors.immutable.SensorReading;
import org.dyn4j.collision.Filter;
import org.dyn4j.dynamics.RaycastResult;
import org.dyn4j.geometry.Ray;

import java.io.Serializable;
import java.util.*;

public class Lidar implements Sensor, ReadingAugmenter {
  public enum Side {
    // between -180 and 180 degrees
    N(120, 60),
    E(30, -30),
    S(-120, -60),
    W(-150, 150);

    // in degrees
    private final int startAngle;
    private final int endAngle;

    Side(int startAngle, int endAngle) {
      this.startAngle = startAngle;
      this.endAngle = endAngle;
    }

    public double getStartAngle() {
      return startAngle;
    }

    public double getEndAngle() {
      return endAngle;
    }

    public double angleDifference() {
      double angleDiff = Math.abs(endAngle - startAngle);
      if (angleDiff > 180) {
        angleDiff = 360 - angleDiff;
      }
      return angleDiff;
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

  private final double rayLength;
  private final double[] rayDirections;
  private final Domain[] domains;

  public Lidar(double rayLength, LinkedHashMap<Side, Integer> raysPerSide) {
    this.rayLength = rayLength;
    int numRays = 0;
    for (int rays : raysPerSide.values()) {
      numRays += rays;
    }
    rayDirections = new double[numRays];
    int rayIdx = 0;
    for (Map.Entry<Side, Integer> entry : raysPerSide.entrySet()) {
      Side side = entry.getKey();
      Integer numSideRays = entry.getValue();
      for (int i = 0; i < numSideRays; i++) {
        double direction = Math.toRadians(side.getStartAngle() - i * (Math.abs(side.angleDifference()) / numSideRays));
        // clip direction in [-π, π]
        if (Math.abs(direction) > Math.PI) {
          direction = 2 * Math.PI - Math.abs(direction);
        }
        rayDirections[rayIdx] = direction;
        rayIdx++;
      }
    }
    domains = new Domain[numRays];
    Arrays.fill(domains, Domain.build(0d, 1d));
  }

  public Lidar(double rayLength, LinkedHashMap<Side, Integer> raysPerSide, double[] rayDir) {
    this.rayLength = rayLength;
    int numRays = 0;
    for (int rays : raysPerSide.values()) {
      numRays += rays;
    }

    int rayIdx = 0;
    rayDirections = new double[numRays];
    for (Map.Entry<Side, Integer> entry : raysPerSide.entrySet()) {
      Side side = entry.getKey();
      Integer numSideRays = entry.getValue();
      for (int i = 0; i < numSideRays; i++) {
        double direction = Math.toRadians(side.getStartAngle() - (side.startAngle - rayDir[i]));
        // clip direction in [-π, π]
        if (Math.abs(direction) > Math.PI) {
          direction = 2 * Math.PI - Math.abs(direction);
        }
        rayDirections[rayIdx] = direction;
        rayIdx++;
      }
    }
    domains = new Domain[numRays];
    Arrays.fill(domains, Domain.build(0d, 1d));
  }

  @Override
  public Domain[] domains() {
    return domains;
  }

  @Override
  public double[] sense(Voxel voxel, double t) {
    double[] rayHits = new double[rayDirections.length];
    // List of objects the ray intersects with
    List<RaycastResult> results = new ArrayList<>();

    for (int rayIdx = 0; rayIdx < rayDirections.length; rayIdx++) {
      double direction = rayDirections[rayIdx];
      // take into account rotation angle
      direction += voxel.getAngle();
      // clip direction in [-π, π]
      if (Math.abs(direction) > Math.PI) {
        direction = 2 * Math.PI - Math.abs(direction);
      }
      // Create a ray from the given start point towards the given direction
      Ray ray = new Ray(voxel.getCenter(), direction);
      results.clear();
      // if the all flag is false, the results list will contain the closest result (if any)
      voxel.getWorld().raycast(ray, rayLength, new RaycastFilter(), true, false, false, results);
      if (results.isEmpty()) {
        rayHits[rayIdx] = 1d;
      } else {
        rayHits[rayIdx] = results.get(0).getRaycast().getDistance() / rayLength;
      }
    }

    return rayHits;
  }

  @Override
  public SensorReading augment(SensorReading reading, Voxel voxel) {
    return new it.units.erallab.hmsrobots.core.sensors.immutable.Lidar(
        reading.getValues(),
        reading.getDomains(),
        reading.getSensorIndex(),
        reading.getnOfSensors(),
        voxel.getAngle(),
        rayLength,
        rayDirections.clone()
    );
  }
}
