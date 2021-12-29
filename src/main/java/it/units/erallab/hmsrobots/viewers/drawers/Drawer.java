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
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.viewers.DrawingUtils;
import it.units.erallab.hmsrobots.viewers.Framer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * @author "Eric Medvet" on 2021/08/13 for 2dhmsr
 */
public interface Drawer {

  void draw(double t, Snapshot snapshot, Graphics2D g);

  static Drawer clear() {
    return clear(Color.WHITE);
  }

  static Drawer clear(Color color) {
    return (t, snapshot, g) -> {
      g.setColor(color);
      g.fill(g.getClip());
    };
  }

  static Drawer clip(BoundingBox boundingBox, Drawer drawer) {
    return (t, snapshot, g) -> {
      Shape shape = g.getClip();
      double clipX = shape.getBounds2D().getX();
      double clipY = shape.getBounds2D().getY();
      double clipW = shape.getBounds2D().getWidth();
      double clipH = shape.getBounds2D().getHeight();
      g.clip(new Rectangle2D.Double(
          clipX + boundingBox.min().x() * clipW,
          clipY + boundingBox.min().y() * clipH,
          clipW * boundingBox.width(),
          clipH * boundingBox.height()
      ));
      //draw
      drawer.draw(t, snapshot, g);
      //restore clip and transform
      g.setClip(shape);
    };
  }

  static Drawer diagonals() {
    return diagonals(DrawingUtils.Colors.AXES);
  }

  static Drawer diagonals(Color color) {
    return (t, snapshot, g) -> {
      Rectangle2D r = (Rectangle2D) g.getClip();
      g.setColor(color);
      g.draw(new Line2D.Double(r.getX(), r.getY(), r.getMaxX(), r.getMaxY()));
      g.draw(new Line2D.Double(r.getX(), r.getMaxY(), r.getMaxX(), r.getY()));
    };
  }

  static Drawer of(Drawer... drawers) {
    return of(List.of(drawers));
  }

  static Drawer of(List<Drawer> drawers) {
    return (t, snapshot, g) -> drawers.forEach(d -> d.draw(t, snapshot, g));
  }

  static Drawer text(String s) {
    return text(s, DrawingUtils.Alignment.CENTER);
  }

  static Drawer text(String s, DrawingUtils.Alignment alignment) {
    return text(s, alignment, DrawingUtils.Colors.TEXT);
  }

  static Drawer text(String s, DrawingUtils.Alignment alignment, Color color) {
    return (t, snapshot, g) -> {
      g.setColor(color);
      g.drawString(
          s,
          switch (alignment) {
            case LEFT -> g.getClipBounds().x + 1;
            case CENTER -> g.getClipBounds().x + g.getClipBounds().width / 2 - g.getFontMetrics().stringWidth(s) / 2;
            case RIGHT -> g.getClipBounds().x + g.getClipBounds().width - 1 - g.getFontMetrics().stringWidth(s);
          },
          g.getClipBounds().y + 1 + g.getFontMetrics().getMaxAscent()
      );
    };
  }

  static Drawer transform(Framer framer, Drawer drawer) {
    return (t, snapshot, g) -> {
      BoundingBox graphicsFrame = BoundingBox.of(
          g.getClip().getBounds2D().getX(),
          g.getClip().getBounds2D().getY(),
          g.getClip().getBounds2D().getMaxX(),
          g.getClip().getBounds2D().getMaxY()
      );
      BoundingBox worldFrame = framer.getFrame(t, snapshot, graphicsFrame.width() / graphicsFrame.height());
      //save original transform and stroke
      AffineTransform oAt = g.getTransform();
      Stroke oStroke = g.getStroke();
      //prepare transformation
      double xRatio = graphicsFrame.width() / worldFrame.width();
      double yRatio = graphicsFrame.height() / worldFrame.height();
      double ratio = Math.min(xRatio, yRatio);
      AffineTransform at = new AffineTransform();
      at.translate(graphicsFrame.min().x(), graphicsFrame.min().y());
      at.scale(ratio, -ratio);
      at.translate(-worldFrame.min().x(), -worldFrame.max().y());
      //apply transform and stroke
      g.setTransform(at);
      g.setStroke(DrawingUtils.getScaleIndependentStroke(1, (float) ratio));
      //draw
      drawer.draw(t, snapshot, g);
      //restore transform
      g.setTransform(oAt);
      g.setStroke(oStroke);
    };
  }

}
