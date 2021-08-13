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
import it.units.erallab.hmsrobots.core.sensors.immutable.SensorReading;

import java.util.Arrays;

public class Normalization implements Sensor, ReadingAugmenter {

  @JsonProperty
  private final Sensor sensor;
  private final Domain[] domains;

  @JsonCreator
  public Normalization(
      @JsonProperty("sensor") Sensor sensor
  ) {
    this.sensor = sensor;
    domains = new Domain[sensor.getDomains().length];
    Arrays.fill(domains, Domain.of(0d, 1d));
  }

  @Override
  public Domain[] getDomains() {
    return domains;
  }

  @Override
  public double[] sense(Voxel voxel, double t) {
    double[] innerValues = sensor.sense(voxel, t);
    double[] values = new double[innerValues.length];
    for (int i = 0; i < values.length; i++) {
      Domain d = sensor.getDomains()[i];
      values[i] = Math.min(Math.max((innerValues[i] - d.getMin()) / (d.getMax() - d.getMin()), 0d), 1d);
    }
    return values;
  }

  @Override
  public String toString() {
    return "Normalization{" +
        "sensor=" + sensor +
        '}';
  }

  @Override
  public SensorReading augment(SensorReading reading, Voxel voxel) {
    if (sensor instanceof ReadingAugmenter) {
      return ((ReadingAugmenter) sensor).augment(reading, voxel);
    }
    return reading;
  }

}
