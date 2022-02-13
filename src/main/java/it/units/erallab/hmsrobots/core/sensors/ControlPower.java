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
import it.units.erallab.hmsrobots.util.DoubleRange;

public class ControlPower extends AbstractSensor {

  @JsonProperty
  private final double controlInterval;
  private double lastT;

  @JsonCreator
  public ControlPower(
      @JsonProperty("controlInterval") double controlInterval
  ) {
    super(new DoubleRange[]{
        DoubleRange.of(0, 1d / controlInterval)
    });
    this.controlInterval = controlInterval;
  }

  public double getControlInterval() {
    return controlInterval;
  }

  @Override
  public double[] sense(double t) {
    double power = voxel.getControlEnergy() / (t - lastT);
    lastT = t;
    return new double[]{power};
  }

  @Override
  public void reset() {
    super.reset();
    lastT = 0;
  }

  @Override
  public String toString() {
    return "ControlPower{" +
        "controlInterval=" + controlInterval +
        '}';
  }
}
