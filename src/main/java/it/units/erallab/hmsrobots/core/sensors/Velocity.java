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
import org.dyn4j.geometry.Vector2;

import java.util.Arrays;
import java.util.EnumSet;

public class Velocity implements Sensor, Configurable<Velocity> {
  public enum Axis {X, Y}

  @ConfigurableField
  private final boolean rotated;
  @ConfigurableField
  private final EnumSet<Axis> axes;
  private final double maxVelocityNorm;
  private final Domain[] domains;

  public Velocity(boolean rotated, double maxVelocityNorm, Axis... axes) {
    this(
        rotated,
        maxVelocityNorm,
        axes.length > 0 ? EnumSet.of(axes[0], axes) : EnumSet.noneOf(Axis.class)
    );
  }

  public Velocity(boolean rotated, double maxVelocityNorm, EnumSet<Axis> axes) {
    this.rotated = rotated;
    this.maxVelocityNorm = maxVelocityNorm;
    this.axes = axes;
    domains = new Domain[this.axes.size()];
    Arrays.fill(domains, Domain.build(-maxVelocityNorm, maxVelocityNorm));
  }

  @Override
  public Domain[] domains() {
    return domains;
  }

  @Override
  public double[] sense(Voxel voxel, double t) {
    double[] values = new double[domains.length];
    int c = 0;
    Vector2 velocity = voxel.getLinearVelocity();
    double angle = 0d;
    if (rotated) {
      angle = voxel.getAngle();
    }
    if (axes.contains(Axis.X)) {
      if (!rotated) {
        values[c] = velocity.x;
      } else {
        values[c] = velocity.copy().dot(new Vector2(voxel.getAngle()));
      }
      c = c + 1;
    }
    if (axes.contains(Axis.Y)) {
      if (!rotated) {
        values[c] = velocity.y;
      } else {
        values[c] = velocity.copy().dot(new Vector2(voxel.getAngle() + Math.PI / 2d));
      }
      c = c + 1;
    }
    return values;
  }
}
