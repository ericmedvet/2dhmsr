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

import it.units.erallab.hmsrobots.util.DoubleRange;

import java.util.Arrays;

/**
 * @author "Eric Medvet" on 2021/09/10 for 2dhmsr
 */
public class MLPState {
  private final double[][] activationValues;
  private final double[][][] weights;
  private final DoubleRange activationDomain;

  public MLPState(double[][] activationValues, double[][][] weights, DoubleRange activationDomain) {
    this.activationValues = copyOf(activationValues);
    this.weights = copyOf(weights);
    this.activationDomain = activationDomain;
  }

  private static double[][] copyOf(double[][] o) {
    double[][] c = new double[o.length][];
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

  public DoubleRange getActivationDomain() {
    return activationDomain;
  }

  public double[][] getActivationValues() {
    return activationValues;
  }

  public double[][][] getWeights() {
    return weights;
  }

}
