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

public class Vector implements Shape {

  private final Point2 start;
  private final Point2 end;

  private Vector(Point2 start, Point2 end) {
    this.start = start;
    this.end = end;
  }

  public static Vector build(Point2 start, Point2 end) {
    return new Vector(start, end);
  }

  @Override
  public BoundingBox boundingBox() {
    return BoundingBox.build(start, end);
  }

  @Override
  public Point2 center() {
    return Point2.average(start, end);
  }

  public Point2 getStart() {
    return start;
  }

  public Point2 getEnd() {
    return end;
  }
}
