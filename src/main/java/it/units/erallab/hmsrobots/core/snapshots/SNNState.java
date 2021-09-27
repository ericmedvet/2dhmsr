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

package it.units.erallab.hmsrobots.core.snapshots;

import it.units.erallab.hmsrobots.util.Domain;

import java.util.Arrays;

/**
 * @author "Eric Medvet" on 2021/09/10 for 2dhmsr
 */
public class SNNState extends MLPState {

  private final int[][][] spikes;

  public SNNState(int[][][] spikes, double[][][] weights) {
    super(computeFiringRates(spikes), weights, Domain.of(0, Double.POSITIVE_INFINITY));
    this.spikes = copyOf(spikes);
  }

  private static double[][] computeFiringRates(int[][][] spikes) {
    double[][] firingRates = new double[spikes.length][];
    for (int i = 0; i < firingRates.length; i++) {
      firingRates[i] = Arrays.stream(spikes[i]).sequential().mapToDouble(s -> Arrays.stream(s).sum() * 1d / s.length).toArray();
    }
    return firingRates;
  }

  public int[][][] getSpikes() {
    return spikes;
  }

  private static double[][] copyOf(double[][] o) {
    double[][] c = new double[o.length][];
    for (int i = 0; i < o.length; i++) {
      c[i] = Arrays.copyOf(o[i], o[i].length);
    }
    return c;
  }

  private static int[][] copyOf(int[][] o) {
    int[][] c = new int[o.length][];
    for (int i = 0; i < o.length; i++) {
      c[i] = Arrays.copyOf(o[i], o[i].length);
    }
    return c;
  }

  private static double[][][] copyOf(double[][][] o) {
    double[][][] c = new double[o.length][][];
    for (int i = 0; i < o.length; i++) {
      c[i] = copyOf(o[i]);
    }
    return c;
  }

  private static int[][][] copyOf(int[][][] o) {
    int[][][] c = new int[o.length][][];
    for (int i = 0; i < o.length; i++) {
      c[i] = copyOf(o[i]);
    }
    return c;
  }

}
