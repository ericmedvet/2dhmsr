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

import java.util.List;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Poly implements Shape {

  private final Point2[] vertexes;

  public Poly(List<Point2> vertexes) {
    this.vertexes = new Point2[vertexes.size()];
    for (int i = 0; i < vertexes.size(); i++) {
      this.vertexes[i] = vertexes.get(i);
    }
  }

  private Poly(Point2... vertexes) {
    this.vertexes = vertexes;
  }

  public static Poly build(Point2... vertexes) {
    return new Poly(vertexes);
  }

  @Override
  public BoundingBox boundingBox() {
    return BoundingBox.build(vertexes);
  }

  public Point2[] getVertexes() {
    return vertexes;
  }

  public double area() {
    double a = 0d;
    int l = vertexes.length;
    for (int i = 0; i < l; i++) {
      a = a + vertexes[i].x * (vertexes[(l + i + 1) % l].y - vertexes[(l + i - 1) % l].y);
    }
    a = 0.5d * Math.abs(a);
    return a;
  }

  @Override
  public Point2 center() {
    return Point2.average(vertexes);
  }

}
