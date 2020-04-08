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
package it.units.erallab.hmsrobots.controllers;

import it.units.erallab.hmsrobots.sensors.Sensor;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.SerializableFunction;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class TimeFunction implements Controller {

  private final Grid<SerializableFunction<Double, Double>> functions;

  public TimeFunction(Grid<SerializableFunction<Double, Double>> functions) {
    this.functions = functions;
  }

  @Override
  public Grid<Double> control(double t, Grid<List<Pair<Sensor, double[]>>> sensorsValues) {
    Grid<Double> forces = Grid.create(sensorsValues);
    for (Grid.Entry<List<Pair<Sensor, double[]>>> entry : sensorsValues) {
      SerializableFunction<Double, Double> function = functions.get(entry.getX(), entry.getY());
      if ((entry.getValue() != null) && (function != null)) {
        forces.set(entry.getX(), entry.getY(), function.apply(t));
      }
    }
    return forces;
  }

  public Grid<SerializableFunction<Double, Double>> getFunctions() {
    return functions;
  }

}
