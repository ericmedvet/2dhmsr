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
package it.units.erallab.hmsrobots.core.geometry;

import java.io.Serializable;

public record BoundingBox(Point2 min, Point2 max) implements Shape, Serializable {

  public static BoundingBox largest(BoundingBox bb1, BoundingBox bb2) {
    return BoundingBox.of(bb1.min, bb1.max, bb2.min, bb2.max);
  }

  public static BoundingBox of(Point2... points) {
    if (points.length == 0) {
      throw new IllegalArgumentException("Cannot build on 0 points");
    }
    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    for (Point2 point : points) {
      minX = Math.min(minX, point.x());
      maxX = Math.max(maxX, point.x());
      minY = Math.min(minY, point.y());
      maxY = Math.max(maxY, point.y());
    }
    return new BoundingBox(Point2.of(minX, minY), Point2.of(maxX, maxY));
  }

  public static BoundingBox of(double minX, double minY, double maxX, double maxY) {
    return of(Point2.of(minX, minY), Point2.of(maxX, maxY));
  }

  @Override
  public BoundingBox boundingBox() {
    return this;
  }

  @Override
  public double area() {
    return width() * height();
  }

  @Override
  public Point2 center() {
    return Point2.of((min.x() + max.x()) / 2d, (min.y() + max.y()) / 2d);
  }

  public double height() {
    return max.y() - min.y();
  }

  public double width() {
    return max.x() - min.x();
  }
}
