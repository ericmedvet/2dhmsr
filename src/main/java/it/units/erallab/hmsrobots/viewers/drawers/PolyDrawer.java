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

import it.units.erallab.hmsrobots.core.geometry.Poly;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.List;

public class PolyDrawer implements Drawer {

  private final static Color COLOR = Color.BLACK;
  private final static int TEXTURE_SIZE = 4;
  private final static Color TEXTURE_COLOR = Color.GRAY;
  public final static TexturePaint TEXTURE_PAINT = createTexturePaint();

  private final Color strokeColor;
  private final Color fillColor;
  private final boolean useTexture;

  private final Class<? extends Snapshottable> creatorClass;

  public PolyDrawer(Class<? extends Snapshottable> creatorClass) {
    this(COLOR, creatorClass);
  }


  public PolyDrawer(Color color, Class<? extends Snapshottable> creatorClass) {
    this.creatorClass = creatorClass;
    strokeColor = color;
    fillColor = GraphicsDrawer.alphaed(color, 0.25f);
    useTexture = false;
  }

  public PolyDrawer(TexturePaint texturePaint, Class<? extends Snapshottable> creatorClass) {
    this.creatorClass = creatorClass;
    strokeColor = COLOR;
    fillColor = GraphicsDrawer.alphaed(COLOR, 0.25f);
    texturePaint = TEXTURE_PAINT;
    useTexture = true;
  }

  @Override
  public void draw(double t, List<Snapshot> lineage, Graphics2D g) {
    Snapshot last = lineage.get(lineage.size() - 1);
    if (!Drawer.match(last, Poly.class, creatorClass)) {
      return;
    }
    Poly poly = (Poly) last.getContent();
    Path2D path = GraphicsDrawer.toPath(poly, true);
    if (useTexture || fillColor != null) {
      if (useTexture) {
        g.setPaint(TEXTURE_PAINT);
      } else {
        g.setColor(fillColor);
      }
      g.fill(path);
    }
    if (strokeColor != null) {
      g.setColor(strokeColor);
      g.draw(path);
    }
  }

  private static TexturePaint createTexturePaint() {
    BufferedImage texture = new BufferedImage(2, 2, BufferedImage.TYPE_4BYTE_ABGR);
    Graphics2D g = texture.createGraphics();
    g.setColor(GraphicsDrawer.alphaed(TEXTURE_COLOR, 0.5f));
    g.fillRect(0, 0, 2, 2);
    g.setColor(GraphicsDrawer.alphaed(TEXTURE_COLOR, 0.75f));
    g.fillRect(1, 0, 1, 1);
    g.fillRect(0, 1, 1, 1);
    g.dispose();
    return new TexturePaint(texture, new Rectangle(0, 0, TEXTURE_SIZE, TEXTURE_SIZE));
  }
}
