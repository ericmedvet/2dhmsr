/*
 * Copyright (C) 2019 eric
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.objects.immutable;

import it.units.erallab.hmsrobots.objects.Voxel;
import java.util.EnumMap;

/**
 *
 * @author eric
 */
public class VoxelComponent extends Component {

  private final double sideLength;
  private final double lastAppliedForce;
  private final EnumMap<Voxel.Sensor, Double> sensorReadings;

  public VoxelComponent(double sideLength, double lastAppliedForce, EnumMap<Voxel.Sensor, Double> sensorReadings, Poly poly) {
    super(Type.ENCLOSING, poly);
    this.sideLength = sideLength;
    this.lastAppliedForce = lastAppliedForce;
    this.sensorReadings = sensorReadings;
  }

  public double getSideLength() {
    return sideLength;
  }

  public double getLastAppliedForce() {
    return lastAppliedForce;
  }

  public EnumMap<Voxel.Sensor, Double> getSensorReadings() {
    return sensorReadings;
  }

}
