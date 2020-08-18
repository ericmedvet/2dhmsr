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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author eric
 * @created 2020/08/18
 * @project TwoDimHighlyModularSoftRobots
 */
public class CentralizedSensing<V extends SensingVoxel> implements Controller<V> {

  private final int nOfInputs;
  private final int nOfOutputs;

  private Function<double[], double[]> function;

  public CentralizedSensing(Grid<SensingVoxel> voxels) {
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

  public int nOfInputs() {
    return nOfInputs;
  }

  public int nOfOutputs() {
    return nOfOutputs;
  }

  public Function<double[], double[]> getFunction() {
    return function;
  }

  public void setFunction(Function<double[], double[]> function) {
    this.function = function;
  }

  @Override
  public void control(double t, Grid<V> voxels) {
    //collect inputs
    double[] inputs = new double[nOfInputs];
    int c = 0;
    List<List<Pair<Sensor, double[]>>> allReadings = voxels.values().stream()
        .filter(Objects::nonNull)
        .map(v -> v.sense(t))
        .collect(Collectors.toList());
    for (List<Pair<Sensor, double[]>> readings : allReadings) {
      for (Pair<Sensor, double[]> sensorPair : readings) {
        double[] sensorReadings = sensorPair.getValue();
        System.arraycopy(sensorReadings, 0, inputs, c, sensorReadings.length);
        c = c + sensorReadings.length;
      }
    }
    //compute outputs
    double[] outputs = function != null ? function.apply(inputs) : new double[nOfOutputs];
    //apply inputs
    c = 0;
    for (V voxel : voxels.values()) {
      if (voxel != null) {
        if (c < outputs.length) {
          voxel.applyForce(outputs[c]);
          c = c + 1;
        }
      }
    }
  }

}
