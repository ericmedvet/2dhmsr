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
package it.units.erallab.hmsrobots.viewers;

import it.units.erallab.hmsrobots.util.Configurable;
import it.units.erallab.hmsrobots.util.ConfigurableField;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.MutablePair;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigurablePane extends JPanel {

  public static class ColorChooserButton extends JButton {

    private final static int ICON_SIZE = 12;
    private Color color;
    private final List<ColorChangedListener> listeners = new ArrayList<>();

    public ColorChooserButton(Color color) {
      this.color = color;
      setMargin(new Insets(1, 1, 1, 1));
      addActionListener(e -> {
        Color newColor = JColorChooser.showDialog(null, "Choose a color", color);
        if (newColor != null) {
          setSelectedColor(newColor);
        }
      });
      setIcon(createIcon(color));
      repaint();
    }

    public Color getSelectedColor() {
      return color;
    }

    public void setSelectedColor(Color color) {
      this.color = color;
      setIcon(createIcon(color));
      repaint();
      for (ColorChangedListener l : listeners) {
        l.colorChanged(color);
      }
    }

    public void addColorChangedListener(ColorChangedListener listener) {
      listeners.add(listener);
    }

    public interface ColorChangedListener {
      void colorChanged(Color newColor);
    }

    public static ImageIcon createIcon(Color color) {
      BufferedImage image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = image.createGraphics();
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, ICON_SIZE, ICON_SIZE);
      graphics.setColor(color);
      graphics.fillPolygon(new int[]{0, ICON_SIZE, 0}, new int[]{0, 0, ICON_SIZE}, 3);
      graphics.setColor(GraphicsDrawer.alphaed(color, 1f));
      graphics.fillPolygon(new int[]{ICON_SIZE, 0, ICON_SIZE}, new int[]{0, ICON_SIZE, ICON_SIZE}, 3);
      image.flush();
      ImageIcon icon = new ImageIcon(image);
      return icon;
    }
  }

  private final Map<String, List<MutablePair<Object, Boolean>>> collectionValues = new LinkedHashMap<>();

  public ConfigurablePane(Configurable<?> configurable) {
    //set general properties
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    //add things
    add(new JLabel(configurable.getClass().getSimpleName()));
    for (String key : configurable.configurables(ConfigurableField.Type.BASIC, ConfigurableField.Type.ADVANCED)) {
      add(forAny(key, configurable));
    }
    add(Box.createVerticalGlue()); // TODO does not work as desired
  }

  private JComponent forAny(String key, Configurable<?> configurable) {
    Object value = configurable.getConfigurable(key);
    if (value == null) {
      return new JLabel(key + " (null)");
    } else if (value instanceof Configurable) {
      return new ConfigurablePane((Configurable<?>) value);
    } else if (value instanceof Number) {
      double num = ((Number) value).doubleValue();
      final Class<? extends Number> type = ((Number) value).getClass();
      final double min = Math.min(num, getUiMin(key, configurable) != null ? getUiMin(key, configurable).doubleValue() : 1d);
      final double max = Math.max(num, getUiMax(key, configurable) != null ? getUiMax(key, configurable).doubleValue() : 5d);
      JSlider slider = new JSlider(
          JSlider.HORIZONTAL,
          0,
          100,
          (int) Math.round(100 * (num - min) / (max - min))
      );
      slider.setMajorTickSpacing(100);
      slider.setMinorTickSpacing(10);
      slider.setPaintTicks(true);
      slider.setPreferredSize(new Dimension(100, 40));
      slider.addChangeListener(e -> configurable.setConfigurable(
          key,
          number(slider.getValue() / 100d * (max - min) + min, type)));
      return justified(new JLabel(key), null, slider);
    } else if (value instanceof Color) {
      ColorChooserButton button = new ColorChooserButton((Color) value);
      button.addColorChangedListener(c -> configurable.setConfigurable(key, c));
      return justified(new JLabel(key), null, button);
    } else if (value instanceof Boolean) {
      JCheckBox checkBox = new JCheckBox();
      checkBox.setSelected(((Boolean) value).booleanValue());
      checkBox.addActionListener(c -> configurable.setConfigurable(key, checkBox.isSelected()));
      return justified(new JLabel(key), null, checkBox);
    } else if (value instanceof Enum) {
      List<Object> values = List.of(value.getClass().getEnumConstants());
      JComboBox comboBox = new JComboBox(values.stream().map(Object::toString).toArray());
      comboBox.setSelectedIndex(values.indexOf(value));
      comboBox.addActionListener(e -> configurable.setConfigurable(key, values.get(comboBox.getSelectedIndex())));
      return justified(new JLabel(key), null, comboBox);
    } else if (value instanceof Collection) {
      List<MutablePair<Object, Boolean>> pairs;
      Collection<Object> collection = (Collection) value;
      pairs = collection.stream()
          .map(o -> MutablePair.of(o, true))
          .collect(Collectors.toList());
      Set<Object> otherValues = getEnumValues(key, configurable);
      otherValues.removeAll(collection);
      pairs.addAll(otherValues.stream()
          .map(o -> MutablePair.of(o, false))
          .collect(Collectors.toList()));
      collectionValues.put(key, pairs);
      JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
      panel.add(new JLabel(key + ":"));
      JPanel elementsPanel = new JPanel();
      elementsPanel.setLayout(new BoxLayout(elementsPanel, BoxLayout.PAGE_AXIS));
      elementsPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
      panel.add(elementsPanel);
      for (MutablePair<Object, Boolean> pair : collectionValues.get(key)) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(pair.getValue());
        checkBox.addActionListener(e -> {
          pair.setValue(!pair.getValue());
          configurable.setConfigurable(key, collection(
              collectionValues.get(key).stream()
                  .filter(p -> p.getValue())
                  .map(p -> p.getKey())
                  .collect(Collectors.toList()),
              collection
          ));
        });
        if (pair.getKey() instanceof Configurable) {
          elementsPanel.add(justified(new ConfigurablePane((Configurable) pair.getKey()), checkBox));
        } else {
          elementsPanel.add(justified(new JLabel(pair.getKey().toString()), null, checkBox));
        }
      }
      return panel;
    }
    return justified(new JLabel(key + " (" + value.getClass().getSimpleName() + ")"), null);
  }

  private static Number number(Double value, Class<? extends Number> type) {
    if (type.equals(Integer.class)) {
      return value.intValue();
    }
    if (type.equals(Float.class)) {
      return value.floatValue();
    }
    return value;
  }

  private static <T> Collection<T> collection(List<T> list, Collection<T> original) {
    if (original instanceof Set) {
      return new LinkedHashSet<>(list);
    }
    return list;
  }

  private static JPanel justified(JComponent... components) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
    addJustified(panel, components);
    return panel;
  }

  private static void addJustified(JPanel panel, JComponent... components) {
    Box box = Box.createHorizontalBox();
    for (JComponent component : components) {
      if (component == null) {
        box.add(Box.createHorizontalGlue());
      } else {
        box.add(component);
      }
    }
    panel.add(box);
  }

  private static Number getUiMin(String key, Configurable configurable) {
    Field field = FieldUtils.getField(configurable.getClass(), key, true);
    if ((field == null) || ((field.getAnnotation(ConfigurableField.class) == null))) {
      return null;
    }
    double uiMin = field.getAnnotation(ConfigurableField.class).uiMin();
    if (uiMin == Double.NEGATIVE_INFINITY) {
      return null;
    }
    return uiMin;
  }

  private static Number getUiMax(String key, Configurable configurable) {
    Field field = FieldUtils.getField(configurable.getClass(), key, true);
    if ((field == null) || ((field.getAnnotation(ConfigurableField.class) == null))) {
      return null;
    }
    double uiMax = field.getAnnotation(ConfigurableField.class).uiMax();
    if (uiMax == Double.POSITIVE_INFINITY) {
      return null;
    }
    return uiMax;
  }

  private static Set<Object> getEnumValues(String key, Configurable configurable) {
    Field field = FieldUtils.getField(configurable.getClass(), key, true);
    if ((field == null) || ((field.getAnnotation(ConfigurableField.class) == null))) {
      return null;
    }
    Class<?> enumClass = field.getAnnotation(ConfigurableField.class).enumClass();
    if (!enumClass.isEnum()) {
      return Collections.EMPTY_SET;
    }
    Set<Object> set = new LinkedHashSet<>();
    for (Object obj : enumClass.getEnumConstants()) {
      set.add(obj);
    }
    return set;
  }

}
