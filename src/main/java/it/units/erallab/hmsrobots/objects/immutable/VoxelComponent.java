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

/**
 *
 * @author eric
 */
public class VoxelComponent extends Component {
  
  private final double lastAppliedForce;
  private final double restArea;
  private final double lastArea;

  public VoxelComponent(double lastAppliedForce, double restArea, double lastArea, Poly poly) {
    super(Type.ENCLOSING, poly);
    this.lastAppliedForce = lastAppliedForce;
    this.restArea = restArea;
    this.lastArea = lastArea;
  }

  public double getLastAppliedForce() {
    return lastAppliedForce;
  }

  public double getRestArea() {
    return restArea;
  }

  public double getLastArea() {
    return lastArea;
  }
  
}
