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
package it.units.erallab.hmsrobots;

import com.google.common.collect.EvictingQueue;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.objects.immutable.Component;
import it.units.erallab.hmsrobots.objects.immutable.Poly;
import it.units.erallab.hmsrobots.objects.immutable.Compound;
import it.units.erallab.hmsrobots.objects.immutable.VoxelComponent;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import org.dyn4j.geometry.Vector2;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Viewer extends JFrame {

  private final static int TARGET_FPS = 25;
  private final static int MAX_QUEUE_SIZE = 10000;

  private final static double FORCE_CIRCLE_RANGE = 0.5d;
  private final static float VOXEL_FILL_ALPHA = 0.75f;
  private final static float VOXEL_COMPONENT_ALPHA = 0.5f;

  private final Canvas canvas;
  private final JLabel infoLabel;
  private final JSlider timeScaleSlider;
  private final JCheckBox playCheckBox;
  private final JProgressBar queueProgressBar;
  private final Map<VoxelVizMode, JCheckBox> vizModeCheckBoxes;

  private final ScheduledExecutorService scheduledExecutorService;
  private final EvictingQueue<WorldEvent> queue;

  private double timeScale;

  private double worldTime = 0d;
  private double localTime = 0d;
  private double worldX1 = -5d, worldY1 = -5d, worldX2 = 105d, worldY2 = 75d;

  public static enum VoxelVizMode {
    POLY, FILL_AREA, SPRINGS, COMPONENTS, FORCE
  }

  public Viewer(ScheduledExecutorService scheduledExecutorService) {
    super("World viewer");
    //create things
    this.scheduledExecutorService = scheduledExecutorService;
    queue = EvictingQueue.create(MAX_QUEUE_SIZE);
    vizModeCheckBoxes = new EnumMap<>(VoxelVizMode.class);
    //create/set ui components
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Dimension dimension = new Dimension(1000, 800);
    canvas = new Canvas();
    canvas.setPreferredSize(dimension);
    canvas.setMinimumSize(dimension);
    canvas.setMaximumSize(dimension);
    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
    infoLabel = new JLabel();
    queueProgressBar = new JProgressBar(0, MAX_QUEUE_SIZE);
    timeScaleSlider = new JSlider(JSlider.HORIZONTAL, 1, 50, 10);
    playCheckBox = new JCheckBox("Play", true);
    for (VoxelVizMode mode : VoxelVizMode.values()) {
      final JCheckBox checkBox = new JCheckBox(mode.name().toLowerCase().replace('_', ' '), true);
      vizModeCheckBoxes.put(mode, checkBox);
      topPanel.add(checkBox);
    }
    //set layout and put components
    bottomPanel.add(infoLabel);
    bottomPanel.add(queueProgressBar);
    bottomPanel.add(timeScaleSlider);
    bottomPanel.add(playCheckBox);
    getContentPane().add(canvas, BorderLayout.CENTER);
    getContentPane().add(bottomPanel, BorderLayout.PAGE_END);
    getContentPane().add(topPanel, BorderLayout.PAGE_START);
    //add(label);
    //pack
    pack();
  }

  public void start() {
    setVisible(true);
    canvas.setIgnoreRepaint(true);
    canvas.createBufferStrategy(2);
    Runnable drawer = new Runnable() {
      private long lastUpdateMillis = System.currentTimeMillis();

      @Override
      public void run() {
        try {
          //get ui params
          timeScale = (double) timeScaleSlider.getValue() / 10d;
          //draw
          long elapsedMillis = System.currentTimeMillis() - lastUpdateMillis;
          lastUpdateMillis = System.currentTimeMillis();
          if (playCheckBox.isSelected()) {
            localTime = localTime + (double) (elapsedMillis) / 1000d;
            double newWorldTime = worldTime + (double) (elapsedMillis) / 1000d * timeScale;
            WorldEvent event = findEvent(newWorldTime);
            if (event != null) {
              worldTime = newWorldTime;
              draw(event, worldX1, worldY1, worldX2, worldY2);
            }
          }
          //print info
          WorldEvent firstEvent, lastEvent;
          int queueSize;
          synchronized (queue) {
            firstEvent = queue.isEmpty() ? null : ((WorldEvent) (queue.toArray()[0]));
            lastEvent = queue.isEmpty() ? null : ((WorldEvent) (queue.toArray()[queue.size() - 1]));
            queueSize = queue.size();
          }
          infoLabel.setText(String.format(
                  "t.w=%.2fs [%.1fs--%.1fs] [%3.1fx] FPS=%4.1f",
                  worldTime,
                  (firstEvent == null) ? null : firstEvent.getTime(),
                  (lastEvent == null) ? null : lastEvent.getTime(),
                  timeScale,
                  (double) (1000d / (double) (elapsedMillis))
          ));
          queueProgressBar.setValue(queueSize);
        } catch (Throwable t) {
          t.printStackTrace();
          System.exit(0);
        }

      }
    };
    scheduledExecutorService.scheduleAtFixedRate(drawer, 0, Math.round(1000d / (double) TARGET_FPS), TimeUnit.MILLISECONDS);
  }

  private void draw(WorldEvent event, double x1, double y1, double x2, double y2) {
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
    g.setColor(Color.WHITE);
    g.fill(new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1));
    for (Compound object : event.getCompounds()) {
      draw(object, g);
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

  private void draw(Compound compound, Graphics2D g) {
    if (compound.getObjectClass().equals(Voxel.class)) {
      for (Component component : compound.getComponents()) {
        if (component.getType().equals(Component.Type.ENCLOSING)) {
          final double lastArea = ((VoxelComponent) component).getLastArea();
          final double restArea = ((VoxelComponent) component).getRestArea();
          final Vector2 c = component.getPoly().center();
          final double f = ((VoxelComponent) component).getLastAppliedForce();
          final double l = Math.sqrt(restArea);
          if (vizModeCheckBoxes.get(VoxelVizMode.FILL_AREA).isSelected()) {
            final Color color = linear(
                    Color.RED, Color.GREEN, Color.YELLOW,
                    .75d, 1d, 1.25d,
                    lastArea / restArea,
                    VOXEL_FILL_ALPHA
            );
            g.setColor(color);
            g.fill(toPath(component.getPoly(), true));
          }
          if (vizModeCheckBoxes.get(VoxelVizMode.POLY).isSelected()) {
            g.setColor(new Color(0f, 0f, 1f, VOXEL_COMPONENT_ALPHA));
            g.draw(toPath(component.getPoly(), true));
          }
          if (vizModeCheckBoxes.get(VoxelVizMode.FORCE).isSelected()) {
            g.setColor(Color.BLUE);
            double r = (l * (1 - FORCE_CIRCLE_RANGE * f)) / 2d;
            Ellipse2D circle = new Ellipse2D.Double(c.x - r, c.y - r, r * 2d, r * 2d);
            g.draw(circle);
          }
        } else if (component.getType().equals(Component.Type.CONNECTION) && vizModeCheckBoxes.get(VoxelVizMode.SPRINGS).isSelected()) {
          g.setColor(Color.BLUE);
          g.draw(toPath(component.getPoly(), false));
        } else if (component.getType().equals(Component.Type.RIGID) && vizModeCheckBoxes.get(VoxelVizMode.COMPONENTS).isSelected()) {
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

  private void drawComponent(Component component, Graphics2D g) {
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

  private WorldEvent findEvent(double time) {
    WorldEvent event = null;
    synchronized (queue) {
      while (!queue.isEmpty()) {
        event = queue.poll();
        if (queue.isEmpty() || (queue.peek().getTime() > time)) {
          break;
        }
      }
    }
    return event;
  }

  public void listen(WorldEvent event) {
    synchronized (queue) {
      queue.add(event);
    }
  }

}
