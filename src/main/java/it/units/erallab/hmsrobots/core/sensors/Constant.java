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

/**
 * @author eric on 2020/12/11 for 2dhmsr
 */
public class Constant extends AbstractSensor {

  @JsonProperty
  private final double[] values;

  @JsonCreator
  public Constant(@JsonProperty("values") double... values) {
    super(computeDomains(values));
    this.values = values;
  }

  private static DoubleRange[] computeDomains(double... values) {
    double max = Arrays.stream(values).max().orElse(1d);
    double min = Arrays.stream(values).min().orElse(0d);
    max = Math.max(1d, max);
    min = Math.min(0d, min);
    DoubleRange[] domains = new DoubleRange[values.length];
    for (int i = 0; i < values.length; i++) {
      domains[i] = DoubleRange.of(min, max);
    }
    return domains;
  }

  @Override
  public double[] sense(double t) {
    return values;
  }

  @Override
  public String toString() {
    return "Constant{" +
        "values=" + Arrays.toString(values) +
        '}';
  }
}
