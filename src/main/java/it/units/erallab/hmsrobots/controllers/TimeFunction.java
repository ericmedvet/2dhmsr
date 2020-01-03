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

import it.units.erallab.hmsrobots.util.SerializableFunction;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.util.Grid;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class TimeFunction implements Controller {
  
  private final Grid<SerializableFunction<Double, Double>> functions;

  public TimeFunction(Grid<SerializableFunction<Double, Double>> functions) {
    this.functions = functions;
  }

  @Override
  public Grid<Double> control(double t, double dt, Grid<Voxel> voxelGrid) {
    Grid<Double> forces = Grid.create(voxelGrid);
    for (Grid.Entry<Voxel> entry : voxelGrid) {
      Voxel voxel = entry.getValue();
      SerializableFunction<Double, Double> function = functions.get(entry.getX(), entry.getY());
      if ((voxel!=null)&&(function!=null)) {
        forces.set(entry.getX(), entry.getY(), function.apply(t));
      }
    }
    return forces;
  }

  public Grid<SerializableFunction<Double, Double>> getFunctions() {
    return functions;
  }
  
}
