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
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JLabel;
import org.dyn4j.collision.Fixture;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Shape;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Viewer extends JFrame {

  private final static int TARGET_FPS = 30;
  private final static int MAX_QUEUE_SIZE = 1000;

  private final Canvas canvas;
  private final JLabel label;
  private final ScheduledExecutorService scheduledExecutorService;
  private final EvictingQueue<WorldEvent> queue;

  private double timeScale = 1d;
  private double worldTime = 0d;
  private double worldX1 = 0d, worldY1 = 0d, worldX2 = 100d, worldY2 = 100d;

  public Viewer() {
    super("World viewer");
    //create things
    scheduledExecutorService = Executors.newScheduledThreadPool(1);
    queue = EvictingQueue.create(MAX_QUEUE_SIZE);
    //create/set ui components
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Dimension dimension = new Dimension(1000, 800);
    canvas = new Canvas();
    canvas.setPreferredSize(dimension);
    canvas.setMinimumSize(dimension);
    canvas.setMaximumSize(dimension);
    label = new JLabel();
    //set layout and put components
    getContentPane().add(canvas, BorderLayout.CENTER);
    getContentPane().add(label, BorderLayout.PAGE_END);
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
        long elapsedMillis = System.currentTimeMillis() - lastUpdateMillis;
        double newWorldTime = worldTime+ (double)(elapsedMillis)/1000d*timeScale;
        WorldEvent event = findEvent(newWorldTime);
        if (event!=null) {
          worldTime = newWorldTime;
          draw(event, worldX1, worldY1, worldX2, worldY2);          
        }
        label.setText(String.format(
                "t=%5.2f FPS=%4.1f queue=%4d/%4d",
                worldTime,
                (double) (1000d / (double) (elapsedMillis)),
                queue.size(),
                MAX_QUEUE_SIZE
        ));
        lastUpdateMillis = System.currentTimeMillis();
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
    double xRatio = (x2-x1)/canvas.getWidth();
    double yRatio = (y2-y1)/canvas.getHeight();
    double ratio = Math.max(xRatio, yRatio);
    AffineTransform at = new AffineTransform();
    at.translate(-x1, y1);
    at.scale(1/ratio, 1/ratio);
    g.setTransform(at);
    //draw
    g.setColor(Color.BLACK);
    g.fill(new Rectangle2D.Double(x1, y1, x2-x1, y2-y1));
    g.setColor(Color.RED);
    for (Shape shape : event.getShapes()) {
      if (shape instanceof Rectangle) {
        Rectangle rectangle = (Rectangle)shape;
        Rectangle2D.Double gShape = new Rectangle2D.Double(
                rectangle.getCenter().x-rectangle.getWidth()/2,
                rectangle.getCenter().y-rectangle.getHeight()/2,
                rectangle.getWidth(),
                rectangle.getHeight()
        );
        g.draw(gShape);
      }
    }
    //inverse transform
    g.setTransform(oAt);
    //finalize
    g.dispose();
    BufferStrategy strategy = canvas.getBufferStrategy();
    if (!strategy.contentsLost()) {
      strategy.show();
    }
    Toolkit.getDefaultToolkit().sync();
  }

  private WorldEvent findEvent(double time) {
    WorldEvent event = null;
    synchronized (queue) {
      while (!queue.isEmpty()) {
        event = queue.poll();
        if (queue.peek().getTime()>time) {
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
