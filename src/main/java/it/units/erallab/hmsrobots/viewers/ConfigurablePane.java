/*
 * Copyright (C) 2020 Eric Medvet <eric.medvet@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.viewers;

import it.units.erallab.hmsrobots.util.Configurable;
import it.units.erallab.hmsrobots.util.Configuration;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public class ConfigurablePane extends JPanel {

  public ConfigurablePane(Configuration<?> configuration) {
    //set general properties
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    //add things
    add(new JLabel(configuration.getClass().getSimpleName()));
    for (String key : configuration.keys(Configurable.Type.BASIC)) {
      add(new JLabel(key + " -> " + configuration.type(key)));
      if (configuration.type(key).equals(Color.class)) {
        Color color = (Color) configuration.value(key);
        add(new JColorChooser(color));
      }
    }
  }
}
