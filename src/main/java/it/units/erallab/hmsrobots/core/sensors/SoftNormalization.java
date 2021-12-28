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

import java.util.Collections;

public class SoftNormalization extends CompositeSensor {

  @JsonCreator
  public SoftNormalization(
      @JsonProperty("sensor") Sensor sensor
  ) {
    super(
        Collections.nCopies(sensor.getDomains().length, DoubleRange.of(0d, 1d)).toArray(DoubleRange[]::new),
        sensor
    );
  }

  @Override
  public double[] sense(double t) {
    double[] innerValues = sensor.getReadings();
    double[] values = new double[innerValues.length];
    for (int i = 0; i < values.length; i++) {
      double v = sensor.getDomains()[i].normalize(innerValues[i]);
      //tanh(((x*2)-1)*2)/2+1/2
      values[i] = Math.tanh(((v * 2d) - 1d) * 2d) / 2d + 0.5d;
    }
    return values;
  }


}
