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
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public interface Configuration<C extends Configuration> extends Serializable {

  static <T extends Configuration<T>> T build(Class<T> type) {
    try {
      Method buildMethod = type.getDeclaredMethod("build");
      T newInstance = (T) buildMethod.invoke(null);
      return newInstance;
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  static <T extends Configuration<T>> T build(Class<T> type, Map<String, Pair<Class<?>, Object>> map) {
    T newInstance = build(type);
    newInstance.configure(map);
    return newInstance;
  }

  default Set<String> configurables(Configurable.Type... types) {
    EnumSet<Configurable.Type> set = EnumSet.noneOf(Configurable.Type.class);
    set.addAll(Lists.newArrayList(types));
    Set<String> annotated = FieldUtils.getFieldsListWithAnnotation(getClass(), Configurable.class).stream()
        .filter(f -> set.isEmpty() || set.contains(f.getAnnotation(Configurable.class).type()))
        .map(Field::getName).collect(Collectors.toSet());
    return annotated;
  }

  default Map<String, Pair<Class<?>, Object>> toMap() {
    Map<String, Pair<Class<?>, Object>> map = new LinkedHashMap<>();
    for (String key : configurables()) {
      Pair<Class<?>, Object> pair = toPair(getConfigurable(key));
      if (pair != null) {
        map.put(key, pair);
      }
    }
    return map;
  }

  private static Pair<Class<?>, Object> toPair(Object object) {
    if (object == null) {
      return null;
    }
    if (object instanceof Configuration) {
      return Pair.of(object.getClass(), ((Configuration<?>) object).toMap());
    }
    if (object instanceof List) {
      object = ((List<?>) object).stream()
          .map(Configuration::toPair)
          .collect(Collectors.toList());
    }
    if (object instanceof Set) {
      object = ((Set<?>) object).stream()
          .map(Configuration::toPair)
          .collect(Collectors.toSet());
    }
    if (object instanceof Map) {
      object = ((Map<?, ?>) object).entrySet().stream()
          .map(e -> Pair.of(toPair(e.getKey()), toPair(e.getValue())))
          .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }
    return Pair.of(object.getClass(), object);
  }

  private static Object fromPair(Pair<Class<?>, Object> pair) {
    if (Lists.newArrayList(pair.getKey().getInterfaces()).contains(Configuration.class)) {
      return build((Class) pair.getKey(), (Map) pair.getValue());
    }
    if (pair.getValue() instanceof List) {
      return ((List<?>) pair.getValue()).stream()
          .map(p -> fromPair((Pair<Class<?>, Object>) p))
          .collect(Collectors.toList());
    }
    if (pair.getValue() instanceof Set) {
      return ((Set<?>) pair.getValue()).stream()
          .map(p -> fromPair((Pair<Class<?>, Object>) p))
          .collect(Collectors.toSet());
    }
    if (pair.getValue() instanceof Map) {
      return ((Map<?, ?>) pair.getValue()).entrySet().stream()
          .map(e -> Pair.of(fromPair((Pair<Class<?>, Object>) e.getKey()), fromPair((Pair<Class<?>, Object>) e.getValue())))
          .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }
    return pair.getValue();
  }

  default void configure(Map<String, Pair<Class<?>, Object>> map) {
    for (Map.Entry<String, Pair<Class<?>, Object>> entry : map.entrySet()) {
      setConfigurable(entry.getKey(), fromPair(entry.getValue()));
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
