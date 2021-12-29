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

import it.units.erallab.hmsrobots.core.objects.Ground;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.util.DoubleRange;
import org.dyn4j.dynamics.Body;

import java.util.List;

public class Touch extends AbstractSensor {
  private final static DoubleRange[] DOMAINS = new DoubleRange[]{
      DoubleRange.of(0d, 1d)
  };

  public Touch() {
    super(DOMAINS);
  }

  public static boolean isTouching(Voxel voxel) {
    for (Body vertexBody : voxel.getVertexBodies()) {
      List<Body> inContactBodies = voxel.getWorld().getInContactBodies(vertexBody, false);
      for (Body inContactBody : inContactBodies) {
        Object userData = inContactBody.getUserData();
        if (userData == null) {
          return true;
        } else if (userData != vertexBody.getUserData()) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean isTouchingGround(Voxel voxel) {
    for (Body vertexBody : voxel.getVertexBodies()) {
      List<Body> inContactBodies = voxel.getWorld().getInContactBodies(vertexBody, false);
      for (Body inContactBody : inContactBodies) {
        if ((inContactBody.getUserData() != null) && (inContactBody.getUserData().equals(Ground.class))) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public double[] sense(double t) {
    return isTouching(voxel) ? new double[]{1d} : new double[]{0d};
  }
}
