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
package it.units.erallab.hmsrobots;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class WorldEvent {
  
  private final double time;
  private final Collection<List<Point2D>> shapes;

  public WorldEvent(double time, Collection<List<Point2D>> shapes) {
    this.time = time;
    this.shapes = shapes;
  }

  public double getTime() {
    return time;
  }

  public Collection<List<Point2D>> getShapes() {
    return shapes;
  }

  @Override
  public String toString() {
    return "WorldEvent{" + "time=" + time + ", shapes=" + shapes + '}';
  }    
  
}
