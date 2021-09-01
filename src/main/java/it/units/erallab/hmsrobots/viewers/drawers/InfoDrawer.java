/*
 * Copyright (C) 2021 Eric Medvet <eric.medvet@gmail.com> (as Eric Medvet <eric.medvet@gmail.com>)
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

/**
 * @author "Eric Medvet" on 2021/08/27 for 2dhmsr
 */
public class InfoDrawer implements Drawer {

  private final String string;
  private final static Color INFO_COLOR = Color.BLUE;

  public InfoDrawer(String string) {
    this.string = string;
  }

  public InfoDrawer() {
    this("");
  }

  @Override
  public void draw(double t, Snapshot snapshot, Graphics2D g) {
    //prepare string
    StringBuilder sb = new StringBuilder();
    if (!string.isEmpty()) {
      sb.append(string);
      sb.append("\n");
    }
    sb.append(String.format("t=%05.2f", t));
    //write
    g.setColor(INFO_COLOR);
    int relY = g.getClipBounds().y + 1;
    for (String line : sb.toString().split(String.format("%n"))) {
      g.drawString(line, g.getClipBounds().x + 1, relY + g.getFontMetrics().getMaxAscent());
      relY = relY + g.getFontMetrics().getMaxAscent() + 1;
    }
  }
}
