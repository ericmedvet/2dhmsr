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
package it.units.erallab.hmsrobots.core.sensors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Average extends AggregatorSensor {

  @JsonCreator
  public Average(
      @JsonProperty("sensor") Sensor sensor,
      @JsonProperty("interval") double interval
  ) {
    super(sensor.getDomains(), sensor, interval);
    reset();
  }

  @Override
  protected double[] aggregate(double t) {
    double[] sums = new double[readings.firstEntry().getValue().length];
    for (double[] pastReadings : readings.values()) {
      for (int i = 0; i < sums.length; i++) {
        sums[i] = sums[i] + pastReadings[i];
      }
    }
    for (int i = 0; i < sums.length; i++) {
      sums[i] = sums[i] / (double) readings.size();
    }
    return sums;
  }

}
