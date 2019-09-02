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

import it.units.erallab.hmsrobots.Snapshot;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.objects.immutable.Component;
import it.units.erallab.hmsrobots.objects.immutable.Compound;
import it.units.erallab.hmsrobots.objects.immutable.Point2;
import it.units.erallab.hmsrobots.objects.immutable.Poly;
import it.units.erallab.hmsrobots.objects.immutable.VoxelComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author eric
 */
public class GraphicsDrawer {

  private final static double FORCE_CIRCLE_RANGE = 0.5d;
  private final static float VOXEL_FILL_ALPHA = 0.75f;
  private final static float VOXEL_COMPONENT_ALPHA = 0.5f;
  private final static boolean GRID_MAJOR = true;
  private final static boolean GRID_MINOR = false;
  private final static Color GRID_COLOR = Color.GRAY;
  private final static Color INFO_COLOR = Color.GRAY.darker();
  private final static Color BACKGROUND_COLOR = Color.WHITE;
  private final static double[] GRID_SIZES = new double[]{2, 5, 10};

  public static enum VoxelVizMode {
    POLY, FILL_AREA, SPRINGS, COMPONENTS, FORCE
  }

  public static void draw(Snapshot snapshot, Graphics2D g, int w, int h, double x1, double y1, double x2, double y2, Set<VoxelVizMode> vizModes) {
    //reset screen
    g.setColor(Color.GRAY);
    g.fillRect(0, 0, w, h);
    AffineTransform oAt = g.getTransform();
    //prepare transformation
    double xRatio = (double) w / (x2 - x1);
    double yRatio = (double) h / (y2 - y1);
    double ratio = Math.min(xRatio, yRatio);
    AffineTransform at = new AffineTransform();
    at.scale(ratio, -ratio);
    at.translate(-x1, -y2);
    //draw background
    g.setColor(BACKGROUND_COLOR);
    g.fill(new Rectangle2D.Double(0, 0, w, h));
    //draw grid
    g.setTransform(at);
    if (GRID_MAJOR || GRID_MINOR) {
      g.setColor(GRID_COLOR);
      g.setStroke(new BasicStroke(1f / (float) ratio));
      double gridSize = computeGridSize(x1, x2);
      if (GRID_MAJOR) {
        for (double gridX = Math.floor(x1 / gridSize) * gridSize; gridX < x2; gridX = gridX + gridSize) {
          g.draw(new Line2D.Double(gridX, y1, gridX, y2));
        }
        for (double gridY = Math.floor(y1 / gridSize) * gridSize; gridY < y2; gridY = gridY + gridSize) {
          g.draw(new Line2D.Double(x1, gridY, x2, gridY));
        }
      }
      if (GRID_MINOR) {
        gridSize = gridSize / 5d;
        g.setStroke(new BasicStroke(
                1f / (float) ratio,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND,
                1.0f,
                new float[]{2f / (float) ratio, 0f, 2f / (float) ratio},
                0f));
        for (double gridX = Math.floor(x1 / gridSize) * gridSize; gridX < x2; gridX = gridX + gridSize) {
          g.draw(new Line2D.Double(gridX, y1, gridX, y2));
        }
        for (double gridY = Math.floor(y1 / gridSize) * gridSize; gridY < y2; gridY = gridY + gridSize) {
          g.draw(new Line2D.Double(x1, gridY, x2, gridY));
        }
      }
    }
    //draw components
    g.setStroke(new BasicStroke(2f / (float) ratio));
    for (Compound object : snapshot.getCompounds()) {
      draw(object, g, vizModes);
    }
    //inverse transform    
    g.setTransform(oAt);
    //info
    g.setColor(INFO_COLOR);
    g.drawString(String.format("(%.0f;%.0f)->(%.0f;%.0f)", x1, y1, x2, y2), 1, 1 + g.getFontMetrics().getMaxAscent());
    //finalize
    g.dispose();
  }

  private static double computeGridSize(double x1, double x2) {
    double gridSize = (x2 - x1) / 10d;
    double exp = Math.floor(Math.log10(gridSize));
    double guess = GRID_SIZES[0];
    double err = Math.abs(gridSize / Math.pow(10d, exp) - guess);
    for (int i = 1; i < GRID_SIZES.length; i++) {
      if (Math.abs(gridSize / Math.pow(10d, exp) - GRID_SIZES[i]) < err) {
        err = Math.abs(gridSize / Math.pow(10d, exp) - GRID_SIZES[i]);
        guess = GRID_SIZES[i];
      }
    }
    gridSize = guess * Math.pow(10d, exp);
    return gridSize;
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
