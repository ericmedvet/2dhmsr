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

public class Trend extends AggregatorSensor {

  @JsonCreator
  public Trend(
      @JsonProperty("sensor") Sensor sensor, @JsonProperty("interval") double interval
  ) {
    super(Arrays.stream(sensor.getDomains())
        .map(d -> DoubleRange.of(-d.extent() / interval, d.extent() / interval))
        .toArray(DoubleRange[]::new), sensor, interval);
    reset();
  }

  @Override
  protected double[] aggregate(double t) {
    double localInterval = readings.lastKey() - readings.firstKey();
    if (localInterval == 0) {
      return new double[domains.length];
    }
    double[] firsts = readings.firstEntry().getValue();
    double[] lasts = readings.lastEntry().getValue();
    double[] changes = new double[firsts.length];
    for (int i = 0; i < changes.length; i++) {
      changes[i] = (lasts[i] - firsts[i]) / (localInterval);
    }
    return changes;
  }

}
