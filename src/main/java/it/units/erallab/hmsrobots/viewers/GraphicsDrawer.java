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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Sets;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.objects.VoxelCompound;
import it.units.erallab.hmsrobots.objects.immutable.*;
import it.units.erallab.hmsrobots.viewers.drawers.Drawer;
import it.units.erallab.hmsrobots.viewers.drawers.JointDrawer;
import it.units.erallab.hmsrobots.viewers.drawers.VoxelDrawer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class GraphicsDrawer {

  public static enum VoxelRenderingMean {
    FILL_COLOR, FILLED_CIRCLE, CIRCLE
  }

  public static enum VoxelRenderingMode {
    POLY, SPRINGS, COMPONENTS
  }

  public static enum GeneralRenderingMode {
    GRID_MAJOR, GRID_MINOR, VIEWPORT_INFO, TIME_INFO, VOXEL_COMPOUND_CENTERS_INFO
  }

  public static class RenderingDirectives {

    private final static LinkedHashMultimap<VoxelRenderingMean, Voxel.Sensor> MEAN_SENSOR_MAP;
    private final static Set<VoxelRenderingMode> VOXEL_RENDERING_MODES;
    private final static Set<GeneralRenderingMode> GENERAL_RENDERING_MODES;

    static {
      MEAN_SENSOR_MAP = LinkedHashMultimap.create();
      MEAN_SENSOR_MAP.put(VoxelRenderingMean.FILL_COLOR, Voxel.Sensor.AREA_RATIO);
      VOXEL_RENDERING_MODES = Sets.newHashSet();
      VOXEL_RENDERING_MODES.add(VoxelRenderingMode.POLY);
      GENERAL_RENDERING_MODES = Sets.newHashSet();
      GENERAL_RENDERING_MODES.add(GeneralRenderingMode.GRID_MAJOR);
      GENERAL_RENDERING_MODES.add(GeneralRenderingMode.TIME_INFO);
      GENERAL_RENDERING_MODES.add(GeneralRenderingMode.VOXEL_COMPOUND_CENTERS_INFO);
    }

    public static RenderingDirectives create() {
      return new RenderingDirectives();
    }

    private LinkedHashMultimap<VoxelRenderingMean, Voxel.Sensor> meanSensorMap;
    private Set<VoxelRenderingMode> voxelRenderingModes;
    private Set<GeneralRenderingMode> generalRenderingModes;

    private RenderingDirectives() {
      this.meanSensorMap = LinkedHashMultimap.create(MEAN_SENSOR_MAP);
      this.voxelRenderingModes = Sets.newHashSet(VOXEL_RENDERING_MODES);
      this.generalRenderingModes = Sets.newHashSet(GENERAL_RENDERING_MODES);
    }

    public LinkedHashMultimap<VoxelRenderingMean, Voxel.Sensor> getMeanSensorMap() {
      return meanSensorMap;
    }

    public Set<VoxelRenderingMode> getVoxelRenderingModes() {
      return voxelRenderingModes;
    }

    public Set<GeneralRenderingMode> getGeneralRenderingModes() {
      return generalRenderingModes;
    }

    public RenderingDirectives sensor(VoxelRenderingMean mean, Voxel.Sensor sensor) {
      meanSensorMap.put(mean, sensor);
      return this;
    }

    public RenderingDirectives voxelRenderingMode(VoxelRenderingMode voxelRenderingMode) {
      voxelRenderingModes.add(voxelRenderingMode);
      return this;
    }

    public RenderingDirectives generalRenderingMode(GeneralRenderingMode generalRenderingMode) {
      generalRenderingModes.add(generalRenderingMode);
      return this;
    }

  }

  public static class Builder {

    private final static float VOXEL_FILL_ALPHA = 0.75f;
    private final static float VOXEL_COMPONENT_ALPHA = 0.5f;
    private final static Color GRID_COLOR = Color.GRAY;
    private final static Color INFO_COLOR = Color.BLUE;
    private final static Color BACKGROUND_COLOR = Color.WHITE;
    private final static Color GROUND_COLOR = Color.BLACK;
    private final static double[] GRID_SIZES = new double[]{2, 5, 10};

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

  public void draw(Snapshot snapshot, Graphics2D g, BoundingBox graphicsFrame, BoundingBox worldFrame, RenderingDirectives directives, String... infos) {
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
    g.setColor(builder.getBackgroundColor());
    g.fillRect(
        (int) graphicsFrame.min.x, (int) graphicsFrame.min.y,
        (int) (graphicsFrame.max.x - graphicsFrame.min.x), (int) (graphicsFrame.max.y - graphicsFrame.min.y)
    );
    //draw grid
    g.setTransform(at);
    if (directives.getGeneralRenderingModes().contains(GeneralRenderingMode.GRID_MAJOR) || directives.getGeneralRenderingModes().contains(GeneralRenderingMode.GRID_MINOR)) {
      g.setColor(builder.getGridColor());
      g.setStroke(new BasicStroke(1f / (float) ratio));
      double gridSize = computeGridSize(worldFrame.min.x, worldFrame.max.x);
      if (directives.getGeneralRenderingModes().contains(GeneralRenderingMode.GRID_MAJOR)) {
        for (double gridX = Math.floor(worldFrame.min.x / gridSize) * gridSize; gridX < worldFrame.max.x; gridX = gridX + gridSize) {
          g.draw(new Line2D.Double(gridX, worldFrame.min.y, gridX, worldFrame.max.y));
        }
        for (double gridY = Math.floor(worldFrame.min.y / gridSize) * gridSize; gridY < worldFrame.max.y; gridY = gridY + gridSize) {
          g.draw(new Line2D.Double(worldFrame.min.x, gridY, worldFrame.max.x, gridY));
        }
      }
      if (directives.getGeneralRenderingModes().contains(GeneralRenderingMode.GRID_MINOR)) {
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

    List<Drawer> drawers = new ArrayList<>();
    drawers.add(new VoxelDrawer());
    drawers.add(new JointDrawer());

    List<Point2> compoundCenters = new ArrayList<>();
    g.setStroke(new BasicStroke(2f / (float) ratio));
    for (ImmutableObject object : snapshot.getObjects()) {
      //draw(object, g, directives);
      recursivelyDraw(object, g, drawers);
      if (directives.getGeneralRenderingModes().contains(GeneralRenderingMode.VOXEL_COMPOUND_CENTERS_INFO)) {
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
    if (directives.getGeneralRenderingModes().contains(GeneralRenderingMode.VIEWPORT_INFO)) {
      sb.append((sb.length() > 0) ? " " : "").append(String.format("vp=(%.0f;%.0f)->(%.0f;%.0f)", worldFrame.min.x, worldFrame.min.y, worldFrame.max.x, worldFrame.max.y));
    }
    if (directives.getGeneralRenderingModes().contains(GeneralRenderingMode.TIME_INFO)) {
      sb.append((sb.length() > 0) ? " " : "").append(String.format("t=%.2f", snapshot.getTime()));
    }
    if (directives.getGeneralRenderingModes().contains(GeneralRenderingMode.VOXEL_COMPOUND_CENTERS_INFO)) {
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
      g.setColor(builder.getInfoColor());
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

  private void recursivelyDraw(final ImmutableObject object, final Graphics2D g, final List<Drawer> drawers) {
    boolean drawChildren = true;
    for (Drawer drawer : drawers) {
      if (match(object.getObjectClass(), drawer.getDrawableClasses())) {
        drawChildren = drawer.draw(object, g);
        break;
      }
    }
    if (drawChildren) {
      object.getChildren().stream().forEach(c -> recursivelyDraw(c, g, drawers));
    }
  }

  private boolean match(final Class c, final Set<Class<? extends Object>> classes) {
    for (Class other : classes) {
      if (other.isAssignableFrom(c)) {
        return true;
      }
    }
    return false;
  }

  private void draw(ImmutableObject object, Graphics2D g, RenderingDirectives directives) {
    //draw all children
    for (ImmutableObject child : object.getChildren()) {
      draw(child, g, directives);
    }
    //draw shape
    if (object.getShape() != null) {
      if (object.getShape() instanceof Poly) {
        Poly poly = (Poly) object.getShape();
        g.setColor(Color.BLUE);
        g.draw(toPath(poly, true));
      } else if (object.getShape() instanceof Vector) {
        Vector vector = (Vector) object.getShape();
        g.setColor(Color.BLUE);
        g.draw(toPath(vector.getStart(), vector.getEnd()));
      }
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

}
