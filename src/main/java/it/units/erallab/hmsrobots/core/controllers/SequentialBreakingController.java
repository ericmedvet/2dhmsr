/*
 * Copyright (C) 2020 Eric Medvet <eric.medvet@gmail.com> (as eric)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.core.controllers;

import it.units.erallab.hmsrobots.core.objects.BreakableVoxel;
import it.units.erallab.hmsrobots.core.objects.ControllableVoxel;
import it.units.erallab.hmsrobots.util.Grid;

import java.util.*;
import java.util.stream.Collectors;

public class SequentialBreakingController<V extends BreakableVoxel> implements Controller<V> {

  private final Controller<V> controller;
  private final double breakingInterval;
  private final Random random;
  private final Map<BreakableVoxel.ComponentType, Set<BreakableVoxel.MalfunctionType>> malfunctions;

  private double lastBreakT = 0d;
  private double lastT = 0d;

  public SequentialBreakingController(Controller<V> controller, double breakingInterval, Random random, Map<BreakableVoxel.ComponentType, Set<BreakableVoxel.MalfunctionType>> malfunctions) {
    this.controller = controller;
    this.breakingInterval = breakingInterval;
    this.random = random;
    this.malfunctions = new EnumMap<>(BreakableVoxel.ComponentType.class);
    for (Map.Entry<BreakableVoxel.ComponentType, Set<BreakableVoxel.MalfunctionType>> entry : malfunctions.entrySet()) {
      this.malfunctions.put(entry.getKey(), EnumSet.copyOf(entry.getValue()));
    }
  }

  @Override
  public void control(double t, Grid<V> voxels) {
    double controlInterval = t - lastT;
    lastT = t;
    double n = breakingInterval / controlInterval;
    double k = 2d / n / (n + 1);
    double i = (t - lastBreakT) / controlInterval;
    double p = k * i;
    if (random.nextDouble() < p) {
      lastBreakT = t;
      List<V> goodVoxels = voxels.values().stream()
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
      if (!goodVoxels.isEmpty()) {
        List<Double> energies = goodVoxels.stream()
            .map(ControllableVoxel::getControlEnergy)
            .collect(Collectors.toList());
        int index = randomIndex(energies, random);
        if (!malfunctions.isEmpty()) {
          BreakableVoxel.ComponentType componentType = random(malfunctions.keySet(), random);
          Set<BreakableVoxel.MalfunctionType> types = malfunctions.get(componentType);
          if (!types.isEmpty()) {
            BreakableVoxel.MalfunctionType malfunctionType = random(types, random);
            goodVoxels.get(index).setMalfunctionType(componentType, malfunctionType);
          }
        }
      }
    }
    controller.control(t, voxels);
  }

  private static int randomIndex(List<Double> values, Random random) {
    double[] sums = new double[values.size()];
    for (int i = 0; i < sums.length; i++) {
      sums[i] = values.get(i);
      if (i > 0) {
        sums[i] = sums[i] + sums[i - 1];
      }
    }
    double r = random.nextDouble() * sums[sums.length - 1];
    for (int i = 0; i < sums.length; i++) {
      if (r < sums[i]) {
        return i;
      }
    }
    throw new IllegalArgumentException("Empty list of values");
  }

  private <T> T random(Set<T> set, Random random) {
    return (T) set.toArray()[random.nextInt(set.size())];
  }

}
