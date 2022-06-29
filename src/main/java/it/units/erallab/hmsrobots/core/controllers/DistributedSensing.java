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
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import it.units.erallab.hmsrobots.util.DoubleRange;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author eric
 */
public class DistributedSensing extends AbstractController implements Snapshottable {

  @JsonProperty
  protected final int signals;
  protected final Grid<double[]> lastSignalsGrid;
  @JsonProperty
  private final Grid<Integer> nOfInputGrid;
  @JsonProperty
  private final Grid<Integer> nOfOutputGrid;
  @JsonProperty
  private final Grid<TimedRealFunction> functions;
  private final Grid<double[]> currentSignalsGrid;
  private final Grid<Double> controlSignalsGrid;

  @JsonCreator
  public DistributedSensing(
      @JsonProperty("signals") int signals,
      @JsonProperty("nOfInputGrid") Grid<Integer> nOfInputGrid,
      @JsonProperty("nOfOutputGrid") Grid<Integer> nOfOutputGrid,
      @JsonProperty("functions") Grid<TimedRealFunction> functions
  ) {
    this.signals = signals;
    this.nOfInputGrid = nOfInputGrid;
    this.nOfOutputGrid = nOfOutputGrid;
    this.functions = functions;
    lastSignalsGrid = Grid.create(functions, f -> new double[signals * Dir.values().length]);
    currentSignalsGrid = Grid.create(functions, f -> new double[signals * Dir.values().length]);
    controlSignalsGrid = Grid.create(functions, f -> 0d);
    reset();
  }

  public DistributedSensing(Grid<Voxel> voxels, int signals) {
    this(
        signals,
        Grid.create(voxels, v -> (v == null) ? 0 : nOfInputs(v, signals)),
        Grid.create(voxels, v -> (v == null) ? 0 : nOfOutputs(v, signals)),
        Grid.create(
            voxels.getW(),
            voxels.getH(),
            (x, y) -> voxels.get(x, y) == null ? null : new FunctionWrapper(RealFunction.build(
                (double[] in) -> new double[1 + signals * Dir.values().length],
                nOfInputs(voxels.get(x, y), signals),
                nOfOutputs(voxels.get(x, y), signals)
            )
            )
        )
    );
  }

  protected enum Dir {

    N(0, -1, 0),
    E(1, 0, 1),
    S(0, 1, 2),
    W(-1, 0, 3);

    final int dx;
    final int dy;
    private final int index;

    Dir(int dx, int dy, int index) {
      this.dx = dx;
      this.dy = dy;
      this.index = index;
    }

    private static Dir adjacent(Dir dir) {
      return switch (dir) {
        case N -> Dir.S;
        case E -> Dir.W;
        case S -> Dir.N;
        case W -> Dir.E;
      };
    }
  }

  protected static class FunctionWrapper implements TimedRealFunction {
    @JsonProperty
    private final TimedRealFunction inner;

    @JsonCreator
    public FunctionWrapper(@JsonProperty("inner") TimedRealFunction inner) {
      this.inner = inner;
    }

    @Override
    public double[] apply(double t, double[] in) {
      return inner.apply(t, in);
    }

    @Override
    public int getInputDimension() {
      return inner.getInputDimension();
    }

    @Override
    public int getOutputDimension() {
      return inner.getOutputDimension();
    }
  }

  public record DistributedSensingState(
      Grid<Boolean> body,
      Grid<Double> controlSignalsGrid,
      Grid<double[]> lastSignalsGrid,
      DoubleRange signalsDomain) {
  }

  public static int nOfInputs(Voxel voxel, int signals) {
    return signals * Dir.values().length + voxel.getSensors().stream().mapToInt(s -> s.getDomains().length).sum();
  }

  public static int nOfOutputs(Voxel voxel, int signals) {
    return 1 + signals * Dir.values().length;
  }

  @Override
  public Grid<Double> computeControlSignals(double t, Grid<Voxel> voxels) {
    for (Grid.Entry<Voxel> entry : voxels) {
      if (entry.value() == null) {
        continue;
      }
      //get inputs
      double[] signals = getLastSignals(entry.key().x(), entry.key().y());
      double[] inputs = ArrayUtils.addAll(entry.value().getSensorReadings(), signals);
      //compute outputs
      TimedRealFunction function = functions.get(entry.key().x(), entry.key().y());
      double[] outputs = function != null ? function.apply(t, inputs) : new double[nOfOutputs(
          entry.key().x(),
          entry.key().y()
      )];
      //save outputs
      controlSignalsGrid.set(entry.key().x(), entry.key().y(), outputs[0]);
      System.arraycopy(outputs, 1, currentSignalsGrid.get(entry.key().x(), entry.key().y()), 0, outputs.length - 1);
    }
    for (Grid.Entry<Voxel> entry : voxels) {
      if (entry.value() == null) {
        continue;
      }
      int x = entry.key().x();
      int y = entry.key().y();
      System.arraycopy(
          currentSignalsGrid.get(x, y),
          0,
          lastSignalsGrid.get(x, y),
          0,
          currentSignalsGrid.get(x, y).length
      );
    }
    return Grid.copy(controlSignalsGrid);
  }

  public Grid<TimedRealFunction> getFunctions() {
    return functions;
  }

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
        int index = Dir.adjacent(dir).index;
        System.arraycopy(lastSignals, index * signals, values, c, signals);
      }
      c = c + signals;
    }
    return values;
  }

  public int nOfInputs(int x, int y) {
    return nOfInputGrid.get(x, y);
  }

  public int nOfOutputs(int x, int y) {
    return nOfOutputGrid.get(x, y);
  }

  @Override
  public Snapshot getSnapshot() {
    return new Snapshot(
        new DistributedSensingState(
            Grid.create(nOfInputGrid, i -> i > 0),
            Grid.copy(controlSignalsGrid),
            Grid.create(lastSignalsGrid, a -> Arrays.copyOf(a, a.length)),
            DoubleRange.of(-1d, 1d)
        ),
        getClass()
    );
  }

  @Override
  public void reset() {
    for (int x = 0; x < lastSignalsGrid.getW(); x++) {
      for (int y = 0; y < lastSignalsGrid.getH(); y++) {
        lastSignalsGrid.set(x, y, new double[signals * Dir.values().length]);
      }
    }
    for (int x = 0; x < currentSignalsGrid.getW(); x++) {
      for (int y = 0; y < currentSignalsGrid.getH(); y++) {
        currentSignalsGrid.set(x, y, new double[signals * Dir.values().length]);
      }
    }
    functions.values().stream().filter(Objects::nonNull).forEach(f -> {
      if (f instanceof Resettable) {
        ((Resettable) f).reset();
      }
    });
  }

  @Override
  public String toString() {
    return "DistributedSensing{" +
        "signals=" + signals +
        ", functions=" + functions +
        '}';
  }
}