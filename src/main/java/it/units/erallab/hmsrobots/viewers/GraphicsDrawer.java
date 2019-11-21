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
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import it.units.erallab.hmsrobots.objects.immutable.Snapshot;
import it.units.erallab.hmsrobots.objects.Ground;
import it.units.erallab.hmsrobots.objects.Voxel;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 *
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
    GRID_MAJOR, GRID_MINOR, VIEWPORT_INFO, TIME_INFO
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
      GENERAL_RENDERING_MODES.add(GeneralRenderingMode.VIEWPORT_INFO);
    }
    
    public static RenderingDirectives create() {
      return new RenderingDirectives();
    }
    
    private LinkedHashMultimap<VoxelRenderingMean, Voxel.Sensor> meanSensorMap;
    private Set<VoxelRenderingMode> voxelRenderingModes;
    private Set<GeneralRenderingMode> generalRenderingModes;

    private RenderingDirectives(LinkedHashMultimap<VoxelRenderingMean, Voxel.Sensor> meanSensorMap, Set<VoxelRenderingMode> voxelRenderingModes, Set<GeneralRenderingMode> generalRenderingModes) {
      this.meanSensorMap = LinkedHashMultimap.create(MEAN_SENSOR_MAP);
      this.voxelRenderingModes = Sets.newHashSet(VOXEL_RENDERING_MODES);
      this.generalRenderingModes = Sets.newHashSet(GENERAL_RENDERING_MODES);
    }
    
    private RenderingDirectives() {
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
  
  private final static EnumMap<Voxel.Sensor, Function<VoxelComponent, Range<Double>>> SENSOR_DOMAIN_FUNCTIONS;
  
  static {
    final double sideRatio = 1.5d;
    SENSOR_DOMAIN_FUNCTIONS = new EnumMap<>(Voxel.Sensor.class);
    SENSOR_DOMAIN_FUNCTIONS.put(Voxel.Sensor.ANGLE, (v) -> Range.closed(-Math.PI, Math.PI));
    SENSOR_DOMAIN_FUNCTIONS.put(Voxel.Sensor.AREA_RATIO, (v) -> Range.closed(0.5d, 1.5d));
    SENSOR_DOMAIN_FUNCTIONS.put(Voxel.Sensor.BROKEN_RATIO, (v) -> Range.closed(0d, 1d));    
    SENSOR_DOMAIN_FUNCTIONS.put(Voxel.Sensor.VELOCITY_MAGNITUDE, (v) -> Range.closed(0d, v.getSideLength()*sideRatio*Math.sqrt(2d)));
    Function<VoxelComponent, Range<Double>> vDomainFunction = (v) -> Range.closed(-v.getSideLength()*sideRatio, v.getSideLength()*sideRatio);
    SENSOR_DOMAIN_FUNCTIONS.put(Voxel.Sensor.X_VELOCITY, vDomainFunction);
    SENSOR_DOMAIN_FUNCTIONS.put(Voxel.Sensor.Y_VELOCITY, vDomainFunction);
    SENSOR_DOMAIN_FUNCTIONS.put(Voxel.Sensor.X_ROT_VELOCITY, vDomainFunction);
    SENSOR_DOMAIN_FUNCTIONS.put(Voxel.Sensor.Y_ROT_VELOCITY, vDomainFunction);    
    SENSOR_DOMAIN_FUNCTIONS.put(Voxel.Sensor.LAST_APPLIED_FORCE, (v) -> Range.closed(-1d, 1d));    
  }

  private GraphicsDrawer() {
    builder = Builder.create();
  }

  private GraphicsDrawer(Builder builder) {
    this.builder = builder;
  }

  public void draw(Snapshot snapshot, Graphics2D g, Frame graphicsFrame, Frame worldFrame, RenderingDirectives directives, String... infos) {
    //set clipping area
    g.setClip(
            (int) graphicsFrame.getX1(), (int) graphicsFrame.getY1(),
            (int) (graphicsFrame.getX2() - graphicsFrame.getX1()), (int) (graphicsFrame.getY2() - graphicsFrame.getY1())
    );
    //save original transform
    AffineTransform oAt = g.getTransform();
    //prepare transformation
    double xRatio = (graphicsFrame.getX2() - graphicsFrame.getX1()) / (worldFrame.getX2() - worldFrame.getX1());
    double yRatio = (graphicsFrame.getY2() - graphicsFrame.getY1()) / (worldFrame.getY2() - worldFrame.getY1());
    double ratio = Math.min(xRatio, yRatio);
    AffineTransform at = new AffineTransform();
    at.translate(graphicsFrame.getX1(), graphicsFrame.getY1());
    at.scale(ratio, -ratio);
    at.translate(-worldFrame.getX1(), -worldFrame.getY2());
    //draw background
    g.setColor(builder.getBackgroundColor());
    g.fillRect(
            (int) graphicsFrame.getX1(), (int) graphicsFrame.getY1(),
            (int) (graphicsFrame.getX2() - graphicsFrame.getX1()), (int) (graphicsFrame.getY2() - graphicsFrame.getY1())
    );
    //draw grid
    g.setTransform(at);
    if (directives.getGeneralRenderingModes().contains(GeneralRenderingMode.GRID_MAJOR) || directives.getGeneralRenderingModes().contains(GeneralRenderingMode.GRID_MINOR)) {
      g.setColor(builder.getGridColor());
      g.setStroke(new BasicStroke(1f / (float) ratio));
      double gridSize = computeGridSize(worldFrame.getX1(), worldFrame.getX2());
      if (directives.getGeneralRenderingModes().contains(GeneralRenderingMode.GRID_MAJOR)) {
        for (double gridX = Math.floor(worldFrame.getX1() / gridSize) * gridSize; gridX < worldFrame.getX2(); gridX = gridX + gridSize) {
          g.draw(new Line2D.Double(gridX, worldFrame.getY1(), gridX, worldFrame.getY2()));
        }
        for (double gridY = Math.floor(worldFrame.getY1() / gridSize) * gridSize; gridY < worldFrame.getY2(); gridY = gridY + gridSize) {
          g.draw(new Line2D.Double(worldFrame.getX1(), gridY, worldFrame.getX2(), gridY));
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
        for (double gridX = Math.floor(worldFrame.getX1() / gridSize) * gridSize; gridX < worldFrame.getX2(); gridX = gridX + gridSize) {
          g.draw(new Line2D.Double(gridX, worldFrame.getY1(), gridX, worldFrame.getY2()));
        }
        for (double gridY = Math.floor(worldFrame.getY1() / gridSize) * gridSize; gridY < worldFrame.getY2(); gridY = gridY + gridSize) {
          g.draw(new Line2D.Double(worldFrame.getX1(), gridY, worldFrame.getX2(), gridY));
        }
      }
    }
    //draw components
    g.setStroke(new BasicStroke(2f / (float) ratio));
    for (Compound object : snapshot.getCompounds()) {
      draw(object, g, directives);
    }
    //restore transform    
    g.setTransform(oAt);
    //info
    StringBuilder sb = new StringBuilder();
    if (directives.getGeneralRenderingModes().contains(GeneralRenderingMode.VIEWPORT_INFO)) {
      sb.append((sb.length() > 0) ? " " : "").append(String.format("vp=(%.0f;%.0f)->(%.0f;%.0f)", worldFrame.getX1(), worldFrame.getY1(), worldFrame.getX2(), worldFrame.getY2()));
    }
    if (directives.getGeneralRenderingModes().contains(GeneralRenderingMode.TIME_INFO)) {
      sb.append((sb.length() > 0) ? " " : "").append(String.format("t=%.2f", snapshot.getTime()));
    }
    for (String info : infos) {
      sb.append((sb.length() > 0) ? " " : "").append(info);
    }
    if (sb.length()>0) {
      g.setColor(builder.getInfoColor());
      g.drawString(sb.toString(), (int) graphicsFrame.getX1() + 1, (int) graphicsFrame.getY1() + 1 + g.getFontMetrics().getMaxAscent());
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

  private void draw(Compound compound, Graphics2D g, RenderingDirectives directives) {
    if (compound.getObjectClass().equals(VoxelCompound.class)) {
      for (Component component : compound.getComponents()) {
        if (component.getType().equals(Component.Type.ENCLOSING)) {
          VoxelComponent voxelComponent = (VoxelComponent) component;
          final Point2 c = component.getPoly().center();
          //iterate over rendering mean
          for (Map.Entry<VoxelRenderingMean, Voxel.Sensor> entry : directives.getMeanSensorMap().entries()) {
            double value = voxelComponent.getSensorReadings().get(entry.getValue());
            double normalizedValue = value;
            if (SENSOR_DOMAIN_FUNCTIONS.containsKey(entry.getValue())) {
              Range<Double> domain = SENSOR_DOMAIN_FUNCTIONS.get(entry.getValue()).apply(voxelComponent);
              normalizedValue = (value-domain.lowerEndpoint())/(domain.upperEndpoint()-domain.lowerEndpoint());
              normalizedValue = Math.max(0d, normalizedValue);
              normalizedValue = Math.min(1d, normalizedValue);
            }
            if (entry.getKey().equals(VoxelRenderingMean.FILL_COLOR)) {
              Color color = linear(Color.RED, Color.GREEN, Color.YELLOW, 0d, 0.5d, 1d, normalizedValue, builder.getVoxelFillAlpha());
              g.setColor(color);
              g.fill(toPath(component.getPoly(), true));
            } else if (entry.getKey().equals(VoxelRenderingMean.FILL_COLOR)) {
              double r = (voxelComponent.getSideLength() * (1d - 0.5d * normalizedValue)) / 2d;
              g.setColor(Color.BLUE);
              Ellipse2D circle = new Ellipse2D.Double(c.x - r, c.y - r, r * 2d, r * 2d);
              g.draw(circle);              
            } else if (entry.getKey().equals(VoxelRenderingMean.CIRCLE)) {
              double r = voxelComponent.getSideLength() * normalizedValue;
              g.setColor(new Color(0f, 0f, 1f, builder.getVoxelFillAlpha()/2));
              Ellipse2D circle = new Ellipse2D.Double(c.x - r, c.y - r, r * 2d, r * 2d);
              g.fill(circle);              
            }
          }
          if (directives.getVoxelRenderingModes().contains(VoxelRenderingMode.POLY)) {
            g.setColor(new Color(0f, 0f, 1f, builder.getVoxelComponentAlpha()));
            g.draw(toPath(component.getPoly(), true));
          }
        } else if (component.getType().equals(Component.Type.CONNECTION) && directives.getVoxelRenderingModes().contains(VoxelRenderingMode.SPRINGS)) {
          g.setColor(Color.BLUE);
          g.draw(toPath(component.getPoly(), false));
        } else if (component.getType().equals(Component.Type.RIGID) && directives.getVoxelRenderingModes().contains(VoxelRenderingMode.COMPONENTS)) {
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

}