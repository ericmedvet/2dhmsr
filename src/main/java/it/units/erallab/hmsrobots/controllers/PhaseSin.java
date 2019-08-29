/*
 * Copyright (C) 2019 eric
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

import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.objects.Voxel;

/**
 *
 * @author eric
 */
public class PhaseSin implements Controller {
  
  private final double frequency;
  private final double amplitude;
  private final Grid<Double> phases;

  public PhaseSin(double frequency, double amplitude, Grid<Double> phases) {
    this.frequency = frequency;
    this.amplitude = amplitude;
    this.phases = phases;
  }

  @Override
  public Grid<Double> control(double t, double dt, Grid<Voxel> voxelGrid) {
    Grid<Double> forces = Grid.create(voxelGrid);
    for (int x = 0; x<voxelGrid.getW(); x++) {
      for (int y = 0; y<voxelGrid.getH(); y++) {
        forces.set(x, y, Math.sin(2d*Math.PI*frequency*t+phases.get(x, y))*amplitude);
      }
    }
    return forces;
  }  
  
}
