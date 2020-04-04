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
package it.units.erallab.hmsrobots.viewers.drawers;

import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.objects.immutable.ImmutableObject;
import it.units.erallab.hmsrobots.objects.immutable.Poly;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;

import java.awt.*;
import java.util.Collections;
import java.util.Set;

public class VoxelDrawer implements Drawer {

  private final static Set<Class<? extends Object>> CLASSES = Collections.unmodifiableSet(Set.of(
      Voxel.class
  ));

  @Override
  public boolean draw(ImmutableObject object, Graphics2D g) {
    Poly poly = (Poly) object.getShape();
    g.setColor(Color.GREEN);
    g.draw(GraphicsDrawer.toPath(poly, true));
    return true;
  }

  @Override
  public Set<Class<? extends Object>> getDrawableClasses() {
    return CLASSES;
  }
}
