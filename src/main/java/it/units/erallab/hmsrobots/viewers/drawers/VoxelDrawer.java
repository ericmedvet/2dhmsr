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

import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.objects.immutable.ImmutableObject;
import it.units.erallab.hmsrobots.objects.immutable.ImmutableVoxel;
import it.units.erallab.hmsrobots.objects.immutable.Poly;
import it.units.erallab.hmsrobots.util.Configurable;
import it.units.erallab.hmsrobots.util.ConfigurableField;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;

import java.awt.*;
import java.awt.geom.Path2D;

public class VoxelDrawer implements Configurable<VoxelDrawer>, Drawer {

  @ConfigurableField
  private final Color strokeColor = Color.BLUE;
  @ConfigurableField
  private final Color restFillColor = GraphicsDrawer.alphaed(Color.YELLOW, 0.5f);
  @ConfigurableField
  private final Color shrunkFillColor = GraphicsDrawer.alphaed(Color.RED, 0.5f);
  @ConfigurableField
  private final Color expandedFillColor = GraphicsDrawer.alphaed(Color.GREEN, 0.5f);
  @ConfigurableField(uiMin = 0.1f, uiMax = 0.999f)
  private final float shrunkRatio = 0.75f;
  @ConfigurableField(uiMin = 1.001f, uiMax = 2f)
  private final float expandendRatio = 1.25f;

  private VoxelDrawer() {
  }

  public static VoxelDrawer build() {
    return new VoxelDrawer();
  }

  @Override
  public boolean draw(ImmutableObject object, Graphics2D g) {
    ImmutableVoxel voxel = (ImmutableVoxel) object;
    Poly poly = (Poly) voxel.getShape();
    Path2D path = GraphicsDrawer.toPath(poly, true);
    g.setColor(strokeColor);
    g.draw(path);
    g.setColor(GraphicsDrawer.linear(
        shrunkFillColor, restFillColor, expandedFillColor,
        shrunkRatio, 1f, expandendRatio,
        (float) (poly.area() / voxel.getRestArea())
    ));
    g.fill(path);
    return true;
  }

  @Override
  public boolean canDraw(Class c) {
    return Voxel.class.isAssignableFrom(c);
  }
}
