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

public class Normalization implements Sensor, Configurable<Normalization> {

  @ConfigurableField
  private final Sensor sensor;
  private final Domain[] domains = new Domain[]{
      Domain.build(0d, 1d)
  };

  public Normalization(Sensor sensor) {
    this.sensor = sensor;
  }

  @Override
  public Domain[] domains() {
    return sensor.domains();
  }

  @Override
  public double[] sense(Voxel voxel, double t) {
    double[] innerValues = sensor.sense(voxel, t);
    double[] values = new double[innerValues.length];
    for (int i = 0; i < values.length; i++) {
      Domain d = sensor.domains()[i];
      values[i] = Math.min(Math.max((innerValues[i] - d.getMin()) / (d.getMax() - d.getMin()), 0d), 1d);
    }
    return values;
  }

}
