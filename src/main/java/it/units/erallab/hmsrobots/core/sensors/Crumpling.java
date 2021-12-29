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

import it.units.erallab.hmsrobots.util.DoubleRange;

public class Crumpling extends AbstractSensor {
  private final static double THRESHOLD = 0.2d;

  private final static DoubleRange[] DOMAINS = new DoubleRange[]{
      DoubleRange.of(0d, 1d)
  };

  public Crumpling() {
    super(DOMAINS);
  }

  @Override
  public double[] sense(double t) {
    double c = 0d;
    for (int i = 0; i < voxel.getVertexBodies().length; i++) {
      for (int j = i + 1; j < voxel.getVertexBodies().length; j++) {
        double d = voxel.getVertexBodies()[i].getWorldCenter().distance(voxel.getVertexBodies()[j].getWorldCenter());
        if (d < voxel.getSideLength() * THRESHOLD) {
          c = c + 1d;
        }
      }
    }
    return new double[]{2d * c / (double) (voxel.getVertexBodies().length * (voxel.getVertexBodies().length - 1))};
  }

}
