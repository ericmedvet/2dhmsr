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

import it.units.erallab.hmsrobots.objects.WorldObject;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Compound {
  
  private final Class<? extends WorldObject> objectClass;
  private final List<Component> components;

  public Compound(Class<? extends WorldObject> objectClass, List<Component> components) {
    this.objectClass = objectClass;
    this.components = components;
  }

  public Compound(Class<? extends WorldObject> objectClass, Component component) {
    this(objectClass, Collections.singletonList(component));
  }

  public Class<? extends WorldObject> getObjectClass() {
    return objectClass;
  }
  
  public List<Component> getComponents() {
    return components;
  }

  @Override
  public String toString() {
    return "Compound{" + "objectClass=" + objectClass + ", components=" + components + '}';
  }
  
}
