/*
 * Copyright (C) 2021 Giorgia Nadizar <giorgia.nadizar@gmail.com> (as Giorgia Nadizar)
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

package it.units.erallab.hmsrobots.core.controllers.snndiscr.onehot;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Arrays;

public class InputConverter implements Serializable {

  @JsonProperty
  private final int nOfBins;

  @JsonCreator
  public InputConverter(@JsonProperty("nOfBins") int nOfBins) {
    this.nOfBins = nOfBins;
  }

  public int[] convert(double[] values) {
    double threshold = 1d / (nOfBins + 1);
    int[] nOfActiveNeurons = Arrays.stream(values).mapToInt(i -> Math.min(nOfBins, (int) (i / threshold))).toArray();
    int[] inputActivity = new int[values.length * nOfBins];
    for (int i = 0; i < nOfActiveNeurons.length; i++) {
      for (int j = 0; j < nOfActiveNeurons[i]; j++) {
        inputActivity[nOfBins * i + j] = 1;
      }
    }
    return inputActivity;
  }

  public int getNOfBins() {
    return nOfBins;
  }
}
