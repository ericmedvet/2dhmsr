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

import com.google.common.base.Stopwatch;
import com.google.common.collect.EvictingQueue;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.objects.immutable.Snapshot;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferStrategy;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.BoxLayout;
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
public class OnlineViewer extends JFrame implements SnapshotListener {

  private final static int TARGET_FPS = 25;
  private final static int MAX_QUEUE_SIZE = 10000;

  private final VoxelCompoundFollower followerFramer;
  private final Canvas canvas;
  private final JLabel infoLabel;
  private final JProgressBar queueProgressBar;

  private final ScheduledExecutorService scheduledExecutorService;
  private final EvictingQueue<Snapshot> queue;
  private final GraphicsDrawer graphicsDrawer;
  private final GraphicsDrawer.RenderingDirectives renderingDirectives;

  public OnlineViewer(ScheduledExecutorService scheduledExecutorService) {
    super("World viewer");
    //create things
    this.scheduledExecutorService = scheduledExecutorService;
    queue = EvictingQueue.create(MAX_QUEUE_SIZE);
    //create drawer
    graphicsDrawer = GraphicsDrawer.Builder.create().build();
    renderingDirectives = GraphicsDrawer.RenderingDirectives.create();
    followerFramer = new VoxelCompoundFollower(TARGET_FPS * 4, 1.5d, 100, VoxelCompoundFollower.AggregateType.MAX);
    //create/set ui components
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Dimension dimension = new Dimension(1000, 800);
    canvas = new Canvas();
    canvas.setPreferredSize(dimension);
    canvas.setMinimumSize(dimension);
    canvas.setMaximumSize(dimension);
    canvas.addMouseWheelListener((MouseWheelEvent e) -> {
      if (e.getWheelRotation() < 0) {
        followerFramer.setSizeRelativeMargin(followerFramer.getSizeRelativeMargin() * 0.9d);
      } else if (e.getWheelRotation() > 0) {
        followerFramer.setSizeRelativeMargin(followerFramer.getSizeRelativeMargin() * 1.1d);
      }
    });
    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
    infoLabel = new JLabel();
    queueProgressBar = new JProgressBar(0, MAX_QUEUE_SIZE);
    //add checkboxes
    JPanel topPanel = new JPanel();
    topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.PAGE_AXIS));
    for (final GraphicsDrawer.VoxelRenderingMean mean : GraphicsDrawer.VoxelRenderingMean.values()) {
      JPanel meanPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0));
      meanPanel.add(new JLabel(name(mean) + ": "));
      for (final Voxel.Sensor sensor : Voxel.Sensor.values()) {
        boolean selected = false;
        if (renderingDirectives.getMeanSensorMap().containsKey(mean)) {
          selected = renderingDirectives.getMeanSensorMap().get(mean).contains(sensor);
        }
        JCheckBox checkBox = new JCheckBox(name(sensor), selected);
        checkBox.addItemListener((ItemEvent e) -> {
          if (e.getStateChange() == ItemEvent.SELECTED) {
            renderingDirectives.getMeanSensorMap().get(mean).add(sensor);
          } else {
            renderingDirectives.getMeanSensorMap().get(mean).remove(sensor);
          }
        });
        meanPanel.add(checkBox);
      }
      topPanel.add(meanPanel);
    }
    JPanel voxelRenderingPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0));
    voxelRenderingPanel.add(new JLabel("voxel: "));
    for (GraphicsDrawer.VoxelRenderingMode mode : GraphicsDrawer.VoxelRenderingMode.values()) {
      JCheckBox checkBox = new JCheckBox(name(mode), renderingDirectives.getVoxelRenderingModes().contains(mode));
      checkBox.addItemListener((ItemEvent e) -> {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          renderingDirectives.getVoxelRenderingModes().add(mode);
        } else {
          renderingDirectives.getVoxelRenderingModes().remove(mode);
        }
      });
      voxelRenderingPanel.add(checkBox);
    }
    topPanel.add(voxelRenderingPanel);
    JPanel generalRenderingPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0));
    generalRenderingPanel.add(new JLabel("general: "));
    for (GraphicsDrawer.GeneralRenderingMode mode : GraphicsDrawer.GeneralRenderingMode.values()) {
      JCheckBox checkBox = new JCheckBox(name(mode), renderingDirectives.getGeneralRenderingModes().contains(mode));
      checkBox.addItemListener((ItemEvent e) -> {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          renderingDirectives.getGeneralRenderingModes().add(mode);
        } else {
          renderingDirectives.getGeneralRenderingModes().remove(mode);
        }
      });
      generalRenderingPanel.add(checkBox);
    }
    topPanel.add(generalRenderingPanel);
    //set layout and put components
    bottomPanel.add(infoLabel);
    bottomPanel.add(queueProgressBar);
    getContentPane().add(topPanel, BorderLayout.PAGE_START);
    getContentPane().add(canvas, BorderLayout.CENTER);
    getContentPane().add(bottomPanel, BorderLayout.PAGE_END);
    //pack
    pack();
  }

  private String name(Enum e) {
    return e.name().replace("_", " ").toLowerCase();
  }

  public void start() {
    setVisible(true);
    canvas.setIgnoreRepaint(true);
    canvas.createBufferStrategy(2);
    final Stopwatch stopwatch = Stopwatch.createStarted();
    Runnable drawer = new Runnable() {
      private double lastTime = (double)stopwatch.elapsed(TimeUnit.MILLISECONDS)/1000d;
      @Override
      public void run() {
        try {
          double currentTime = (double)stopwatch.elapsed(TimeUnit.MILLISECONDS)/1000d;
          //draw
          Snapshot snapshot = findSnapshot(currentTime);
          if (snapshot != null) {
            Graphics2D g = (Graphics2D) canvas.getBufferStrategy().getDrawGraphics();
            //get frame
            Frame frame = followerFramer.getFrame(snapshot, (double) canvas.getWidth() / (double) canvas.getHeight());
            //draw
            graphicsDrawer.draw(snapshot, g, new Frame(0, canvas.getWidth(), 0, canvas.getHeight()), frame, renderingDirectives);
            g.dispose();
            BufferStrategy strategy = canvas.getBufferStrategy();
            if (!strategy.contentsLost()) {
              strategy.show();
            }
            Toolkit.getDefaultToolkit().sync();
          }
          //print info
          int queueSize;
          synchronized (queue) {
            queueSize = queue.size();
          }
          infoLabel.setText(String.format(
                  "t.w=%6.2fs FPS=%4.1f",
                  currentTime,
                  1/(currentTime-lastTime)
          ));
          queueProgressBar.setValue(queueSize);
          lastTime = currentTime;
        } catch (Throwable t) {
          System.exit(0);
        }

      }
    };
    scheduledExecutorService.scheduleAtFixedRate(drawer, 0, Math.round(1000d / (double) TARGET_FPS), TimeUnit.MILLISECONDS);
  }

  private Snapshot findSnapshot(double time) {
    Snapshot event = null;
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

  @Override
  public void listen(Snapshot snapshot) {
    synchronized (queue) {
      queue.add(snapshot);
    }
  }

}
