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

import java.awt.*;

public abstract class Drawer<I extends Immutable> {

  private final Class<I> drawableClass;

  protected Drawer(Class<I> drawableClass) {
    this.drawableClass = drawableClass;
  }

  public abstract boolean draw(I immutable, Immutable parent, Graphics2D g);

  public boolean canDraw(Class<? extends Immutable> immutableClass) {
    return drawableClass.isAssignableFrom(immutableClass);
  }

}
