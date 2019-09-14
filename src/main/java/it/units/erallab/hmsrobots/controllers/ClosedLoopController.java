/*
 * Copyright (C) 2019 Eric Medvet <eric.medvet@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.controllers;

import it.units.erallab.hmsrobots.objects.Voxel;
import java.util.EnumSet;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public abstract class ClosedLoopController implements Controller {

  public enum Input {
    AREA_RATIO, ABS_LINEAR_VELOCITY;
  }

  private final EnumSet<Input> inputs;

  public ClosedLoopController(EnumSet<Input> inputs) {
    this.inputs = inputs;
  }

  protected double[] collectInputs(Voxel voxel) {
    double[] values = new double[inputs.size()];
    collectInputs(voxel, values, 0);
    return values;
  }
  
  protected void collectInputs(Voxel voxel, double[] values, int c) {
    for (Input input : inputs) {
      if (input.equals(Input.AREA_RATIO)) {
        values[c] = voxel.getAreaRatio();
        c = c + 1;
      } else if (input.equals(Input.ABS_LINEAR_VELOCITY)) {
        values[c] = voxel.getLinearVelocity().getMagnitude();
        c = c + 1;
      }
    }
  }

  public EnumSet<Input> getInputs() {
    return inputs;
  }

}
