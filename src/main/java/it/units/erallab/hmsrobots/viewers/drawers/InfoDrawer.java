/*
 * Copyright (c) "Eric Medvet" 2021.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.units.erallab.hmsrobots.viewers.drawers;

import it.units.erallab.hmsrobots.core.snapshots.Snapshot;

import java.awt.*;
import java.util.List;

/**
 * @author "Eric Medvet" on 2021/08/27 for 2dhmsr
 */
public class InfoDrawer implements Drawer {

  private final static Color INFO_COLOR = Color.BLUE;

  @Override
  public void draw(double t, List<Snapshot> lineage, Graphics2D g) {
    //prepare string
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("t=%05.2f", t));
    //write
    g.setColor(INFO_COLOR);
    int relY = 1;
    for (String line : sb.toString().split(String.format("%n"))) {
      g.drawString(line, 1, relY + g.getFontMetrics().getMaxAscent());
      relY = relY + g.getFontMetrics().getMaxAscent() + 1;
    }
  }
}
