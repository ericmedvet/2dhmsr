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
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.util.SerializableFunction;

public class TimeFunction implements Sensor {

  @JsonProperty
  private final SerializableFunction<Double, Double> function;
  @JsonProperty
  private final double min;
  @JsonProperty
  private final double max;
  private final Domain[] domains;

  @JsonCreator
  public TimeFunction(
      @JsonProperty("function") SerializableFunction<Double, Double> function,
      @JsonProperty("min") double min,
      @JsonProperty("max") double max
  ) {
    this.min = min;
    this.max = max;
    this.function = function;
    domains = new Domain[]{Domain.of(min, max)};
  }

  @Override
  public Domain[] getDomains() {
    return domains;
  }

  @Override
  public double[] sense(Voxel voxel, double t) {
    return new double[]{function.apply(t)};
  }

  @Override
  public String toString() {
    return "TimeFunction{" +
        "function=" + function +
        ", min=" + min +
        ", max=" + max +
        '}';
  }
}
