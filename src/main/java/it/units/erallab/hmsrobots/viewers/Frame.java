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
package it.units.erallab.hmsrobots.viewers;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Frame {

  private final double x1;
  private final double x2;
  private final double y1;
  private final double y2;

  public Frame(double x1, double x2, double y1, double y2) {
    this.x1 = x1;
    this.x2 = x2;
    this.y1 = y1;
    this.y2 = y2;
  }

  public double getX1() {
    return x1;
  }

  public double getX2() {
    return x2;
  }

  public double getY1() {
    return y1;
  }

  public double getY2() {
    return y2;
  }

  @Override
  public String toString() {
    return String.format("Frame{(%6.1f,%6.1f)->(%6.1f,%6.1f)}", x1, y1, x2, y2);
  }

}
