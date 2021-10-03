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
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.List;


/**
 * @author "Eric Medvet" on 2021/10/03 for 2dhmsr
 */
public class TargetDrawer extends SubtreeDrawer {

  private final static Color COLOR = Color.RED;

  private final Color color;

  public TargetDrawer(Extractor extractor, Color color) {
    super(extractor);
    this.color = color;
  }

  public TargetDrawer(Extractor extractor) {
    this(extractor, COLOR);
  }

  @Override

  protected void innerDraw(double t, Snapshot snapshot, Graphics2D g) {
    @SuppressWarnings("unchecked")
    List<Double> targets = (List<Double>) snapshot.getContent();
    Rectangle2D r = (Rectangle2D) g.getClip();
    g.setColor(color);
    for (double x : targets) {
      g.draw(new Line2D.Double(x, r.getMinY(), x, r.getMaxY()));
    }
  }
}
