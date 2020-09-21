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
package it.units.erallab.hmsrobots.viewers.drawers;

import it.units.erallab.hmsrobots.core.objects.immutable.Immutable;
import it.units.erallab.hmsrobots.util.Configurable;
import it.units.erallab.hmsrobots.util.ConfigurableField;
import it.units.erallab.hmsrobots.util.Poly;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

public class Ground extends Drawer<it.units.erallab.hmsrobots.core.objects.immutable.Ground> implements Configurable<Ground> {

  @ConfigurableField
  private Color strokeColor = Color.BLACK;
  @ConfigurableField
  private Color fillColor = GraphicsDrawer.alphaed(Color.BLACK, 0.25f);
  @ConfigurableField
  private boolean useTexture = true;
  private final TexturePaint texturePaint;
  private final static int TEXTURE_SIZE = 4;
  private final static Color TEXTURE_COLOR = Color.GRAY;

  private Ground() {
    super(it.units.erallab.hmsrobots.core.objects.immutable.Ground.class);
    texturePaint = createTexturePaint();
  }

  public static Ground build() {
    return new Ground();
  }

  @Override
  public boolean draw(it.units.erallab.hmsrobots.core.objects.immutable.Ground immutable, Immutable parent, Graphics2D g) {
    Path2D path = GraphicsDrawer.toPath((Poly) immutable.getShape(), true);
    if (useTexture || fillColor != null) {
      if (useTexture) {
        g.setPaint(texturePaint);

      } else {
        g.setColor(fillColor);
      }
      g.fill(path);
    }
    if (strokeColor != null) {
      g.setColor(strokeColor);
      g.draw(path);
    }
    return false;
  }

  private TexturePaint createTexturePaint() {
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
