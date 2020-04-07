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

import com.google.common.collect.Lists;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public interface Configuration<C extends Configuration> extends Serializable {

  default Set<String> configurables(Configurable.Type... types) {
    EnumSet<Configurable.Type> set = EnumSet.noneOf(Configurable.Type.class);
    set.addAll(Lists.newArrayList(types));
    Set<String> annotated = FieldUtils.getFieldsListWithAnnotation(getClass(), Configurable.class).stream()
        .filter(f -> set.isEmpty() || set.contains(f.getAnnotation(Configurable.class).type()))
        .map(Field::getName).collect(Collectors.toSet());
    return annotated;
  }

  default Map<String, Object> getConfigurables() {
    Map<String, Object> map = new LinkedHashMap<>();
    for (String key : configurables()) {
      map.put(key, getConfigurable(key));
    }
    return map;
  }

  default void setConfigurables(Map<String, Object> map) {
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      setConfigurable(entry.getKey(), entry.getValue());
    }
  }

  default Object getConfigurable(String key) {
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

  default C setConfigurable(String key, Object value) {
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

  default Class typeOfConfigurable(String key) {
    Field field = FieldUtils.getField(this.getClass(), key, true);
    if ((field == null) || ((field.getAnnotation(Configurable.class) == null))) {
      Logger.getLogger(Configuration.class.getName()).log(Level.WARNING, String.format("Cannot get type of %s", key));
      return null;
    }
    return field.getType();
  }

}
