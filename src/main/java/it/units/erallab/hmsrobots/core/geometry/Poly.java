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
package it.units.erallab.hmsrobots.core.geometry;

import java.io.Serializable;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public record Poly(Point2[] vertexes) implements Shape, Serializable {

  public static Poly of(Point2... vertexes) {
    return new Poly(vertexes);
  }

  @Override
  public BoundingBox boundingBox() {
    return BoundingBox.of(vertexes);
  }

  @Override
  public Point2 center() {
    return Point2.average(vertexes);
  }

  @Override
  public double area() {
    double a = 0d;
    int l = vertexes.length;
    for (int i = 0; i < l; i++) {
      a = a + vertexes[i].x() * (vertexes[(l + i + 1) % l].y() - vertexes[(l + i - 1) % l].y());
    }
    a = 0.5d * Math.abs(a);
    return a;
  }

}
