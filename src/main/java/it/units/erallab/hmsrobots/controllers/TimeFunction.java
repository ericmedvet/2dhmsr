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
import java.util.function.Function;

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
    for (int x = 0; x<voxelGrid.getW(); x++) {
      for (int y = 0; y<voxelGrid.getH(); y++) {
        forces.set(x, y, functions.get(x, y).apply(t));
      }
    }
    return forces;
  }

  public Grid<SerializableFunction<Double, Double>> getFunctions() {
    return functions;
  }
  
  
}
