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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.core.sensors.Sensor;
import it.units.erallab.hmsrobots.core.snapshots.ScopedReadings;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import it.units.erallab.hmsrobots.core.snapshots.StackedScopedReadings;
import it.units.erallab.hmsrobots.util.Domain;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Collection;
import java.util.Objects;

/**
 * @author eric
 */
public class CentralizedSensing extends AbstractController<SensingVoxel> implements Snapshottable {

  @JsonProperty
  private final int nOfInputs;
  @JsonProperty
  private final int nOfOutputs;

  @JsonProperty
  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
  private TimedRealFunction function;

  private double[] inputs;
  private double[] outputs;
  private Domain[] inputDomains;
  private final Domain[] outputDomains;

  public CentralizedSensing(
      @JsonProperty("nOfInputs") int nOfInputs,
      @JsonProperty("nOfOutputs") int nOfOutputs,
      @JsonProperty("function") TimedRealFunction function
  ) {
    this.nOfInputs = nOfInputs;
    this.nOfOutputs = nOfOutputs;
    outputDomains = Domain.of(-1d, 1d, nOfOutputs);
    setFunction(function);
  }

  public CentralizedSensing(Grid<? extends SensingVoxel> voxels) {
    this(voxels, RealFunction.build(in -> new double[nOfOutputs(voxels)], nOfInputs(voxels), nOfOutputs(voxels)));
  }

  public CentralizedSensing(Grid<? extends SensingVoxel> voxels, TimedRealFunction function) {
    this(nOfInputs(voxels), nOfOutputs(voxels), function);
  }

  public static int nOfInputs(Grid<? extends SensingVoxel> voxels) {
    return voxels.values().stream()
        .filter(Objects::nonNull)
        .mapToInt(v -> v.getSensors().stream()
            .mapToInt(s -> s.getDomains().length)
            .sum())
        .sum();
  }

  public static int nOfOutputs(Grid<? extends SensingVoxel> voxels) {
    return (int) voxels.values().stream()
        .filter(Objects::nonNull)
        .count();
  }

  public int nOfInputs() {
    return nOfInputs;
  }

  public int nOfOutputs() {
    return nOfOutputs;
  }

  public TimedRealFunction getFunction() {
    return function;
  }

  public void setFunction(TimedRealFunction function) {
    if (function.getInputDimension() != nOfInputs || function.getOutputDimension() != nOfOutputs) {
      throw new IllegalArgumentException(String.format(
          "Wrong dimension of input or output in provided function: R^%d->R^%d expected, R^%d->R^%d found",
          nOfInputs, nOfOutputs,
          function.getInputDimension(), function.getOutputDimension()
      ));
    }
    this.function = function;
  }

  @Override
  public Grid<Double> computeControlSignals(double t, Grid<? extends SensingVoxel> voxels) {
    //collect inputs
    inputs = voxels.values().stream()
        .filter(Objects::nonNull)
        .map(SensingVoxel::getSensorReadings)
        .reduce(ArrayUtils::addAll)
        .orElse(new double[nOfInputs]);
    inputDomains = voxels.values().stream()
        .filter(Objects::nonNull)
        .map(SensingVoxel::getSensors)
        .flatMap(Collection::stream)
        .map(Sensor::getDomains)
        .reduce(ArrayUtils::addAll)
        .orElse(Domain.of(-1d, 1d, nOfInputs));
    //compute outputs
    outputs = function != null ? function.apply(t, inputs) : new double[nOfOutputs];
    //apply inputs
    Grid<Double> controlSignals = Grid.create(voxels.getW(), voxels.getH());
    int c = 0;
    for (Grid.Entry<? extends SensingVoxel> entry : voxels) {
      if (entry.getValue() != null) {
        if (c < outputs.length) {
          controlSignals.set(entry.getX(), entry.getY(), outputs[c]);
          c = c + 1;
        }
      }
    }
    return controlSignals;
  }

  @Override
  public void reset() {
    if (function instanceof Resettable) {
      ((Resettable) function).reset();
    }
  }

  @Override
  public String toString() {
    return "CentralizedSensing{" +
        "function=" + function +
        '}';
  }

  @Override
  public Snapshot getSnapshot() {
    Snapshot snapshot = new Snapshot(
        new StackedScopedReadings(
            new ScopedReadings(inputs, inputDomains),
            new ScopedReadings(outputs, outputDomains)
        ),
        getClass()
    );
    if (function instanceof Snapshottable) {
      snapshot.getChildren().add(((Snapshottable) function).getSnapshot());
    }
    return snapshot;
  }

}
