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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author "Eric Medvet" on 2021/08/13 for 2dhmsr
 */
public interface Drawer {

  void draw(double t, Snapshot snapshot, Graphics2D g);

  static Drawer of(Drawer... drawers) {
    return of(List.of(drawers));
  }

  static Drawer of(List<Drawer> drawers) {
    return (t, snapshot, g) -> drawers.forEach(d -> d.draw(t, snapshot, g));
  }


  static void main(String[] args) throws IOException {
    int w = 1000;
    int h = 500;
    BufferedImage bi = new BufferedImage(1000, 500, BufferedImage.TYPE_3BYTE_BGR);
    Graphics2D g = bi.createGraphics();
    g.setClip(new Rectangle2D.Double(0, 0, w, h));
    System.out.println("device-bounds: " + g.getDeviceConfiguration().getBounds());
    System.out.println("g clip: " + g.getClip());
    System.out.println("g clip-bound: " + g.getClipBounds());

    g.setColor(Color.RED);
    g.drawRect(0, 0, w, h);
    g.drawLine(10, 10, w - 10, h - 10);
    g.drawLine(10, h - 10, w - 10, 10);

    Drawer d = Drawer.of(
        new InfoDrawer(),
        diagonals(),
        Drawer.clip(BoundingBox.build(0, 0.5, 0.5, 1), diagonals()),
        Drawer.clip(BoundingBox.build(0.5, 0.5, 1, 1), diagonals())
    );
    d.draw(1.23, null, g);
    g.dispose();

    ImageIO.write(bi, "png", new File("/home/eric/img.png"));
  }

  static Drawer diagonals() {
    return (t, snapshot, g) -> {
      Rectangle2D r = (Rectangle2D) g.getClip();
      g.setColor(Color.GREEN);
      g.draw(new Line2D.Double(r.getX(), r.getY(), r.getMaxX(), r.getMaxY()));
      g.draw(new Line2D.Double(r.getX(), r.getMaxY(), r.getMaxX(), r.getY()));
    };
  }

  static Drawer clip(BoundingBox boundingBox, Drawer drawer) {
    return (t, snapshot, g) -> {
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
      drawer.draw(t, snapshot, g);
      //restore clip and transform
      g.setTransform(originalTransform);
      g.setClip(shape);
    };
  }

  static Drawer transform(Framer framer, Drawer drawer) {
    return (t, snapshot, g) -> {
      BoundingBox graphicsFrame = BoundingBox.build(
          g.getClip().getBounds2D().getX(),
          g.getClip().getBounds2D().getY(),
          g.getClip().getBounds2D().getMaxX(),
          g.getClip().getBounds2D().getMaxY()
      );
      BoundingBox worldFrame = framer.getFrame(snapshot, graphicsFrame.width() / graphicsFrame.height());
      //save original transform and stroke
      AffineTransform oAt = g.getTransform();
      Stroke oStroke = g.getStroke();
      //prepare transformation
      double xRatio = graphicsFrame.width() / worldFrame.width();
      double yRatio = graphicsFrame.height() / worldFrame.height();
      double ratio = Math.min(xRatio, yRatio);
      AffineTransform at = new AffineTransform();
      at.translate(graphicsFrame.min.x, graphicsFrame.min.y);
      at.scale(ratio, -ratio);
      at.translate(-worldFrame.min.x, -worldFrame.max.y);
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
