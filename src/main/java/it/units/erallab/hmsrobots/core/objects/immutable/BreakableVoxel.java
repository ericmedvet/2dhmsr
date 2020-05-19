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
package it.units.erallab.hmsrobots.core.objects.immutable;

import it.units.erallab.hmsrobots.util.Shape;

public class BreakableVoxel extends ControllableVoxel {

  private final it.units.erallab.hmsrobots.core.objects.BreakableVoxel.MalfunctionType actuatorMalfunctionType;
  private final it.units.erallab.hmsrobots.core.objects.BreakableVoxel.MalfunctionType sensorsMalfunctionType;
  private final it.units.erallab.hmsrobots.core.objects.BreakableVoxel.MalfunctionType structureMalfunctionType;

  public BreakableVoxel(Shape shape, double areaRatio, double appliedForce, double controlEnergy, double controlEnergyDelta, it.units.erallab.hmsrobots.core.objects.BreakableVoxel.MalfunctionType actuatorMalfunctionType, it.units.erallab.hmsrobots.core.objects.BreakableVoxel.MalfunctionType sensorsMalfunctionType, it.units.erallab.hmsrobots.core.objects.BreakableVoxel.MalfunctionType structureMalfunctionType) {
    super(shape, areaRatio, appliedForce, controlEnergy, controlEnergyDelta);
    this.actuatorMalfunctionType = actuatorMalfunctionType;
    this.sensorsMalfunctionType = sensorsMalfunctionType;
    this.structureMalfunctionType = structureMalfunctionType;
  }

  public it.units.erallab.hmsrobots.core.objects.BreakableVoxel.MalfunctionType getActuatorMalfunctionType() {
    return actuatorMalfunctionType;
  }

  public it.units.erallab.hmsrobots.core.objects.BreakableVoxel.MalfunctionType getSensorsMalfunctionType() {
    return sensorsMalfunctionType;
  }

  public it.units.erallab.hmsrobots.core.objects.BreakableVoxel.MalfunctionType getStructureMalfunctionType() {
    return structureMalfunctionType;
  }
}
