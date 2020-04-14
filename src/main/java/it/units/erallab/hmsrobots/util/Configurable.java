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

public interface Configurable<C extends Configurable> extends Serializable {

  static <T extends Configurable<T>> T build(Class<T> type) {
    try {
      Method buildMethod = type.getDeclaredMethod("build");
      T newInstance = (T) buildMethod.invoke(null);
      return newInstance;
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  static <T extends Configurable<T>> T build(Configuration<T> configuration) {
    T newInstance = build(configuration.getType());
    newInstance.configure(configuration);
    return newInstance;
  }

  default Set<String> configurables(ConfigurableField.Type... types) {
    EnumSet<ConfigurableField.Type> set = EnumSet.noneOf(ConfigurableField.Type.class);
    set.addAll(Lists.newArrayList(types));
    Set<String> annotated = FieldUtils.getFieldsListWithAnnotation(getClass(), ConfigurableField.class).stream()
        .filter(f -> set.isEmpty() || set.contains(f.getAnnotation(ConfigurableField.class).uiType()))
        .map(Field::getName).collect(Collectors.toSet());
    return annotated;
  }

  default Configuration<C> toConfiguration() {
    Map<String, Configuration<?>> map = new LinkedHashMap<>();
    for (String key : configurables()) {
      Configuration<?> configuration = toConfiguration(getConfigurable(key));
      if (configuration != null) {
        map.put(key, configuration);
      }
    }
    return (Configuration<C>) Configuration.of(getClass(), map);
  }

  private static Configuration<?> toConfiguration(Object object) {
    if (object == null) {
      return null;
    }
    if (object instanceof Configurable) {
      return Configuration.of(object.getClass(), ((Configurable<?>) object).toConfiguration());
    }
    if (object instanceof List) {
      object = ((List<?>) object).stream()
          .map(Configurable::toConfiguration)
          .collect(Collectors.toList());
    }
    if (object instanceof Set) {
      object = ((Set<?>) object).stream()
          .map(Configurable::toConfiguration)
          .collect(Collectors.toSet());
    }
    if (object instanceof Map) {
      object = ((Map<?, ?>) object).entrySet().stream()
          .map(e -> Pair.of(toConfiguration(e.getKey()), toConfiguration(e.getValue())))
          .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }
    return Configuration.of(object.getClass(), object);
  }

  private static Object fromConfiguration(Configuration<?> configuration) {
    if (Lists.newArrayList(configuration.getType().getInterfaces()).contains(Configurable.class)) {
      return build((Configuration) configuration);
    }
    if (configuration.getValue() instanceof List) {
      return ((List<?>) configuration.getValue()).stream()
          .map(p -> fromConfiguration((Configuration<?>) p))
          .collect(Collectors.toList());
    }
    if (configuration.getValue() instanceof Set) {
      return ((Set<?>) configuration.getValue()).stream()
          .map(p -> fromConfiguration((Configuration<?>) p))
          .collect(Collectors.toSet());
    }
    if (configuration.getValue() instanceof Map) {
      return ((Map<?, ?>) configuration.getValue()).entrySet().stream()
          .map(e -> Pair.of(
              fromConfiguration((Configuration<?>) e.getKey()),
              fromConfiguration((Configuration<?>) e.getValue())))
          .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }
    return configuration.getValue();
  }

  default C configure(Configuration<C> configuration) {
    Map<String, Configuration<?>> map = (Map<String, Configuration<?>>) configuration.getValue();
    for (Map.Entry<String, Configuration<?>> entry : map.entrySet()) {
      setConfigurable(entry.getKey(), fromConfiguration(entry.getValue()));
    }
    return (C) this;
  }

  default Object getConfigurable(String key) {
    Field field = FieldUtils.getField(getClass(), key, true);
    if ((field == null) || ((field.getAnnotation(ConfigurableField.class) == null))) {
      Logger.getLogger(Configurable.class.getName()).log(Level.WARNING, String.format("Cannot get value of %s", key));
      return null;
    }
    try {
      return FieldUtils.readField(field, this);
    } catch (IllegalAccessException e) {
      Logger.getLogger(Configurable.class.getName()).log(Level.WARNING, String.format("Cannot get value of %s", key), e);
      return null;
    }
  }

  default C setConfigurable(String key, Object value) {
    Field field = FieldUtils.getField(getClass(), key, true);
    if ((field == null) || ((field.getAnnotation(ConfigurableField.class) == null))) {
      Logger.getLogger(Configurable.class.getName()).log(Level.WARNING, String.format("Cannot set value of %s", key));
    } else {
      try {
        FieldUtils.writeField(field, this, value);
      } catch (IllegalAccessException e) {
        Logger.getLogger(Configurable.class.getName()).log(Level.WARNING, String.format("Cannot set value of %s", key), e);
      }
    }
    return (C) this;
  }

  default Class typeOfConfigurable(String key) {
    Field field = FieldUtils.getField(this.getClass(), key, true);
    if ((field == null) || ((field.getAnnotation(ConfigurableField.class) == null))) {
      Logger.getLogger(Configurable.class.getName()).log(Level.WARNING, String.format("Cannot get type of %s", key));
      return null;
    }
    return field.getType();
  }

}
