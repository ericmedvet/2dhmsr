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
import org.dyn4j.dynamics.Body;

import java.util.List;

public class Touch implements Sensor {
  private final Domain[] domains = new Domain[]{
      Domain.build(0d, 1d)
  };

  @Override
  public Domain[] domains() {
    return domains;
  }

  @Override
  public double[] sense(Voxel voxel, double t) {
    for (Body vertexBody : voxel.getVertexBodies()) {
      List<Body> inContactBodies = vertexBody.getInContactBodies(false);
      for (Body inContactBody : inContactBodies) {
        Object userData = inContactBody.getUserData();
        if (userData == null) {
          return new double[]{1d};
        } else if (userData != vertexBody.getUserData()) {
          return new double[]{1d};
        }
      }
    }
    return new double[]{0d};
  }
}
