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
import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import it.units.erallab.hmsrobots.viewers.Framer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * @author "Eric Medvet" on 2021/08/13 for 2dhmsr
 */
public interface Drawer {
  void draw(double t, List<Snapshot> lineage, Graphics2D g);

  static Drawer of(List<Drawer> drawers) {
    return (t, lineage, g) -> drawers.forEach(d -> d.draw(t, lineage, g));
  }

  static Drawer clip(BoundingBox boundingBox, Drawer drawer) {
    return (t, lineage, g) -> {
      AffineTransform originalTransform = g.getTransform();
      Shape shape = g.getClip();
      double clipX = shape.getBounds2D().getX();
      double clipY = shape.getBounds2D().getY();
      double clipW = shape.getBounds2D().getWidth();
      double clipH = shape.getBounds2D().getHeight();
      g.clip(new Rectangle2D.Double(
          clipX + boundingBox.min.x * clipW,
          clipY + boundingBox.min.y * clipH,
          clipW * boundingBox.width(),
          clipH * boundingBox.height()
      ));
      AffineTransform transform = new AffineTransform();
      transform.translate(g.getClip().getBounds2D().getX(), g.getClip().getBounds2D().getY());
      g.setTransform(transform);
      //draw

      System.out.println(g.getTransform());

      drawer.draw(t, lineage, g);
      //restore clip and transform
      g.setClip(shape);
      g.setTransform(originalTransform);
    };
  }

  static Drawer transform(Framer framer, Drawer drawer) {
    return (t, lineage, g) -> {
      BoundingBox graphicsFrame = BoundingBox.build(
          Point2.build(g.getClip().getBounds2D().getX(), g.getClip().getBounds2D().getY()),
          Point2.build(g.getClip().getBounds2D().getMaxX(), g.getClip().getBounds2D().getMaxY())
      );
      BoundingBox worldFrame = framer.getFrame(lineage, graphicsFrame.width() / graphicsFrame.height());
      //save original transform
      AffineTransform oAt = g.getTransform();
      //prepare transformation
      double xRatio = graphicsFrame.width() / worldFrame.width();
      double yRatio = graphicsFrame.height() / worldFrame.height();
      double ratio = Math.min(xRatio, yRatio);
      AffineTransform at = new AffineTransform();
      at.translate(graphicsFrame.min.x, graphicsFrame.min.y);
      at.scale(ratio, -ratio);
      at.translate(-worldFrame.min.x, -worldFrame.max.y);
      //draw
      drawer.draw(t, lineage, g);
      //restore transform
      g.setTransform(oAt);
    };
  }

  static boolean match(Snapshot snapshot, Class<?> contentClass, Class<? extends Snapshottable> creatorClass) {
    return contentClass.isAssignableFrom(snapshot.getContent().getClass()) && creatorClass.isAssignableFrom(snapshot.getSnapshottableClass());
  }

  static Snapshot lastMatching(List<Snapshot> lineage, Class<?> contentClass, Class<? extends Snapshottable> creatorClass) {
    for (int i = lineage.size() - 1; i >= 0; i--) {
      if (match(lineage.get(i), contentClass, creatorClass)) {
        return lineage.get(i);
      }
    }
    return null;
  }
}
