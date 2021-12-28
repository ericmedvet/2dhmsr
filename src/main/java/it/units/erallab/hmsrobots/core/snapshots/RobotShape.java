/*
 * Copyright (c) "Eric Medvet" 2021.
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

package it.units.erallab.hmsrobots.core.snapshots;

import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.geometry.Shape;
import it.units.erallab.hmsrobots.util.Grid;

/**
 * @author "Eric Medvet" on 2021/09/17 for 2dhmsr
 */
public class RobotShape implements Shape {
  private final Grid<? extends VoxelPoly> polies;
  private final BoundingBox boundingBox;

  public RobotShape(Grid<? extends VoxelPoly> polies, BoundingBox boundingBox) {
    this.polies = polies;
    this.boundingBox = boundingBox;
  }

  @Override
  public BoundingBox boundingBox() {
    return boundingBox;
  }

  @Override
  public Point2 center() {
    return boundingBox.center();
  }


  public Grid<? extends VoxelPoly> getPolies() {
    return polies;
  }
}
