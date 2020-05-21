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
package it.units.erallab.hmsrobots.core.controllers;

import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.core.sensors.Sensor;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public abstract class SensingController<C, V extends SensingVoxel> implements Controller<V> {

  @Override
  public void control(double t, Grid<V> voxels) {
    Grid<List<Pair<Sensor, double[]>>> sensorsValues = Grid.create(voxels, v -> v == null ? null : v.sense(t));
    Grid<C> controls = computeControlValues(t, sensorsValues);
    for (Grid.Entry<V> entry : voxels) {
      if (entry.getValue() != null) {
        control(controls.get(entry.getX(), entry.getY()), entry.getValue());
      }
    }
  }

  protected abstract Grid<C> computeControlValues(double t, Grid<List<Pair<Sensor, double[]>>> sensorsValues);

  protected abstract void control(C c, V voxel);

}
