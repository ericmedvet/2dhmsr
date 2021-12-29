/*
 * Copyright (c) "Eric Medvet" 2021.
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

package it.units.erallab.hmsrobots.behavior;

import java.util.Arrays;

/**
 * @author "Eric Medvet" on 2021/09/16 for 2dhmsr
 */
public class Footprint {
  private final boolean[] mask;

  public Footprint(boolean[] mask) {
    this.mask = mask;
  }

  public boolean[] getMask() {
    return mask;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(mask);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Footprint footprint = (Footprint) o;
    return Arrays.equals(mask, footprint.mask);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (boolean b : mask) {
      sb.append(b ? '_' : '.');
    }
    return sb.toString();
  }

  public int length() {
    return mask.length;
  }
}

