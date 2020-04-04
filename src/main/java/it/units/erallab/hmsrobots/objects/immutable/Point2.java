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

import org.dyn4j.geometry.Vector2;

import java.io.Serializable;

/**
 * @author eric
 */
public class Point2 implements Serializable {

  public final double x;
  public final double y;

  public Point2(double x, double y) {
    //TODO make private
    this.x = x;
    this.y = y;
  }

  public Point2(Vector2 v) {
    //TODO make private
    x = v.x;
    y = v.y;
  }

  public static Point2 build(double x, double y) {
    return new Point2(x, y);
  }

  public static Point2 mid(Point2 p1, Point2 p2) {
    return build((p1.x + p2.x) / 2d, (p1.y + p2.y) / 2d);
  }

  @Override
  public String toString() {
    return String.format("(%5.3f, %5.3f)", x, y);
  }

}
