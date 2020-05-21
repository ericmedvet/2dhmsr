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
package it.units.erallab.hmsrobots.util;

import org.dyn4j.geometry.Vector2;

/**
 * @author eric
 */
public class Point2 implements Shape {

  public final double x;
  public final double y;

  private Point2(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public static Point2 build(double x, double y) {
    return new Point2(x, y);
  }

  public static Point2 build(Vector2 v) {
    return new Point2(v.x, v.y);
  }

  public static Point2 average(Point2... points) {
    double cx = 0;
    double cy = 0;
    double n = 0;
    for (Point2 point : points) {
      cx = cx + point.x;
      cy = cy + point.y;
      n = n + 1;
    }
    return Point2.build(cx / n, cy / n);
  }

  @Override
  public BoundingBox boundingBox() {
    return BoundingBox.build(this);
  }

  @Override
  public Point2 center() {
    return this;
  }

  @Override
  public String toString() {
    return String.format("(%5.3f, %5.3f)", x, y);
  }

}
