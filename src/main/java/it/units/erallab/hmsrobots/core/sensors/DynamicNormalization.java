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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.objects.Voxel;

import java.util.Arrays;
import java.util.TreeMap;

public class DynamicNormalization implements Sensor {

  @JsonProperty
  private final Sensor sensor;
  @JsonProperty
  private final double interval;

  private final TreeMap<Double, double[]> readings;
  private final Domain[] domains;

  @JsonCreator
  public DynamicNormalization(
      @JsonProperty("sensor") Sensor sensor,
      @JsonProperty("interval") double interval
  ) {
    this.sensor = sensor;
    this.interval = interval;
    domains = new Domain[sensor.domains().length];
    readings = new TreeMap<>();
    Arrays.fill(domains, Domain.of(0d, 1d));
  }

  @Override
  public Domain[] domains() {
    return domains;
  }

  @Override
  public double[] sense(Voxel voxel, double t) {
    double[] currentReadings = sensor.sense(voxel, t);
    readings.put(t, currentReadings);
    double t0 = readings.firstKey();
    while (t0 < (t - interval)) {
      readings.remove(t0);
      t0 = readings.firstKey();
    }
    double[] mins = new double[currentReadings.length];
    double[] maxs = new double[currentReadings.length];
    Arrays.fill(mins, Double.POSITIVE_INFINITY);
    Arrays.fill(maxs, Double.NEGATIVE_INFINITY);
    for (double[] pastReadings : readings.values()) {
      for (int i = 0; i < currentReadings.length; i++) {
        mins[i] = Math.min(mins[i], pastReadings[i]);
        maxs[i] = Math.max(maxs[i], pastReadings[i]);
      }
    }
    double[] values = new double[currentReadings.length];
    for (int i = 0; i < values.length; i++) {
      values[i] = Math.min(Math.max((currentReadings[i] - mins[i]) / (maxs[i] - mins[i]), 0d), 1d);
    }
    return values;
  }

}
