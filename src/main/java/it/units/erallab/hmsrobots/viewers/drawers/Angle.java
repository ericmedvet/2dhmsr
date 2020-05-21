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

import it.units.erallab.hmsrobots.core.objects.immutable.Immutable;
import it.units.erallab.hmsrobots.core.objects.immutable.Voxel;
import it.units.erallab.hmsrobots.util.Configurable;
import it.units.erallab.hmsrobots.util.ConfigurableField;
import it.units.erallab.hmsrobots.util.Point2;
import it.units.erallab.hmsrobots.util.Poly;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;

import java.awt.*;


public class Angle extends Drawer<it.units.erallab.hmsrobots.core.sensors.immutable.Angle> implements Configurable<Angle> {

  @ConfigurableField
  private final Color strokeColor = Color.BLACK;

  private Angle() {
    super(it.units.erallab.hmsrobots.core.sensors.immutable.Angle.class);
  }

  public static Angle build() {
    return new Angle();
  }

  @Override
  public boolean draw(it.units.erallab.hmsrobots.core.sensors.immutable.Angle immutable, Immutable parent, Graphics2D g) {
    double angle = immutable.getValues()[0];
    Voxel voxel = (Voxel) parent;
    Point2 center = voxel.getShape().center();
    double radius = Math.sqrt(((Poly) voxel.getShape()).area()) / 2d;
    g.setColor(strokeColor);
    g.draw(GraphicsDrawer.toPath(
        center,
        Point2.build(
            center.x + radius * Math.cos(angle),
            center.y + radius * Math.sin(angle)
        )
    ));
    return false;
  }
}
