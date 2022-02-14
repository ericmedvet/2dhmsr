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
import it.units.erallab.hmsrobots.core.geometry.Poly;
import it.units.erallab.hmsrobots.core.geometry.Shape;
import it.units.erallab.hmsrobots.core.objects.BreakableVoxel;

import java.util.Map;

/**
 * @author "Eric Medvet" on 2021/08/13 for 2dhmsr
 */
public class VoxelPoly implements Shape {

  private final Poly poly;
  private final double angle;
  private final Point2 linearVelocity;
  private final boolean isTouchingGround;
  private final double areaRatio;
  private final double areaRatioEnergy;
  private final double lastAppliedForce;
  private final double controlEnergy;
  private final Map<BreakableVoxel.ComponentType, BreakableVoxel.MalfunctionType> malfunctions;

  public VoxelPoly(
      Poly poly,
      double angle,
      Point2 linearVelocity,
      boolean isTouchingGround,
      double areaRatio,
      double areaRatioEnergy
  ) {
    this(poly, angle, linearVelocity, isTouchingGround, areaRatio, areaRatioEnergy, 0d, 0d);
  }

  public VoxelPoly(
      Poly poly,
      double angle,
      Point2 linearVelocity,
      boolean isTouchingGround,
      double areaRatio,
      double areaRatioEnergy,
      double lastAppliedForce,
      double controlEnergy
  ) {
    this(
        poly,
        angle,
        linearVelocity,
        isTouchingGround,
        areaRatio,
        areaRatioEnergy,
        lastAppliedForce,
        controlEnergy,
        Map.of()
    );
  }

  public VoxelPoly(
      Poly poly,
      double angle,
      Point2 linearVelocity,
      boolean isTouchingGround,
      double areaRatio,
      double areaRatioEnergy,
      double lastAppliedForce,
      double controlEnergy,
      Map<BreakableVoxel.ComponentType, BreakableVoxel.MalfunctionType> malfunctions
  ) {
    this.poly = poly;
    this.angle = angle;
    this.linearVelocity = linearVelocity;
    this.isTouchingGround = isTouchingGround;
    this.areaRatio = areaRatio;
    this.areaRatioEnergy = areaRatioEnergy;
    this.lastAppliedForce = lastAppliedForce;
    this.controlEnergy = controlEnergy;
    this.malfunctions = malfunctions;
  }

  @Override
  public BoundingBox boundingBox() {
    return poly.boundingBox();
  }

  @Override
  public double area() {
    return poly.area();
  }

  @Override
  public Point2 center() {
    return poly.center();
  }

  public double getAngle() {
    return angle;
  }

  public double getAreaRatio() {
    return areaRatio;
  }

  public double getAreaRatioEnergy() {
    return areaRatioEnergy;
  }

  public double getControlEnergy() {
    return controlEnergy;
  }

  public double getLastAppliedForce() {
    return lastAppliedForce;
  }

  public Point2 getLinearVelocity() {
    return linearVelocity;
  }

  public Map<BreakableVoxel.ComponentType, BreakableVoxel.MalfunctionType> getMalfunctions() {
    return malfunctions;
  }

  public boolean isTouchingGround() {
    return isTouchingGround;
  }

  public Point2[] vertexes() {
    return poly.vertexes();
  }
}
