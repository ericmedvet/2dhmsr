/*
 * Copyright (C) 2020 Eric Medvet <eric.medvet@gmail.com>
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
package it.units.erallab.hmsrobots.objects.immutable;

import java.util.Collections;
import java.util.List;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class ImmutableObject {

  private final Class<? extends Object> objectClass;
  private final int objectHashCode;
  private final Shape shape;
  private final List<ImmutableObject> children;

  public ImmutableObject(Object object, Shape shape, List<ImmutableObject> children) {
    objectClass = object.getClass();
    objectHashCode = object.hashCode();
    this.shape = shape;
    this.children = Collections.unmodifiableList(children);
  }

  public ImmutableObject(Object object, Shape shape) {
    objectClass = object.getClass();
    objectHashCode = object.hashCode();
    this.shape = shape;
    this.children = Collections.EMPTY_LIST;
  }

  public Class<? extends Object> getObjectClass() {
    return objectClass;
  }

  public int getObjectHashCode() {
    return objectHashCode;
  }

  public Shape getShape() {
    return shape;
  }

  public List<ImmutableObject> getChildren() {
    return children;
  }
}
