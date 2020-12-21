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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.objects.ControllableVoxel;
import it.units.erallab.hmsrobots.core.objects.Voxel;

public class ControlPower implements Sensor {

  private double lastT;
  @JsonProperty
  private final double controlInterval;
  private final Domain[] domains;

  @JsonCreator
  public ControlPower(
      @JsonProperty("controlInterval") double controlInterval
  ) {
    this.controlInterval = controlInterval;
    domains = new Domain[]{
        Domain.of(0, 1d / controlInterval)
    };
  }

  @Override
  public Domain[] domains() {
    return domains;
  }

  @Override
  public double[] sense(Voxel voxel, double t) {
    if (voxel instanceof ControllableVoxel) {
      return new double[]{((ControllableVoxel) voxel).getControlEnergy() / (t - lastT)};
    }
    lastT = t;
    return new double[]{0d};
  }

  public double getControlInterval() {
    return controlInterval;
  }

  @Override
  public String toString() {
    return "ControlPower{" +
        "controlInterval=" + controlInterval +
        '}';
  }
}
