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
package it.units.erallab.hmsrobots.util;

import java.io.Serializable;

public class Configuration<T> implements Serializable {
  private final Class<T> type;
  private final Object value;

  public static <C> Configuration<C> of(Class<C> type, Object value) {
    return new Configuration<>(type, value);
  }

  private Configuration(Class<T> type, Object value) {
    this.type = type;
    this.value = value;
  }

  public Class<T> getType() {
    return type;
  }

  public Object getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "(" + type.getSimpleName() + ") " + value;
  }
}
