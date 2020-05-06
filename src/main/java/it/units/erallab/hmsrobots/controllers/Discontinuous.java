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
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class Discontinuous implements Controller {

  public enum Type {IMPULSE, STEP}

  private final Controller controller;
  private final double interval;
  private final Type type;

  private double lastT;
  private Grid<Double> lastControlValues;

  public Discontinuous(Controller controller, double interval, Type type) {
    this.controller = controller;
    this.interval = interval;
    this.type = type;
  }

  @Override
  public Grid<Double> control(double t, Grid<List<Pair<Sensor, double[]>>> sensorsValues) {
    if ((lastControlValues == null) || (t - lastT >= interval)) {
      lastControlValues = controller.control(t, sensorsValues);
      lastT = t;
      return lastControlValues;
    }
    if (type.equals(Type.IMPULSE)) {
      return Grid.create(lastControlValues, d -> (d == null) ? null : 0d);
    }
    return lastControlValues;
  }

  public Controller getController() {
    return controller;
  }

  public double getInterval() {
    return interval;
  }

  public Type getType() {
    return type;
  }
}
