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
package it.units.erallab.hmsrobots.viewers.drawers;

import it.units.erallab.hmsrobots.objects.immutable.ImmutableObject;
import it.units.erallab.hmsrobots.objects.immutable.ImmutableReading;
import it.units.erallab.hmsrobots.objects.immutable.Point2;
import it.units.erallab.hmsrobots.objects.immutable.Poly;
import it.units.erallab.hmsrobots.util.Configurable;
import it.units.erallab.hmsrobots.util.ConfigurableField;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;

import java.awt.*;


public class Angle implements Configurable<Angle>, Drawer {

  @ConfigurableField
  private final Color strokeColor = Color.BLACK;

  private Angle() {
  }

  public static Angle build() {
    return new Angle();
  }

  @Override
  public boolean draw(ImmutableObject object, Graphics2D g) {
    Poly voxelPoly = (Poly) object.getShape();
    double radius = Math.sqrt(voxelPoly.area()) / 2d;
    Point2 center = voxelPoly.center();
    double angle = ((ImmutableReading) object).getValues()[0];
    g.setColor(strokeColor);
    g.draw(GraphicsDrawer.toPath(
        center,
        Point2.build(center.x + angle * Math.cos(angle), center.y + angle * Math.sin(angle))
    ));
    return false;
  }

  @Override
  public boolean canDraw(Class c) {
    return it.units.erallab.hmsrobots.sensors.Angle.class.isAssignableFrom(c);
  }
}
