/*
 * Copyright (C) 2019 Eric Medvet <eric.medvet@gmail.com>
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

public class BoundingBox {

  public final Point2 min;
  public final Point2 max;

  public static BoundingBox build(Point2... points) {
    if (points.length == 0) {
      throw new IllegalArgumentException("Cannot build on 0 points");
    }
    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    for (Point2 point : points) {
      minX = Math.min(minX, point.x);
      maxX = Math.max(maxX, point.x);
      minY = Math.min(minY, point.y);
      maxY = Math.max(maxY, point.y);
    }
    return new BoundingBox(
        new Point2(minX, minY),
        new Point2(maxX, maxY)
    );
  }

  private BoundingBox(Point2 min, Point2 max) {
    this.min = min;
    this.max = max;
  }

  public static BoundingBox largest(BoundingBox bb1, BoundingBox bb2) {
    return BoundingBox.build(bb1.min, bb1.max, bb2.min, bb2.max);
  }
}
