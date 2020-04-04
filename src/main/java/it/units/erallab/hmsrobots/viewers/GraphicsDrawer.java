/*
 * Copyright (C) 2019 Eric Medvet <eric.medvet@gmail.com>
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

import it.units.erallab.hmsrobots.objects.VoxelCompound;
import it.units.erallab.hmsrobots.objects.immutable.*;
import it.units.erallab.hmsrobots.util.Configurable;
import it.units.erallab.hmsrobots.util.Configuration;
import it.units.erallab.hmsrobots.viewers.drawers.*;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class GraphicsDrawer implements Configuration<GraphicsDrawer> {

  public static enum GeneralRenderingMode {
    GRID_MAJOR, GRID_MINOR, VIEWPORT_INFO, TIME_INFO, VOXEL_COMPOUND_CENTERS_INFO
  }

  @Configurable(type = Configurable.Type.BASIC)
  private Set<GeneralRenderingMode> generalRenderingModes = new HashSet<>(Set.of(
      GeneralRenderingMode.GRID_MAJOR,
      GeneralRenderingMode.VOXEL_COMPOUND_CENTERS_INFO,
      GeneralRenderingMode.TIME_INFO
  ));
  @Configurable
  private Color gridColor = Color.GRAY;
  @Configurable
  private Color infoColor = Color.BLUE;
  @Configurable
  private Color backgroundColor = Color.WHITE;
  @Configurable
  private Color basicColor = Color.BLUE;
  private double[] gridSizes = new double[]{2, 5, 10};
  @Configurable
  private float strokeWidth = 1f;
  @Configurable
  private List<Drawer> drawers = new ArrayList<>(List.of(
      VoxelDrawer.build(),
      BodyDrawer.build(),
      GroundDrawer.build(),
      JointDrawer.build()
  ));

  private GraphicsDrawer() {
  }

  public static GraphicsDrawer build() {
    return new GraphicsDrawer();
  }

  public void draw(Snapshot snapshot, Graphics2D g, BoundingBox graphicsFrame, BoundingBox worldFrame, String... infos) {
    //set clipping area
    g.setClip(
        (int) graphicsFrame.min.x, (int) graphicsFrame.min.y,
        (int) (graphicsFrame.max.x - graphicsFrame.min.x), (int) (graphicsFrame.max.y - graphicsFrame.min.y)
    );
    //save original transform
    AffineTransform oAt = g.getTransform();
    //prepare transformation
    double xRatio = (graphicsFrame.max.x - graphicsFrame.min.x) / (worldFrame.max.x - worldFrame.min.x);
    double yRatio = (graphicsFrame.max.y - graphicsFrame.min.y) / (worldFrame.max.y - worldFrame.min.y);
    double ratio = Math.min(xRatio, yRatio);
    AffineTransform at = new AffineTransform();
    at.translate(graphicsFrame.min.x, graphicsFrame.min.y);
    at.scale(ratio, -ratio);
    at.translate(-worldFrame.min.x, -worldFrame.max.y);
    //draw background
    g.setColor(backgroundColor);
    g.fillRect(
        (int) graphicsFrame.min.x, (int) graphicsFrame.min.y,
        (int) (graphicsFrame.max.x - graphicsFrame.min.x), (int) (graphicsFrame.max.y - graphicsFrame.min.y)
    );
    //draw grid
    g.setTransform(at);
    if (generalRenderingModes.contains(GeneralRenderingMode.GRID_MAJOR) || generalRenderingModes.contains(GeneralRenderingMode.GRID_MINOR)) {
      g.setColor(gridColor);
      g.setStroke(new BasicStroke(1f / (float) ratio));
      double gridSize = computeGridSize(worldFrame.min.x, worldFrame.max.x);
      if (generalRenderingModes.contains(GeneralRenderingMode.GRID_MAJOR)) {
        for (double gridX = Math.floor(worldFrame.min.x / gridSize) * gridSize; gridX < worldFrame.max.x; gridX = gridX + gridSize) {
          g.draw(new Line2D.Double(gridX, worldFrame.min.y, gridX, worldFrame.max.y));
        }
        for (double gridY = Math.floor(worldFrame.min.y / gridSize) * gridSize; gridY < worldFrame.max.y; gridY = gridY + gridSize) {
          g.draw(new Line2D.Double(worldFrame.min.x, gridY, worldFrame.max.x, gridY));
        }
      }
      if (generalRenderingModes.contains(GeneralRenderingMode.GRID_MINOR)) {
        gridSize = gridSize / 5d;
        g.setStroke(new BasicStroke(
            1f / (float) ratio,
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_ROUND,
            1.0f,
            new float[]{2f / (float) ratio, 0f, 2f / (float) ratio},
            0f));
        for (double gridX = Math.floor(worldFrame.min.x / gridSize) * gridSize; gridX < worldFrame.max.x; gridX = gridX + gridSize) {
          g.draw(new Line2D.Double(gridX, worldFrame.min.y, gridX, worldFrame.max.y));
        }
        for (double gridY = Math.floor(worldFrame.min.y / gridSize) * gridSize; gridY < worldFrame.max.y; gridY = gridY + gridSize) {
          g.draw(new Line2D.Double(worldFrame.min.x, gridY, worldFrame.max.x, gridY));
        }
      }
    }
    //draw components
    List<Point2> compoundCenters = new ArrayList<>();
    Stroke basicStroke = new BasicStroke(strokeWidth / (float) ratio);
    for (ImmutableObject object : snapshot.getObjects()) {
      recursivelyDraw(object, g, basicStroke);
      if (generalRenderingModes.contains(GeneralRenderingMode.VOXEL_COMPOUND_CENTERS_INFO)) {
        if (object.getObjectClass().equals(VoxelCompound.class)) {
          Point2[] centers = new Point2[object.getChildren().size()];
          centers = object.getChildren().stream().map(o -> o.getShape().center()).collect(Collectors.toList()).toArray(centers);
          compoundCenters.add(Point2.average(centers));
        }
      }
    }
    //restore transform
    g.setTransform(oAt);
    //info
    StringBuilder sb = new StringBuilder();
    if (generalRenderingModes.contains(GeneralRenderingMode.VIEWPORT_INFO)) {
      sb.append((sb.length() > 0) ? " " : "").append(String.format("vp=(%.0f;%.0f)->(%.0f;%.0f)", worldFrame.min.x, worldFrame.min.y, worldFrame.max.x, worldFrame.max.y));
    }
    if (generalRenderingModes.contains(GeneralRenderingMode.TIME_INFO)) {
      sb.append((sb.length() > 0) ? " " : "").append(String.format("t=%.2f", snapshot.getTime()));
    }
    if (generalRenderingModes.contains(GeneralRenderingMode.VOXEL_COMPOUND_CENTERS_INFO)) {
      if (!compoundCenters.isEmpty()) {
        sb.append((sb.length() > 0) ? String.format("%n") : "").append("c:");
        for (Point2 center : compoundCenters) {
          sb.append(String.format(" (%.0f,%.0f)", center.x, center.y));
        }
      }
    }
    for (String info : infos) {
      sb.append((sb.length() > 0) ? String.format("%n") : "").append(info);
    }
    if (sb.length() > 0) {
      g.setColor(infoColor);
      int relY = 1;
      for (String line : sb.toString().split(String.format("%n"))) {
        g.drawString(line, (int) graphicsFrame.min.x + 1, (int) graphicsFrame.min.y + relY + g.getFontMetrics().getMaxAscent());
        relY = relY + g.getFontMetrics().getMaxAscent() + 1;
      }
    }
  }

  private double computeGridSize(double x1, double x2) {
    double gridSize = (x2 - x1) / 10d;
    double exp = Math.floor(Math.log10(gridSize));
    double guess = gridSizes[0];
    double err = Math.abs(gridSize / Math.pow(10d, exp) - guess);
    for (int i = 1; i < gridSizes.length; i++) {
      if (Math.abs(gridSize / Math.pow(10d, exp) - gridSizes[i]) < err) {
        err = Math.abs(gridSize / Math.pow(10d, exp) - gridSizes[i]);
        guess = gridSizes[i];
      }
    }
    gridSize = guess * Math.pow(10d, exp);
    return gridSize;
  }

  private void recursivelyDraw(final ImmutableObject object, final Graphics2D g, Stroke basicStroke) {
    boolean drawChildren = true;
    for (Drawer drawer : drawers) {
      if (drawer.canDraw(object.getObjectClass())) {
        g.setStroke(basicStroke);
        g.setColor(basicColor);
        drawChildren = drawer.draw(object, g);
        break;
      }
    }
    if (drawChildren) {
      object.getChildren().stream().forEach(c -> recursivelyDraw(c, g, basicStroke));
    }
  }

  public static Color linear(final Color c1, final Color c2, final Color c3, float x1, float x2, float x3, float x) {
    if (x < x2) {
      return linear(c1, c2, x1, x2, x);
    }
    return linear(c2, c3, x2, x3, x);
  }

  public static Color linear(final Color c1, final Color c2, float min, float max, float x) {
    x = (x - min) / (max - min);
    x = Float.max(0f, Float.min(1f, x));
    final float r1 = c1.getRed() / 255f;
    final float g1 = c1.getGreen() / 255f;
    final float b1 = c1.getBlue() / 255f;
    final float a1 = c1.getAlpha() / 255f;
    final float r2 = c2.getRed() / 255f;
    final float g2 = c2.getGreen() / 255f;
    final float b2 = c2.getBlue() / 255f;
    final float a2 = c2.getAlpha() / 255f;
    final float r = r1 + (r2 - r1) * x;
    final float g = g1 + (g2 - g1) * x;
    final float b = b1 + (b2 - b1) * x;
    final float a = a1 + (a2 - a1) * x;
    return new Color(r, g, b, a);
  }

  public static Path2D toPath(Poly poly, boolean close) {
    Path2D path = toPath(poly.getVertexes());
    if (close) {
      path.closePath();
    }
    return path;
  }

  public static Path2D toPath(Point2... points) {
    Path2D path = new Path2D.Double();
    path.moveTo(points[0].x, points[0].y);
    for (int i = 1; i < points.length; i++) {
      path.lineTo(points[i].x, points[i].y);
    }
    return path;
  }

  public static Color alphaed(Color color, float alpha) {
    return new Color(
        (float) color.getRed() / 255f,
        (float) color.getGreen() / 255f,
        (float) color.getBlue() / 255f,
        alpha);
  }

}
