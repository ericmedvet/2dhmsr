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
import it.units.erallab.hmsrobots.objects.snapshot.Component;
import it.units.erallab.hmsrobots.objects.snapshot.Poly;
import it.units.erallab.hmsrobots.objects.snapshot.Compound;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Viewer extends JFrame {

  private final static int TARGET_FPS = 50;
  private final static int MAX_QUEUE_SIZE = 10000;

  private final Canvas canvas;
  private final JLabel infoLabel;
  private final JSlider timeScaleSlider;
  private final JCheckBox playCheckBox;
  private final JProgressBar queueProgressBar;

  private final ScheduledExecutorService scheduledExecutorService;
  private final EvictingQueue<WorldEvent> queue;
  private final Set<VoxelVizMode> voxelVizModes;

  private double timeScale = 1d;
  private double worldTime = 0d;
  private double localTime = 0d;
  private double worldX1 = -5d, worldY1 = -5d, worldX2 = 105d, worldY2 = 75d;

  public static enum VoxelVizMode {
    POLY, FILL_AREA, SPRINGS, COMPONENTS
  }

  public Viewer(ScheduledExecutorService scheduledExecutorService) {
    super("World viewer");
    //create things
    this.scheduledExecutorService = scheduledExecutorService;
    queue = EvictingQueue.create(MAX_QUEUE_SIZE);
    voxelVizModes = EnumSet.allOf(VoxelVizMode.class);
    //create/set ui components
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Dimension dimension = new Dimension(1000, 800);
    canvas = new Canvas();
    canvas.setPreferredSize(dimension);
    canvas.setMinimumSize(dimension);
    canvas.setMaximumSize(dimension);
    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
    infoLabel = new JLabel();
    queueProgressBar = new JProgressBar(0, MAX_QUEUE_SIZE);
    timeScaleSlider = new JSlider(JSlider.HORIZONTAL, 1, 50, 10);
    playCheckBox = new JCheckBox("Play", true);
    //set layout and put components
    bottomPanel.add(infoLabel);
    bottomPanel.add(queueProgressBar);
    bottomPanel.add(timeScaleSlider);
    bottomPanel.add(playCheckBox);
    getContentPane().add(canvas, BorderLayout.CENTER);
    getContentPane().add(bottomPanel, BorderLayout.PAGE_END);
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
              worldTime = event.getTime();
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
    g.setColor(Color.BLACK);
    g.fill(new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1));
    for (Compound object : event.getSnapshots()) {
      drawObject(object, g);
    }
    //inverse transform    
    g.setTransform(oAt);
    //info
    g.setColor(Color.WHITE);
    g.drawString(String.format("%6.2f", event.getTime()), 1, 1 + g.getFontMetrics().getMaxAscent());
    //finalize
    g.dispose();
    BufferStrategy strategy = canvas.getBufferStrategy();
    if (!strategy.contentsLost()) {
      strategy.show();
    }
    Toolkit.getDefaultToolkit().sync();
  }

  private void drawObject(Compound snapshot, Graphics2D g) {
    if (snapshot.getObjectClass().equals(Voxel.class)) {
      for (Component component : snapshot.getComponents()) {
        if (component.getType().equals(Component.Type.ENCLOSING)) {
          final double area = area(component.getPoly());
          g.setColor(mid(Color.GREEN, Color.RED, Math.round(1d - area / Voxel.SIDE_LENGHT / Voxel.SIDE_LENGHT)));
          g.fill(toPath(component.getPoly(), true));
        }
      }
    } else {
      for (Component component : snapshot.getComponents()) {
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

  private Color mid(final Color c1, final Color c2, double x) {
    x = Math.max(0d, Math.min(1d, x));
    final double r1 = c1.getRed();
    final double g1 = c1.getGreen();
    final double b1 = c1.getBlue();
    final double r2 = c2.getRed();
    final double g2 = c2.getGreen();
    final double b2 = c2.getBlue();
    final long r = Math.round(r1 + (r2 - r1) * x);
    final long g = Math.round(g1 + (g2 - g1) * x);
    final long b = Math.round(b1 + (b2 - b1) * x);
    return new Color((int)r, (int)g, (int)b);
  }

  private double area(Poly poly) {
    double a = 0d;
    int l = poly.getVertexes().length;
    for (int i = 0; i < l; i++) {
      a = a + poly.getVertexes()[i].x * (poly.getVertexes()[(l + i + 1) % l].y - poly.getVertexes()[(l + i - 1) % l].y);
    }
    a = 0.5d * Math.abs(a);
    return a;
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
