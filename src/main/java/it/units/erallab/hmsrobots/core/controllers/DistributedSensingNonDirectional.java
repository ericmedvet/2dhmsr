/*
 * Copyright (C) 2022 Giorgia Nadizar <giorgia.nadizar@gmail.com>
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.util.Grid;

/**
 * @author giorgia
 */
public class DistributedSensingNonDirectional extends DistributedSensing {

  @JsonCreator
  public DistributedSensingNonDirectional(
      @JsonProperty("signals") int signals,
      @JsonProperty("nOfInputGrid") Grid<Integer> nOfInputGrid,
      @JsonProperty("nOfOutputGrid") Grid<Integer> nOfOutputGrid,
      @JsonProperty("functions") Grid<TimedRealFunction> functions
  ) {
    super(signals, nOfInputGrid, nOfOutputGrid, functions);
  }

  public DistributedSensingNonDirectional(Grid<Voxel> voxels, int stateSize) {
    this(
        stateSize,
        Grid.create(voxels, v -> (v == null) ? 0 : nOfInputs(v, stateSize)),
        Grid.create(voxels, v -> (v == null) ? 0 : nOfOutputs(v, stateSize)),
        Grid.create(
            voxels.getW(),
            voxels.getH(),
            (x, y) -> voxels.get(x, y) == null ? null : new FunctionWrapper(RealFunction.build(
                (double[] in) -> new double[1 + stateSize],
                nOfInputs(voxels.get(x, y), stateSize),
                nOfOutputs(voxels.get(x, y), stateSize)
            )
            )
        )
    );
  }

  public static int nOfOutputs(Voxel voxel, int signals) {
    return 1 + signals;
  }

  @Override
  protected double[] getLastSignals(int x, int y) {
    double[] values = new double[signals * Dir.values().length];
    if (signals <= 0) {
      return values;
    }
    int c = 0;
    for (Dir dir : Dir.values()) {
      int adjacentX = x + dir.dx;
      int adjacentY = y + dir.dy;
      double[] lastSignals = lastSignalsGrid.get(adjacentX, adjacentY);
      if (lastSignals != null) {
        System.arraycopy(lastSignals, 0, values, c, signals);
      }
      c = c + signals;
    }
    return values;
  }

  @Override
  public String toString() {
    return super.toString().replace("DistributedSensing", "DistributedSensingNonDirectional");
  }
}