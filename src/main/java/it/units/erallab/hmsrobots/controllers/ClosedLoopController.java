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

import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.util.Grid;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public abstract class ClosedLoopController implements Controller {

  private final Grid<List<Pair<Voxel.Sensor, Integer>>> sensorsGrid;
  private final Grid<Map<Voxel.Sensor, double[]>> readingsGrid;

  public ClosedLoopController(Grid<List<Pair<Voxel.Sensor, Integer>>> sensorsGrid) {
    this.sensorsGrid = sensorsGrid;
    readingsGrid = Grid.create(sensorsGrid);
  }

  protected void readSensors(Grid<Voxel> voxelGrid) {
    for (Grid.Entry<List<Pair<Voxel.Sensor, Integer>>> entry : sensorsGrid) {
      if (entry.getValue() == null) {
        continue;
      }
      int x = entry.getX();
      int y = entry.getY();
      if (voxelGrid.get(x, y) == null) {
        throw new RuntimeException(String.format("Cannot read sensors at (%d, %d) because there is no voxel!", x, y));
      }
      //check if everything is defined
      if (readingsGrid.get(x, y) == null) {
        readingsGrid.set(x, y, new EnumMap<Voxel.Sensor, double[]>(Voxel.Sensor.class));
        for (Pair<Voxel.Sensor, Integer> sensorPair : entry.getValue()) {
          double[] values = readingsGrid.get(x, y).get(sensorPair.getLeft());
          if ((values == null) || (values.length < (sensorPair.getRight() + 1))) {
            values = new double[sensorPair.getRight() + 1];
            Arrays.fill(values, Double.NaN);
            readingsGrid.get(x, y).put(sensorPair.getLeft(), values);
          }
        }
      }
      //iterate over sensor
      for (Pair<Voxel.Sensor, Integer> sensorPair : entry.getValue()) {
        double value = voxelGrid.get(x, y).getSensorReading(sensorPair.getLeft());
        double[] values = readingsGrid.get(x, y).get(sensorPair.getLeft());
        if (values.length > 1) {
          double[] shifted = new double[values.length - 1];
          System.arraycopy(values, 0, shifted, 0, shifted.length);
          System.arraycopy(shifted, 0, values, 1, shifted.length);
          //check if empty and fill with current value
          if (Double.isNaN(values[1])) {
            for (int i = 1; i < values.length; i++) {
              values[i] = value;
            }
          }
        }
        values[0] = value;
      }
    }
  }

  protected double[] getReadings(int x, int y) {
    List<Pair<Voxel.Sensor, Integer>> sensors = sensorsGrid.get(x, y);
    double[] values = new double[sensors.size()];
    for (int i = 0; i < sensors.size(); i++) {
      double[] sensorValues = readingsGrid.get(x, y).get(sensors.get(i).getLeft());
      values[i] = sensorValues[sensors.get(i).getRight()];
    }
    return values;
  }

  public Grid<List<Pair<Voxel.Sensor, Integer>>> getSensorsGrid() {
    return sensorsGrid;
  }

}
