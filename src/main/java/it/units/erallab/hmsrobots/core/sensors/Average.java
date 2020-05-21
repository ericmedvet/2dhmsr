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
import it.units.erallab.hmsrobots.util.Configurable;
import it.units.erallab.hmsrobots.util.ConfigurableField;

import java.util.TreeMap;

public class Average implements Sensor, Configurable<Average> {

  @ConfigurableField
  private final Sensor sensor;
  @ConfigurableField
  private final double interval;
  private final TreeMap<Double, double[]> readings;

  public Average(Sensor sensor, double interval) {
    this.sensor = sensor;
    this.interval = interval;
    readings = new TreeMap<>();
  }

  @Override
  public Domain[] domains() {
    return sensor.domains();
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
    double[] sums = new double[currentReadings.length];
    for (double[] pastReadings : readings.values()) {
      for (int i = 0; i < sums.length; i++) {
        sums[i] = sums[i] + pastReadings[i];
      }
    }
    for (int i = 0; i < sums.length; i++) {
      sums[i] = sums[i] / (double) readings.size();
    }
    return sums;
  }

}
