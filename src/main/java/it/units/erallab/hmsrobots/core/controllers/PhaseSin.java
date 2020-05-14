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

import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.SerializableFunction;

import java.util.Objects;

/**
 * @author eric
 */
public class PhaseSin extends TimeFunctions {

  private final Grid<Double> phases;

  public PhaseSin(double frequency, double amplitude, Grid<Double> phases) {
    super(getFunctions(frequency, amplitude, phases));
    this.phases = phases;
  }

  private static Grid<SerializableFunction<Double, Double>> getFunctions(final double frequency, final double amplitude, final Grid<Double> phases) {
    Grid<SerializableFunction<Double, Double>> functions = Grid.create(phases);
    for (Grid.Entry<Double> entry : phases) {
      if (entry.getValue()!=null) {
        functions.set(entry.getX(), entry.getY(), t -> Math.sin(2d*Math.PI*frequency*t+entry.getValue())*amplitude);
      }
    }
    return functions;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 83 * hash + Objects.hashCode(this.phases);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final PhaseSin other = (PhaseSin) obj;
    return Objects.equals(this.phases, other.phases);
  }
  
  public Grid<Double> getPhases() {
    return phases;
  }
  
}
