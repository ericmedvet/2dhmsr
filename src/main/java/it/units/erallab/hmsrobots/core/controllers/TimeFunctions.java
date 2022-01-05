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
package it.units.erallab.hmsrobots.core.controllers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.core.snapshots.ScopedReadings;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import it.units.erallab.hmsrobots.core.snapshots.StackedScopedReadings;
import it.units.erallab.hmsrobots.util.DoubleRange;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.SerializableFunction;

import java.util.Objects;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class TimeFunctions extends AbstractController implements Snapshottable {

  @JsonProperty
  private final Grid<SerializableFunction<Double, Double>> functions;

  private double[] outputs;

  @JsonCreator
  public TimeFunctions(
      @JsonProperty("functions") Grid<SerializableFunction<Double, Double>> functions
  ) {
    this.functions = functions;
  }

  @Override
  public Grid<Double> computeControlSignals(double t, Grid<Voxel> voxels) {
    outputs = new double[(int) voxels.values().stream().filter(Objects::nonNull).count()];
    Grid<Double> controlSignals = Grid.create(voxels.getW(), voxels.getH());
    int c = 0;
    for (Grid.Entry<Voxel> entry : voxels) {
      SerializableFunction<Double, Double> function = functions.get(entry.key().x(), entry.key().y());
      if ((entry.value() != null) && (function != null)) {
        double v = function.apply(t);
        controlSignals.set(entry.key().x(), entry.key().y(), v);
        outputs[c] = v;
        c = c + 1;
      }
    }
    return controlSignals;
  }

  public Grid<SerializableFunction<Double, Double>> getFunctions() {
    return functions;
  }

  @Override
  public Snapshot getSnapshot() {
    return new Snapshot(
        new StackedScopedReadings(new ScopedReadings(outputs, DoubleRange.of(-1d, 1d, outputs.length))),
        getClass()
    );
  }

  @Override
  public void reset() {
  }

  @Override
  public String toString() {
    return "TimeFunctions{" +
        "functions=" + functions +
        '}';
  }
}
