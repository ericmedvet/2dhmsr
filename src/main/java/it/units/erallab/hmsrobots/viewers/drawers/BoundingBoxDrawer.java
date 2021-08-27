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

import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.geometry.Shape;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.List;

public class BoundingBoxDrawer implements Drawer {

  private final static Color COLOR = Color.PINK;

  private final Color fillColor;
  private final Color strokeColor;
  private final Class<? extends Snapshottable> creatorClass;

  public BoundingBoxDrawer(Color fillColor, Color strokeColor, Class<? extends Snapshottable> creatorClass) {
    this.fillColor = fillColor;
    this.strokeColor = strokeColor;
    this.creatorClass = creatorClass;
  }

  public BoundingBoxDrawer(Class<? extends Snapshottable> creatorClass) {
    this(COLOR, GraphicsDrawer.alphaed(COLOR, 0.5f), creatorClass);
  }

  @Override
  public void draw(double t, List<Snapshot> lineage, Graphics2D g) {
    Snapshot last = lineage.get(lineage.size() - 1);
    if (!Drawer.match(last, Shape.class, creatorClass)) {
      return;
    }
    BoundingBox box = ((Shape) last.getContent()).boundingBox();
    Rectangle2D rect = new Rectangle2D.Double(
        box.min.x,
        box.min.y,
        box.max.x - box.min.x,
        box.max.y - box.min.y
    );
    g.setColor(fillColor);
    g.fill(rect);
    g.setColor(strokeColor);
    g.draw(rect);
  }

}
