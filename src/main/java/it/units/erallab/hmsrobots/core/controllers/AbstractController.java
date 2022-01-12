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

package it.units.erallab.hmsrobots.core.controllers;

import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.util.Grid;

import java.io.Serializable;

public abstract class AbstractController implements Controller, Serializable {

  public abstract Grid<Double> computeControlSignals(double t, Grid<Voxel> voxels);

  @Override
  public void control(double t, Grid<Voxel> voxels) {
    Grid<Double> controlSignals = computeControlSignals(t, voxels);
    voxels.forEach(e -> {
      if (e.value() != null) {
        e.value().applyForce(controlSignals.get(e.key().x(), e.key().y()));
      }
    });
  }

  public AbstractController smoothed(double controlSignalSpeed) {
    return new SmoothedController(this, controlSignalSpeed);
  }

  public AbstractController step(double stepT) {
    return new StepController(this, stepT);
  }

}
