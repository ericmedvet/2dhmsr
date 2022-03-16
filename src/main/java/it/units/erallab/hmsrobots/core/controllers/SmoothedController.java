/*
 * Copyright (C) 2021 Giorgia Nadizar <giorgia.nadizar@gmail.com> (as Giorgia Nadizar)
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.util.Grid;

public class SmoothedController extends CompositeController {

  @JsonProperty
  private final double controlSignalSpeed;

  double lastT = Double.NEGATIVE_INFINITY;
  Grid<Double> currentControlSignals = null;

  @JsonCreator
  public SmoothedController(
      @JsonProperty("innerController") AbstractController innerController,
      @JsonProperty("controlSignalSpeed") double controlSignalSpeed
  ) {
    super(innerController);
    this.controlSignalSpeed = controlSignalSpeed;
  }

  @Override
  public Grid<Double> computeControlSignals(double t, Grid<Voxel> voxels) {
    Grid<Double> targetControlSignals = innerController.computeControlSignals(t, voxels);
    if (currentControlSignals == null) {
      currentControlSignals = Grid.create(targetControlSignals, v -> 0d);
      lastT = t;
    }
    double dT = t - lastT;
    double dControlSignal = dT * controlSignalSpeed;
    lastT = t;
    return Grid.create(targetControlSignals.getW(), targetControlSignals.getH(), (x, y) -> {
      Double targetControlSignal = targetControlSignals.get(x, y);
      double currentControlSignal = currentControlSignals.get(x, y);
      if (targetControlSignal == null) {
        return 0d;
      }
      if (Math.abs(targetControlSignal - currentControlSignal) <= dControlSignal) {
        currentControlSignals.set(x, y, targetControlSignal);
      } else if (targetControlSignal > currentControlSignal) {
        currentControlSignals.set(x, y, currentControlSignals.get(x, y) + dControlSignal);
      } else {
        currentControlSignals.set(x, y, currentControlSignals.get(x, y) - dControlSignal);
      }
      return currentControlSignals.get(x, y);
    });
  }

  @Override
  public void reset() {
    innerController.reset();
    currentControlSignals = null;
  }

}
