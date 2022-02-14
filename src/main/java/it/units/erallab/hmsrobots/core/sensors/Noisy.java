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

import java.util.Arrays;
import java.util.Random;
import java.util.random.RandomGenerator;

/**
 * @author eric on 2020/12/18 for 2dhmsr
 */
public class Noisy extends CompositeSensor {

  @JsonProperty
  private final double sigma;
  @JsonProperty
  private final long seed;

  private final double[] sigmas;
  private final RandomGenerator random;

  @JsonCreator
  public Noisy(
      @JsonProperty("sensor") Sensor sensor,
      @JsonProperty("sigma") double sigma,
      @JsonProperty("seed") long seed
  ) {
    super(sensor.getDomains(), sensor);
    this.sigma = sigma;
    this.seed = seed;
    random = new Random(seed);
    sigmas = Arrays.stream(sensor.getDomains())
        .mapToDouble(d -> d.extent() * sigma)
        .toArray();
    reset();
  }

  @Override
  public double[] sense(double t) {
    double[] values = sensor.getReadings();
    for (int i = 0; i < values.length; i++) {
      values[i] = values[i] + random.nextGaussian() * sigmas[i];
    }
    return values;
  }

  @Override
  public String toString() {
    return "Noisy{" +
        "sensor=" + sensor +
        ", sigma=" + sigma +
        '}';
  }

}
