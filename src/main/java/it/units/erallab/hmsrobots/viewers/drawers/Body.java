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
import it.units.erallab.hmsrobots.objects.immutable.Poly;
import it.units.erallab.hmsrobots.util.Configurable;
import it.units.erallab.hmsrobots.util.ConfigurableField;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;

import java.awt.*;
import java.awt.geom.Path2D;

public class Body implements Configurable<Body>, Drawer {

  @ConfigurableField
  private final Color strokeColor = Color.BLUE;
  @ConfigurableField
  private final Color fillColor = GraphicsDrawer.alphaed(Color.BLUE, 0.25f);

  private Body() {
  }

  public static Body build() {
    return new Body();
  }

  @Override
  public boolean draw(ImmutableObject object, Graphics2D g) {
    Path2D path = GraphicsDrawer.toPath((Poly) object.getShape(), true);
    if (strokeColor != null) {
      g.setColor(strokeColor);
      g.draw(path);
    }
    if (fillColor != null) {
      g.setColor(fillColor);
      g.fill(path);
    }
    return false;
  }

  @Override
  public boolean canDraw(Class c) {
    return org.dyn4j.dynamics.Body.class.isAssignableFrom(c);
  }
}
