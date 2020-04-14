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
import it.units.erallab.hmsrobots.objects.immutable.Vector;
import it.units.erallab.hmsrobots.util.Configurable;
import it.units.erallab.hmsrobots.util.ConfigurableField;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;

import java.awt.*;

public class Joint implements Configurable<Joint>, Drawer {

  @ConfigurableField
  private final Color color = Color.RED;
  @ConfigurableField(uiMin = 1, uiMax = 5)
  private final float strokeWidth = 2f;

  private Joint() {
  }

  public static Joint build() {
    return new Joint();
  }

  @Override
  public boolean draw(ImmutableObject object, Graphics2D g) {
    Vector vector = (Vector) object.getShape();
    g.setStroke(new BasicStroke(strokeWidth / (float) g.getTransform().getScaleX()));
    g.setColor(color);
    g.draw(GraphicsDrawer.toPath(vector.getStart(), vector.getEnd()));
    return false;
  }

  @Override
  public boolean canDraw(Class c) {
    return org.dyn4j.dynamics.joint.Joint.class.isAssignableFrom(c);
  }
}
