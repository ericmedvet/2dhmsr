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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.util.DoubleRange;

import java.util.Arrays;
import java.util.Collections;

public class DynamicNormalization extends AggregatorSensor {

  @JsonCreator
  public DynamicNormalization(
      @JsonProperty("sensor") Sensor sensor,
      @JsonProperty("interval") double interval
  ) {
    super(
        Collections.nCopies(sensor.getDomains().length, DoubleRange.of(0d, 1d)).toArray(DoubleRange[]::new),
        sensor,
        interval
    );
    reset();
  }

  @Override
  protected double[] aggregate(double t) {
    double[] currentReadings = sensor.getReadings();
    double[] mins = new double[currentReadings.length];
    double[] maxs = new double[currentReadings.length];
    Arrays.fill(mins, Double.POSITIVE_INFINITY);
    Arrays.fill(maxs, Double.NEGATIVE_INFINITY);
    for (double[] pastReadings : readings.values()) {
      for (int i = 0; i < currentReadings.length; i++) {
        mins[i] = Math.min(mins[i], pastReadings[i]);
        maxs[i] = Math.max(maxs[i], pastReadings[i]);
      }
    }
    double[] values = new double[currentReadings.length];
    for (int i = 0; i < values.length; i++) {
      values[i] = Math.min(Math.max((currentReadings[i] - mins[i]) / (maxs[i] - mins[i]), 0d), 1d);
    }
    return values;
  }

}
