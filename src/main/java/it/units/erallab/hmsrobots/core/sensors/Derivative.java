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
package it.units.erallab.hmsrobots.core.sensors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.objects.Voxel;

import java.util.Arrays;

public class Derivative implements Sensor {

  private final static double DOMAIN_MULTIPLIER = 10d;

  @JsonProperty
  private final Sensor sensor;

  private final Domain[] domains;
  private double lastT;
  private double[] lastReadings;

  @JsonCreator
  public Derivative(
      @JsonProperty("sensor") Sensor sensor
  ) {
    this.sensor = sensor;
    domains = Arrays.stream(sensor.getDomains())
        .map(d -> Domain.of(
            -DOMAIN_MULTIPLIER * (Math.abs(d.getMax() - d.getMin())),
            DOMAIN_MULTIPLIER * (Math.abs(d.getMax() - d.getMin()))
        ))
        .toArray(Domain[]::new);
  }

  @Override
  public Domain[] getDomains() {
    return domains;
  }

  @Override
  public double[] sense(Voxel voxel, double t) {
    double[] currentReadings = sensor.sense(voxel, t);
    double[] diffs = new double[currentReadings.length];
    if (lastReadings != null) {
      for (int i = 0; i < diffs.length; i++) {
        diffs[i] = (currentReadings[i] - lastReadings[i]) / (t - lastT);
      }
    }
    lastT = t;
    lastReadings = currentReadings;
    return diffs;
  }

  @Override
  public String toString() {
    return "Derivative{" +
        "sensor=" + sensor +
        '}';
  }
}
