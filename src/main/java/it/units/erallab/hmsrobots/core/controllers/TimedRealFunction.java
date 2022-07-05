/*
 * Copyright (C) 2022 Eric Medvet <eric.medvet@gmail.com> (as Eric Medvet <eric.medvet@gmail.com>)
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

package it.units.erallab.hmsrobots.core.controllers;

import java.util.function.BiFunction;

/**
 * @author eric on 2021/03/09 for 2dhmsr
 */
public interface TimedRealFunction {
  double[] apply(double t, double[] input);

  int getInputDimension();

  int getOutputDimension();

  static TimedRealFunction from(BiFunction<Double, double[], double[]> f, int nOfInputs, int nOfOutputs) {
    return new TimedRealFunction() {
      @Override
      public double[] apply(double t, double[] input) {
        if (input.length != nOfInputs) {
          throw new IllegalArgumentException(String.format("Unsupported input size: %d instead of %d", input.length, nOfInputs));
        }
        double[] output = f.apply(t, input);
        if (output.length != nOfOutputs) {
          throw new IllegalArgumentException(String.format("Unsupported output size: %d instead of %d", output.length, nOfOutputs));
        }
        return output;
      }

      @Override
      public int getInputDimension() {
        return nOfInputs;
      }

      @Override
      public int getOutputDimension() {
        return nOfOutputs;
      }
    };
  }
}
