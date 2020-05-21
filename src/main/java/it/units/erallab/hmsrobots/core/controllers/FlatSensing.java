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
import java.util.Objects;

public abstract class FlatSensing extends SensingController<Double, SensingVoxel> {

  private final Grid<SensingVoxel> voxels;

  private final int nOfInputs;
  private final int nOfOutputs;

  public FlatSensing(Grid<SensingVoxel> voxels) {
    this.voxels = voxels;
    nOfInputs = voxels.values().stream()
        .filter(Objects::nonNull)
        .mapToInt(v -> v.getSensors().stream()
            .mapToInt(s -> s.domains().length)
            .sum())
        .sum();
    nOfOutputs = (int) voxels.values().stream()
        .filter(Objects::nonNull)
        .count();
  }

  protected int nOfInputs() {
    return nOfInputs;
  }

  protected int nOfOutputs() {
    return nOfOutputs;
  }

  private double[] flatten(Grid<List<Pair<Sensor, double[]>>> readingsGrid) {
    double[] values = new double[nOfInputs];
    int c = 0;
    for (List<Pair<Sensor, double[]>> readings : readingsGrid.values()) {
      if (readings != null) {
        for (Pair<Sensor, double[]> sensorPair : readings) {
          double[] sensorReadings = sensorPair.getValue();
          System.arraycopy(sensorReadings, 0, values, c, sensorReadings.length);
          c = c + sensorReadings.length;
        }
      }
    }
    return values;
  }

  @Override
  public void control(double t, Grid<SensingVoxel> voxels) {
    Grid<List<Pair<Sensor, double[]>>> sensorsValues = Grid.create(voxels, v -> v == null ? null : v.sense(t));
    double[] inputs = flatten(sensorsValues);
    double[] outputs = control(t, inputs);
    int c = 0;
    for (Grid.Entry<SensingVoxel> entry : voxels) {
      if (entry.getValue() != null) {
        entry.getValue().applyForce(outputs[c]);
        c = c + 1;
      }
    }
  }

  @Override
  protected Grid<Double> computeControlValues(double t, Grid<List<Pair<Sensor, double[]>>> sensorsValues) {
    double[] inputs = flatten(sensorsValues);
    double[] outputs = control(t, inputs);
    int c = 0;
    Grid<Double> controls = Grid.create(sensorsValues);
    for (Grid.Entry<List<Pair<Sensor, double[]>>> entry : sensorsValues) {
      if (entry.getValue() != null) {
        controls.set(entry.getX(), entry.getY(), outputs[c]);
        c = c + 1;
      }
    }
    return controls;
  }

  @Override
  protected void control(Double value, SensingVoxel voxel) {
    voxel.applyForce(value);
  }

  protected abstract double[] control(double t, double[] inputs);
}
