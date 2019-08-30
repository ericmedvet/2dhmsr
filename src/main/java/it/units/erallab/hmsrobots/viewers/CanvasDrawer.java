/*
 * Copyright (C) 2019 eric
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.viewers;

import it.units.erallab.hmsrobots.WorldEvent;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.objects.immutable.Component;
import it.units.erallab.hmsrobots.objects.immutable.Compound;
import it.units.erallab.hmsrobots.objects.immutable.Point2;
import it.units.erallab.hmsrobots.objects.immutable.Poly;
import it.units.erallab.hmsrobots.objects.immutable.VoxelComponent;
import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.util.Set;

/**
 *
 * @author eric
 */
public class CanvasDrawer {

  private final static double FORCE_CIRCLE_RANGE = 0.5d;
  private final static float VOXEL_FILL_ALPHA = 0.75f;
  private final static float VOXEL_COMPONENT_ALPHA = 0.5f;

  public static enum VoxelVizMode {
    POLY, FILL_AREA, SPRINGS, COMPONENTS, FORCE
  }

  public static void draw(WorldEvent event, Canvas canvas, double x1, double y1, double x2, double y2, Set<VoxelVizMode> vizModes) {
    Graphics2D g = (Graphics2D) canvas.getBufferStrategy().getDrawGraphics();
    //reset screen
    g.setColor(Color.GRAY);
    g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    AffineTransform oAt = g.getTransform();
    //transform
    double xRatio = (double) canvas.getWidth() / (x2 - x1);
    double yRatio = (double) canvas.getHeight() / (y2 - y1);
    double ratio = Math.min(xRatio, yRatio);
    AffineTransform at = new AffineTransform();
    at.scale(ratio, -ratio);
    at.translate(-x1, -y2);
    g.setTransform(at);
    g.setStroke(new BasicStroke(1f / (float) ratio));
    //draw
    g.setColor(Color.BLACK);
    g.fill(new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1));
    for (Compound object : event.getCompounds()) {
      draw(object, g, vizModes);
    }
    //inverse transform    
    g.setTransform(oAt);
    //info
    g.setColor(Color.BLACK);
    g.drawString(String.format("(%.0f;%.0f)->(%.0f;%.0f)", x1, y1, x2, y2), 1, 1 + g.getFontMetrics().getMaxAscent());
    //finalize
    g.dispose();
    BufferStrategy strategy = canvas.getBufferStrategy();
    if (!strategy.contentsLost()) {
      strategy.show();
    }
    Toolkit.getDefaultToolkit().sync();
  }

  private static void draw(Compound compound, Graphics2D g, Set<VoxelVizMode> vizModes) {
    if (compound.getObjectClass().equals(Voxel.class)) {
      for (Component component : compound.getComponents()) {
        if (component.getType().equals(Component.Type.ENCLOSING)) {
          final double lastArea = ((VoxelComponent) component).getLastArea();
          final double restArea = ((VoxelComponent) component).getRestArea();
          final Point2 c = component.getPoly().center();
          final double f = ((VoxelComponent) component).getLastAppliedForce();
          final double l = Math.sqrt(restArea);
          if (vizModes.contains(VoxelVizMode.FILL_AREA)) {
            final Color color = linear(
                    Color.RED, Color.GREEN, Color.YELLOW,
                    .75d, 1d, 1.25d,
                    lastArea / restArea,
                    VOXEL_FILL_ALPHA
            );
            g.setColor(color);
            g.fill(toPath(component.getPoly(), true));
          }
          if (vizModes.contains(VoxelVizMode.POLY)) {
            g.setColor(new Color(0f, 0f, 1f, VOXEL_COMPONENT_ALPHA));
            g.draw(toPath(component.getPoly(), true));
          }
          if (vizModes.contains(VoxelVizMode.FORCE)) {
            g.setColor(Color.BLUE);
            double r = (l * (1 - FORCE_CIRCLE_RANGE * f)) / 2d;
            Ellipse2D circle = new Ellipse2D.Double(c.x - r, c.y - r, r * 2d, r * 2d);
            g.draw(circle);
          }
        } else if (component.getType().equals(Component.Type.CONNECTION) && vizModes.contains(VoxelVizMode.SPRINGS)) {
          g.setColor(Color.BLUE);
          g.draw(toPath(component.getPoly(), false));
        } else if (component.getType().equals(Component.Type.RIGID) && vizModes.contains(VoxelVizMode.COMPONENTS)) {
          g.setColor(new Color(0f, 0f, 1f, VOXEL_COMPONENT_ALPHA));
          g.fill(toPath(component.getPoly(), true));
        }
      }
    } else {
      for (Component component : compound.getComponents()) {
        drawComponent(component, g);
      }
    }
  }

  private static void drawComponent(Component component, Graphics2D g) {
    switch (component.getType()) {
      case CONNECTION:
        g.setColor(Color.GREEN);
        g.draw(toPath(component.getPoly(), false));
        break;
      case RIGID:
        g.setColor(Color.YELLOW);
        g.draw(toPath(component.getPoly(), true));
        break;
      default:
        g.setColor(Color.RED);
        g.draw(toPath(component.getPoly(), true));
        break;
    }
  }

  private static Color linear(final Color c1, final Color c2, final Color c3, double x1, double x2, double x3, double x, float alpha) {
    if (x < x2) {
      return mid(c1, c2, x1, x2, x, alpha);
    }
    return mid(c2, c3, x2, x3, x, alpha);
  }

  private static Color mid(final Color c1, final Color c2, double min, double max, double x, float alpha) {
    x = (x - min) / (max - min);
    x = Math.max(0d, Math.min(1d, x));
    final double r1 = c1.getRed() / 255d;
    final double g1 = c1.getGreen() / 255d;
    final double b1 = c1.getBlue() / 255d;
    final double r2 = c2.getRed() / 255d;
    final double g2 = c2.getGreen() / 255d;
    final double b2 = c2.getBlue() / 255d;
    final double r = r1 + (r2 - r1) * x;
    final double g = g1 + (g2 - g1) * x;
    final double b = b1 + (b2 - b1) * x;
    return new Color((float) r, (float) g, (float) b, alpha);
  }

  private static Path2D toPath(Poly poly, boolean close) {
    Path2D path = new Path2D.Double();
    path.moveTo(poly.getVertexes()[0].x, poly.getVertexes()[0].y);
    for (int i = 1; i < poly.getVertexes().length; i++) {
      path.lineTo(poly.getVertexes()[i].x, poly.getVertexes()[i].y);
    }
    if (close) {
      path.closePath();
    }
    return path;
  }

}
