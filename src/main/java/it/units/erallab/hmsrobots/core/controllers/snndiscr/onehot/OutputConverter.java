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
import java.util.stream.IntStream;

public class OutputConverter implements Serializable {

  @JsonProperty
  private final int nOfBins;

  @JsonCreator
  public OutputConverter(@JsonProperty("nOfBins") int nOfBins) {
    this.nOfBins = nOfBins;
  }

  public double[] convert(int[] outputActivity) {
    if (outputActivity.length % nOfBins != 0) {
      throw new IllegalArgumentException(String.format("Output size %d cannot be divided by the number of bins %d", outputActivity.length, nOfBins));
    }
    return IntStream.range(0, outputActivity.length / nOfBins).mapToDouble(i -> {
      int nOfActiveNeurons = IntStream.range(0, nOfBins).map(j -> outputActivity[nOfBins * i + j]).sum();
      return (double) nOfActiveNeurons / nOfBins * 2 - 1;
    }).toArray();
  }

  public int getNOfBins() {
    return nOfBins;
  }

}
