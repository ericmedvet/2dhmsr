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

import com.google.common.collect.EvictingQueue;
import it.units.erallab.hmsrobots.Snapshot;
import it.units.erallab.hmsrobots.objects.Ground;
import it.units.erallab.hmsrobots.objects.VoxelCompound;
import it.units.erallab.hmsrobots.objects.immutable.Component;
import it.units.erallab.hmsrobots.objects.immutable.Compound;
import it.units.erallab.hmsrobots.objects.immutable.Point2;
import it.units.erallab.hmsrobots.objects.immutable.Poly;
import it.units.erallab.hmsrobots.objects.immutable.VoxelComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Set;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class GraphicsDrawer {

  public static enum RenderingMode {
    VOXEL_POLY, VOXEL_FILL_AREA, VOXEL_SPRINGS, VOXEL_COMPONENTS, VOXEL_FORCE,
    GRID_MAJOR, GRID_MINOR, VIEWPORT_INFO, TIME_INFO
  }

  public static class Builder {

    private final static double FORCE_CIRCLE_RANGE = 0.5d;
    private final static float VOXEL_FILL_ALPHA = 0.75f;
    private final static float VOXEL_COMPONENT_ALPHA = 0.5f;
    private final static Color GRID_COLOR = Color.GRAY;
    private final static Color INFO_COLOR = Color.BLUE;
    private final static Color BACKGROUND_COLOR = Color.WHITE;
    private final static Color GROUND_COLOR = Color.BLACK;
    private final static double[] GRID_SIZES = new double[]{2, 5, 10};

    private double forceCircleRange = FORCE_CIRCLE_RANGE;
    private float voxelFillAlpha = VOXEL_FILL_ALPHA;
    private float voxelComponentAlpha = VOXEL_COMPONENT_ALPHA;
    private Color gridColor = GRID_COLOR;
    private Color infoColor = INFO_COLOR;
    private Color backgroundColor = BACKGROUND_COLOR;
    private Color groundColor = GROUND_COLOR;
    private double[] gridSizes = GRID_SIZES;

    public static Builder create() {
      return new Builder();
    }

    public Builder forceCircleRange(double forceCircleRange) {
      this.forceCircleRange = forceCircleRange;
      return this;
    }

    public Builder voxelFillAlpha(float voxelFillAlpha) {
      this.voxelFillAlpha = voxelFillAlpha;
      return this;
    }

    public Builder voxelComponentAlpha(float voxelComponentAlpha) {
      this.voxelComponentAlpha = voxelComponentAlpha;
      return this;
    }

    public Builder gridColor(Color gridColor) {
      this.gridColor = gridColor;
      return this;
    }

    public Builder infoColor(Color infoColor) {
      this.infoColor = infoColor;
      return this;
    }

    public Builder backgroundColor(Color backgroundColor) {
      this.backgroundColor = backgroundColor;
      return this;
    }

    public Builder groundColor(Color groundColor) {
      this.groundColor = groundColor;
      return this;
    }

    public Builder gridSizes(double[] gridSizes) {
      this.gridSizes = gridSizes;
      return this;
    }

    public double getForceCircleRange() {
      return forceCircleRange;
    }

    public float getVoxelFillAlpha() {
      return voxelFillAlpha;
    }

    public float getVoxelComponentAlpha() {
      return voxelComponentAlpha;
    }

    public Color getGridColor() {
      return gridColor;
    }

    public Color getInfoColor() {
      return infoColor;
    }

    public Color getBackgroundColor() {
      return backgroundColor;
    }

    public Color getGroundColor() {
      return groundColor;
    }

    public double[] getGridSizes() {
      return gridSizes;
    }

    public GraphicsDrawer build() {
      return new GraphicsDrawer(this);
    }
  }

  private final Builder builder;

  private GraphicsDrawer() {
    builder = Builder.create();
  }

  private GraphicsDrawer(Builder builder) {
    this.builder = builder;
  }

  public void draw(Snapshot snapshot, Graphics2D g, Frame graphicsFrame, Frame worldFrame, Set<RenderingMode> renderingModes, String... otherInfos) {
    //set clipping area
    g.setClip(
            (int) graphicsFrame.x1, (int) graphicsFrame.y1,
            (int) (graphicsFrame.x2 - graphicsFrame.x1), (int) (graphicsFrame.y2 - graphicsFrame.y1)
    );
    //save original transform
    AffineTransform oAt = g.getTransform();
    //prepare transformation
    double xRatio = (graphicsFrame.x2 - graphicsFrame.x1) / (worldFrame.x2 - worldFrame.x1);
    double yRatio = (graphicsFrame.y2 - graphicsFrame.y1) / (worldFrame.y2 - worldFrame.y1);
    double ratio = Math.min(xRatio, yRatio);
    AffineTransform at = new AffineTransform();
    at.translate(graphicsFrame.x1, graphicsFrame.y1);
    at.scale(ratio, -ratio);
    at.translate(-worldFrame.x1, -worldFrame.y2);
    //draw background
    g.setColor(builder.getBackgroundColor());
    g.fillRect(
            (int) graphicsFrame.x1, (int) graphicsFrame.y1,
            (int) (graphicsFrame.x2 - graphicsFrame.x1), (int) (graphicsFrame.y2 - graphicsFrame.y1)
    );
    //draw grid
    g.setTransform(at);
    if (renderingModes.contains(RenderingMode.GRID_MAJOR) || renderingModes.contains(RenderingMode.GRID_MINOR)) {
      g.setColor(builder.getGridColor());
      g.setStroke(new BasicStroke(1f / (float) ratio));
      double gridSize = computeGridSize(worldFrame.x1, worldFrame.x2);
      if (renderingModes.contains(RenderingMode.GRID_MAJOR)) {
        for (double gridX = Math.floor(worldFrame.x1 / gridSize) * gridSize; gridX < worldFrame.x2; gridX = gridX + gridSize) {
          g.draw(new Line2D.Double(gridX, worldFrame.y1, gridX, worldFrame.y2));
        }
        for (double gridY = Math.floor(worldFrame.y1 / gridSize) * gridSize; gridY < worldFrame.y2; gridY = gridY + gridSize) {
          g.draw(new Line2D.Double(worldFrame.x1, gridY, worldFrame.x2, gridY));
        }
      }
      if (renderingModes.contains(RenderingMode.GRID_MINOR)) {
        gridSize = gridSize / 5d;
        g.setStroke(new BasicStroke(
                1f / (float) ratio,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND,
                1.0f,
                new float[]{2f / (float) ratio, 0f, 2f / (float) ratio},
                0f));
        for (double gridX = Math.floor(worldFrame.x1 / gridSize) * gridSize; gridX < worldFrame.x2; gridX = gridX + gridSize) {
          g.draw(new Line2D.Double(gridX, worldFrame.y1, gridX, worldFrame.y2));
        }
        for (double gridY = Math.floor(worldFrame.y1 / gridSize) * gridSize; gridY < worldFrame.y2; gridY = gridY + gridSize) {
          g.draw(new Line2D.Double(worldFrame.x1, gridY, worldFrame.x2, gridY));
        }
      }
    }
    //draw components
    g.setStroke(new BasicStroke(2f / (float) ratio));
    for (Compound object : snapshot.getCompounds()) {
      draw(object, g, renderingModes);
    }
    //restore transform    
    g.setTransform(oAt);
    //info
    StringBuilder sb = new StringBuilder();
    if (renderingModes.contains(RenderingMode.VIEWPORT_INFO)) {
      sb.append(((sb.length() > 0) ? " " : "") + String.format("vp=(%.0f;%.0f)->(%.0f;%.0f)", worldFrame.x1, worldFrame.y1, worldFrame.x2, worldFrame.y2));
    }
    if (renderingModes.contains(RenderingMode.TIME_INFO)) {
      sb.append(((sb.length() > 0) ? " " : "") + String.format("t=%.2f", snapshot.getTime()));
    }
    for (String otherInfo : otherInfos) {
      sb.append(((sb.length() > 0) ? " " : "") + otherInfo);
    }
    if (sb.length()>0) {
      g.setColor(builder.getInfoColor());
      g.drawString(sb.toString(), (int) graphicsFrame.x1 + 1, (int) graphicsFrame.y1 + 1 + g.getFontMetrics().getMaxAscent());
    }
  }

  private double computeGridSize(double x1, double x2) {
    double gridSize = (x2 - x1) / 10d;
    double exp = Math.floor(Math.log10(gridSize));
    double guess = builder.getGridSizes()[0];
    double err = Math.abs(gridSize / Math.pow(10d, exp) - guess);
    for (int i = 1; i < builder.getGridSizes().length; i++) {
      if (Math.abs(gridSize / Math.pow(10d, exp) - builder.getGridSizes()[i]) < err) {
        err = Math.abs(gridSize / Math.pow(10d, exp) - builder.getGridSizes()[i]);
        guess = builder.getGridSizes()[i];
      }
    }
    gridSize = guess * Math.pow(10d, exp);
    return gridSize;
  }

  private void draw(Compound compound, Graphics2D g, Set<RenderingMode> vizModes) {
    if (compound.getObjectClass().equals(VoxelCompound.class)) {
      for (Component component : compound.getComponents()) {
        if (component.getType().equals(Component.Type.ENCLOSING)) {
          final double lastArea = ((VoxelComponent) component).getLastArea();
          final double restArea = ((VoxelComponent) component).getRestArea();
          final Point2 c = component.getPoly().center();
          final double f = ((VoxelComponent) component).getLastAppliedForce();
          final double l = Math.sqrt(restArea);
          if (vizModes.contains(RenderingMode.VOXEL_FILL_AREA)) {
            final Color color = linear(
                    Color.RED, Color.GREEN, Color.YELLOW,
                    .75d, 1d, 1.25d,
                    lastArea / restArea,
                    builder.getVoxelFillAlpha()
            );
            g.setColor(color);
            g.fill(toPath(component.getPoly(), true));
          }
          if (vizModes.contains(RenderingMode.VOXEL_POLY)) {
            g.setColor(new Color(0f, 0f, 1f, builder.getVoxelComponentAlpha()));
            g.draw(toPath(component.getPoly(), true));
          }
          if (vizModes.contains(RenderingMode.VOXEL_FORCE)) {
            g.setColor(Color.BLUE);
            double r = (l * (1d - builder.getForceCircleRange() * f)) / 2d;
            Ellipse2D circle = new Ellipse2D.Double(c.x - r, c.y - r, r * 2d, r * 2d);
            g.draw(circle);
          }
        } else if (component.getType().equals(Component.Type.CONNECTION) && vizModes.contains(RenderingMode.VOXEL_SPRINGS)) {
          g.setColor(Color.BLUE);
          g.draw(toPath(component.getPoly(), false));
        } else if (component.getType().equals(Component.Type.RIGID) && vizModes.contains(RenderingMode.VOXEL_COMPONENTS)) {
          g.setColor(new Color(0f, 0f, 1f, builder.getVoxelFillAlpha()));
          g.fill(toPath(component.getPoly(), true));
        }
      }
    } else if (compound.getObjectClass().equals(Ground.class)) {
      for (Component component : compound.getComponents()) {
        Color fillColor = new Color(
                builder.getGroundColor().getRed(),
                builder.getGroundColor().getGreen(),
                builder.getGroundColor().getBlue(),
                builder.getGroundColor().getAlpha() / 2
        );
        final Path2D path = toPath(component.getPoly(), true);
        g.setColor(Color.BLACK);
        g.draw(path);
        g.setColor(fillColor);
        g.fill(path);
      }
    } else {
      for (Component component : compound.getComponents()) {
        drawComponent(component, g);
      }
    }
  }

  private void drawComponent(Component component, Graphics2D g) {
    switch (component.getType()) {
      case CONNECTION:
        g.setColor(Color.BLUE);
        g.draw(toPath(component.getPoly(), false));
        break;
      case RIGID:
        g.setColor(Color.BLUE);
        g.draw(toPath(component.getPoly(), true));
        break;
      default:
        g.setColor(Color.BLUE);
        g.draw(toPath(component.getPoly(), true));
        break;
    }
  }

  private Color linear(final Color c1, final Color c2, final Color c3, double x1, double x2, double x3, double x, float alpha) {
    if (x < x2) {
      return mid(c1, c2, x1, x2, x, alpha);
    }
    return mid(c2, c3, x2, x3, x, alpha);
  }

  private Color mid(final Color c1, final Color c2, double min, double max, double x, float alpha) {
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

  private Path2D toPath(Poly poly, boolean close) {
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

  public static class Frame {

    private final double x1;
    private final double x2;
    private final double y1;
    private final double y2;

    public Frame(double x1, double x2, double y1, double y2) {
      this.x1 = x1;
      this.x2 = x2;
      this.y1 = y1;
      this.y2 = y2;
    }

    public double getX1() {
      return x1;
    }

    public double getX2() {
      return x2;
    }

    public double getY1() {
      return y1;
    }

    public double getY2() {
      return y2;
    }

  }

  public static class FrameFollower {

    private final double sizeRelativeMargin;

    private final EvictingQueue<Double> centerYQueue;
    private final EvictingQueue<Double> minXQueue;
    private final EvictingQueue<Double> maxXQueue;

    public FrameFollower(int windowSize, double sizeRelativeMargin) {
      this.sizeRelativeMargin = sizeRelativeMargin;
      centerYQueue = EvictingQueue.create(windowSize);
      minXQueue = EvictingQueue.create(windowSize);
      maxXQueue = EvictingQueue.create(windowSize);
    }

    public Frame getFrame(Compound compound, double ratio) {
      //get center and width
      double cy = 0d;
      double minX = Double.POSITIVE_INFINITY;
      double maxX = Double.NEGATIVE_INFINITY;
      for (Component component : compound.getComponents()) {
        cy = cy + component.getPoly().center().y;
        for (Point2 point : component.getPoly().getVertexes()) {
          if (point.x < minX) {
            minX = point.x;
          }
          if (point.x > maxX) {
            maxX = point.x;
          }
        }
      }
      centerYQueue.offer(cy / (double) compound.getComponents().size());
      minXQueue.offer(minX);
      maxXQueue.offer(maxX);
      //get moving average
      double x1 = minXQueue.stream().mapToDouble(Double::doubleValue).average().orElse(0d);
      double x2 = maxXQueue.stream().mapToDouble(Double::doubleValue).average().orElse(1d);
      double yc = centerYQueue.stream().mapToDouble(Double::doubleValue).average().orElse(0d);
      //enlarge width
      double w = x2 - x1;
      x1 = x1 - w * sizeRelativeMargin;
      x2 = x2 + w * sizeRelativeMargin;
      //compute y1 and y2
      double h = (x2 - x1) / ratio;
      double y1 = yc - h / 2;
      double y2 = yc + h / 2;
      //return
      return new Frame(x1, x2, y1, y2);
    }

  }

}
