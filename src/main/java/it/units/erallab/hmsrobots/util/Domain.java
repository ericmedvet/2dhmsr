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

import java.io.Serializable;
import java.util.Arrays;

public class Domain implements Serializable {
  private final double min;
  private final double max;

  private Domain(double min, double max) {
    this.min = min;
    this.max = max;
  }

  public static Domain of(double min, double max) {
    return new Domain(min, max);
  }

  public static Domain[] of(double min, double max, int n) {
    Domain[] domains = new Domain[n];
    Arrays.fill(domains, Domain.of(min, max));
    return domains;
  }

  public double getMin() {
    return min;
  }

  public double getMax() {
    return max;
  }

  @Override
  public String toString() {
    return String.format("[%.1f;%.1f]", min, max);
  }
}
