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
package it.units.erallab.hmsrobots.objects.snapshot;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Component {

  public static enum Type {
    RIGID, CONNECTION, ENCLOSING
  }
  
  private final Type type;
  private final Poly poly;

  public Component(Type type, Poly poly) {
    this.type = type;
    this.poly = poly;
  }

  public Type getType() {
    return type;
  }

  public Poly getPoly() {
    return poly;
  }

  @Override
  public String toString() {
    return "Component{" + "type=" + type + ", poly=" + poly + '}';
  }

}
