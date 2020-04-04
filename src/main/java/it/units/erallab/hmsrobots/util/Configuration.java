/*
 * Copyright (C) 2020 Eric Medvet <eric.medvet@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.units.erallab.hmsrobots.util;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public interface Configuration<C extends Configuration> extends Serializable {

  public default Set<String> keys(Configurable.Type type) {
    Set<String> annotated = FieldUtils.getFieldsListWithAnnotation(getClass(), Configurable.class).stream()
        .filter(f -> f.getAnnotation(Configurable.class).type().equals(type))
        .map(Field::getName).collect(Collectors.toSet());
    return annotated;
  }

  public default Object value(String key) {
    Field field = FieldUtils.getField(getClass(), key, true);
    if ((field == null) || ((field.getAnnotation(Configurable.class) == null))) {
      Logger.getLogger(Configuration.class.getName()).log(Level.WARNING, String.format("Cannot get value of %s", key));
      return null;
    }
    try {
      return FieldUtils.readField(field, this);
    } catch (IllegalAccessException e) {
      Logger.getLogger(Configuration.class.getName()).log(Level.WARNING, String.format("Cannot get value of %s", key), e);
      return null;
    }
  }

  public default C set(String key, Object value) {
    Field field = FieldUtils.getField(getClass(), key, true);
    if ((field == null) || ((field.getAnnotation(Configurable.class) == null))) {
      Logger.getLogger(Configuration.class.getName()).log(Level.WARNING, String.format("Cannot set value of %s", key));
    } else {
      try {
        FieldUtils.writeField(field, this, value);
      } catch (IllegalAccessException e) {
        Logger.getLogger(Configuration.class.getName()).log(Level.WARNING, String.format("Cannot set value of %s", key), e);
      }
    }
    return (C) this;
  }

  public default Class type(String key) {
    Field field = FieldUtils.getField(this.getClass(), key, true);
    if ((field == null) || ((field.getAnnotation(Configurable.class) == null))) {
      Logger.getLogger(Configuration.class.getName()).log(Level.WARNING, String.format("Cannot get type of %s", key));
      return null;
    }
    return field.getType();
  }

}
