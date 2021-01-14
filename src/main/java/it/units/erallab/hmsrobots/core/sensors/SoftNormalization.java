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
import it.units.erallab.hmsrobots.core.sensors.immutable.SensorReading;

import java.util.Arrays;

public class SoftNormalization implements Sensor, ReadingAugmenter {

  @JsonProperty
  private final Sensor sensor;
  private final Sensor.Domain[] domains;

  @JsonCreator
  public SoftNormalization(
      @JsonProperty("sensor") Sensor sensor
  ) {
    this.sensor = sensor;
    domains = new Sensor.Domain[sensor.domains().length];
    Arrays.fill(domains, Sensor.Domain.of(0d, 1d));
  }

  @Override
  public Sensor.Domain[] domains() {
    return domains;
  }

  @Override
  public double[] sense(Voxel voxel, double t) {
    double[] innerValues = sensor.sense(voxel, t);
    double[] values = new double[innerValues.length];
    for (int i = 0; i < values.length; i++) {
      Sensor.Domain d = sensor.domains()[i];
      double v = (innerValues[i] - d.getMin()) / (d.getMax() - d.getMin());
      //tanh(((x*2)-1)*2)/2+1/2
      values[i] = Math.tanh(((v * 2d) - 1d) * 2d) / 2d + 0.5d;
    }
    return values;
  }

  @Override
  public String toString() {
    return "SoftNormalization{" +
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
