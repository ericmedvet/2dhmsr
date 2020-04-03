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

import com.google.common.collect.Range;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.util.Grid;
import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public abstract class ClosedLoopController implements Controller {

  public static enum Aggregate {
    MEAN, DIFF;
  }

  public static class TimedSensor implements Serializable {

    private final Voxel.Sensor sensor;
    private final Range<Integer> range;
    private final Aggregate aggregate;

    public TimedSensor(Voxel.Sensor sensor, int from, int to, Aggregate aggregate) {
      this.sensor = sensor;
      range = Range.closed(from, to);
      this.aggregate = aggregate;
    }

    public TimedSensor(Voxel.Sensor sensor, int when) {
      this.sensor = sensor;
      range = Range.closed(when, when + 1);
      aggregate = Aggregate.MEAN;
    }

    public Voxel.Sensor getSensor() {
      return sensor;
    }

    public Range<Integer> getRange() {
      return range;
    }

    public Aggregate getAggregate() {
      return aggregate;
    }

    @Override
    public int hashCode() {
      int hash = 7;
      hash = 11 * hash + Objects.hashCode(this.sensor);
      hash = 11 * hash + Objects.hashCode(this.range);
      hash = 11 * hash + Objects.hashCode(this.aggregate);
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
      final TimedSensor other = (TimedSensor) obj;
      if (this.sensor != other.sensor) {
        return false;
      }
      if (!Objects.equals(this.range, other.range)) {
        return false;
      }
      if (this.aggregate != other.aggregate) {
        return false;
      }
      return true;
    }

  }

  private final Grid<List<TimedSensor>> sensorsGrid;
  private final Grid<Map<Voxel.Sensor, double[]>> readingsGrid;

  public ClosedLoopController(Grid<List<TimedSensor>> sensorsGrid) {
    this.sensorsGrid = sensorsGrid;
    readingsGrid = Grid.create(sensorsGrid, (List<TimedSensor> timedSensors) -> (timedSensors == null) ? null : (new EnumMap<>(timedSensors.stream().collect(Collectors.toMap(
            TimedSensor::getSensor,
            (TimedSensor t) -> emptyArray(t.getRange().upperEndpoint()),
            (double[] t, double[] u) -> (u.length > t.length) ? u : t)
    ))));
  }

  protected void readSensors(Grid<Voxel> voxelGrid) {
    for (Grid.Entry<Map<Voxel.Sensor, double[]>> gridEntry : readingsGrid) {
      if (gridEntry.getValue() == null) {
        continue;
      }
      int x = gridEntry.getX();
      int y = gridEntry.getY();
      if (voxelGrid.get(x, y) == null) {
        throw new RuntimeException(String.format("Cannot read sensors at (%d, %d) because there is no voxel!", x, y));
      }
      //iterate over sensors
      for (Map.Entry<Voxel.Sensor, double[]> mapEntry : gridEntry.getValue().entrySet()) {
        double value = voxelGrid.get(x, y).getSensorReading(mapEntry.getKey());
        double[] values = mapEntry.getValue();
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
    List<TimedSensor> sensors = sensorsGrid.get(x, y);
    double[] values = new double[sensors.size()];
    for (int i = 0; i < sensors.size(); i++) {
      Range<Integer> range = sensors.get(i).getRange();
      double[] sensorValues = readingsGrid.get(x, y).get(sensors.get(i).getSensor());
      if (range.lowerEndpoint().intValue() == (range.upperEndpoint().intValue() - 1)) {
        values[i] = sensorValues[range.lowerEndpoint()];
      } else {
        if (sensors.get(i).getAggregate().equals(Aggregate.MEAN)) {
          double sum = 0d;
          for (int j = range.lowerEndpoint(); j < range.upperEndpoint(); j++) {
            sum = sum + sensorValues[j];
          }
          values[i] = sum / ((double) range.upperEndpoint() - (double) range.lowerEndpoint());
        } else if (sensors.get(i).getAggregate().equals(Aggregate.DIFF)) {
          values[i] = sensorValues[range.lowerEndpoint()] - sensorValues[range.upperEndpoint() - 1];
        }
      }
    }
    return values;
  }

  public Grid<List<TimedSensor>> getSensorsGrid() {
    return sensorsGrid;
  }

  private static double[] emptyArray(int n) {
    double[] values = new double[n];
    Arrays.fill(values, Double.NaN);
    return values;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 97 * hash + Objects.hashCode(this.sensorsGrid);
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
    final ClosedLoopController other = (ClosedLoopController) obj;
    if (!Objects.equals(this.sensorsGrid, other.sensorsGrid)) {
      return false;
    }
    return true;
  }

}
