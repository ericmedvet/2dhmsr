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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.util.DoubleRange;
import org.dyn4j.geometry.Vector2;

import java.util.EnumSet;

public class Velocity extends AbstractSensor {
  @JsonProperty
  private final boolean rotated;
  @JsonProperty
  private final EnumSet<Axis> axes;
  @JsonProperty
  private final double maxVelocityNorm;

  public Velocity(boolean rotated, double maxVelocityNorm, Axis... axes) {
    this(
        rotated,
        maxVelocityNorm,
        axes.length > 0 ? EnumSet.of(axes[0], axes) : EnumSet.noneOf(Axis.class)
    );
  }

  @JsonCreator
  public Velocity(
      @JsonProperty("rotated") boolean rotated,
      @JsonProperty("maxVelocityNorm") double maxVelocityNorm,
      @JsonProperty("axes") EnumSet<Axis> axes
  ) {
    super(axes.stream().map(a -> DoubleRange.of(-maxVelocityNorm, maxVelocityNorm)).toArray(DoubleRange[]::new));
    this.rotated = rotated;
    this.maxVelocityNorm = maxVelocityNorm;
    this.axes = axes;
  }

  public enum Axis {X, Y}

  @Override
  public double[] sense(double t) {
    double[] values = new double[domains.length];
    int c = 0;
    Point2 linearVelocity = voxel.getLinearVelocity();
    Vector2 velocity = new Vector2(linearVelocity.x(), linearVelocity.y());
    double angle = voxel.getAngle();
    if (axes.contains(Axis.X)) {
      if (!rotated) {
        values[c] = velocity.x;
      } else {
        values[c] = velocity.copy().dot(new Vector2(angle));
      }
      c = c + 1;
    }
    if (axes.contains(Axis.Y)) {
      if (!rotated) {
        values[c] = velocity.y;
      } else {
        values[c] = velocity.copy().dot(new Vector2(angle + Math.PI / 2d));
      }
    }
    return values;
  }

  @Override
  public String toString() {
    return "Velocity{" +
        "rotated=" + rotated +
        ", axes=" + axes +
        ", maxVelocityNorm=" + maxVelocityNorm +
        '}';
  }
}
