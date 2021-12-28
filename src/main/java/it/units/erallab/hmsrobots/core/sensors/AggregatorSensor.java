/*
 * Copyright (C) 2021 Eric Medvet <eric.medvet@gmail.com> (as Eric Medvet <eric.medvet@gmail.com>)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.units.erallab.hmsrobots.core.sensors;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.util.DoubleRange;

import java.util.TreeMap;

/**
 * @author "Eric Medvet" on 2021/08/13 for 2dhmsr
 */
public abstract class AggregatorSensor extends CompositeSensor {
  @JsonProperty
  protected final double interval;
  protected final TreeMap<Double, double[]> readings;

  public AggregatorSensor(DoubleRange[] domains, Sensor sensor, double interval) {
    super(domains, sensor);
    this.interval = interval;
    readings = new TreeMap<>();
    reset();
  }

  protected abstract double[] aggregate(double t);

  @Override
  public void reset() {
    super.reset();
    readings.clear();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "sensor=" + sensor +
        ", interval=" + interval +
        '}';
  }

  @Override
  protected double[] sense(double t) {
    double[] currentReadings = sensor.getReadings();
    readings.put(t, currentReadings);
    double t0 = readings.firstKey();
    while (t0 < (t - interval)) {
      readings.remove(t0);
      t0 = readings.firstKey();
    }
    return aggregate(t);
  }

}
