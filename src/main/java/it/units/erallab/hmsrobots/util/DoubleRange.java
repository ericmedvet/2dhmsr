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

package it.units.erallab.hmsrobots.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.io.Serializable;
import java.util.Arrays;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record DoubleRange(double min, double max) implements Serializable {

  public DoubleRange {
    if (max < min) {
      throw new IllegalArgumentException(String.format(
          "Max has to be lower or equal than min; %f is not than %f.",
          max,
          min
      ));
    }
  }

  public static DoubleRange of(double min, double max) {
    return new DoubleRange(min, max);
  }

  public static DoubleRange[] of(double min, double max, int n) {
    DoubleRange[] domains = new DoubleRange[n];
    Arrays.fill(domains, DoubleRange.of(min, max));
    return domains;
  }

  public double clip(double value) {
    return Math.min(Math.max(value, min), max);
  }

  public double extent() {
    return max - min;
  }

  public double normalize(double value) {
    return (clip(value) - min) / (max - min);
  }

}
