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

import java.io.Serializable;

public interface Sensor extends Serializable {

  class Domain implements Serializable {
    private final double min;
    private final double max;

    private Domain(double min, double max) {
      this.min = min;
      this.max = max;
    }

    public static Domain build(double min, double max) {
      return new Domain(min, max);
    }

    public double getMin() {
      return min;
    }

    public double getMax() {
      return max;
    }
  }

  Domain[] domains();

  double[] sense(Voxel voxel, double t);
}
