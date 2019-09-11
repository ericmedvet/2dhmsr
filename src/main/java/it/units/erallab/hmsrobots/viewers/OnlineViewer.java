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
import it.units.erallab.hmsrobots.objects.VoxelCompound;
import it.units.erallab.hmsrobots.objects.immutable.Compound;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
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
public class OnlineViewer extends JFrame implements Listener {

  private final static int TARGET_FPS = 50;
  private final static int MAX_QUEUE_SIZE = 10000;

  private final Canvas canvas;
  private final JLabel infoLabel;
  private final JSlider timeScaleSlider;
  private final JCheckBox playCheckBox;
  private final JProgressBar queueProgressBar;
  private final Map<GraphicsDrawer.VoxelVizMode, JCheckBox> vizModeCheckBoxes;

  private final ScheduledExecutorService scheduledExecutorService;
  private final EvictingQueue<Snapshot> queue;
  private final GraphicsDrawer graphicsDrawer;

  private double timeScale;

  private double worldTime = 0d;
  private double localTime = 0d;

  public OnlineViewer(ScheduledExecutorService scheduledExecutorService) {
    super("World viewer");
    //create things
    this.scheduledExecutorService = scheduledExecutorService;
    queue = EvictingQueue.create(MAX_QUEUE_SIZE);
    vizModeCheckBoxes = new EnumMap<>(GraphicsDrawer.VoxelVizMode.class);
    //create drawer
    graphicsDrawer = GraphicsDrawer.Builder.create().build();
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
    playCheckBox = new JCheckBox("Play", false);
    for (GraphicsDrawer.VoxelVizMode mode : GraphicsDrawer.VoxelVizMode.values()) {
      final JCheckBox checkBox = new JCheckBox(
              mode.name().toLowerCase().replace('_', ' '),
              mode.equals(GraphicsDrawer.VoxelVizMode.FILL_AREA)
      );
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

    final GraphicsDrawer.FrameFollower frameFollower = new GraphicsDrawer.FrameFollower(50, 1d);

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
            Snapshot snapshot = findSnapshot(newWorldTime);
            if (snapshot != null) {
              worldTime = newWorldTime;
              Graphics2D g = (Graphics2D) canvas.getBufferStrategy().getDrawGraphics();
              //get voxel compound
              Compound voxelCompound = null;
              for (Compound compound : snapshot.getCompounds()) {
                if (compound.getObjectClass().equals(VoxelCompound.class)) {
                  voxelCompound = compound;
                  break;
                }
              }
              GraphicsDrawer.Frame frame = frameFollower.getFrame(voxelCompound, (double)canvas.getWidth()/(double)canvas.getHeight());
              //draw
              graphicsDrawer.draw(snapshot, g, canvas.getWidth(), canvas.getHeight(), frame, getVizModes());
              BufferStrategy strategy = canvas.getBufferStrategy();
              if (!strategy.contentsLost()) {
                strategy.show();
              }
              Toolkit.getDefaultToolkit().sync();
            }
          }
          //print info
          Snapshot firstSnapshot;
          Snapshot lastSnapshot;
          int queueSize;
          synchronized (queue) {
            firstSnapshot = queue.isEmpty() ? null : ((Snapshot) (queue.toArray()[0]));
            lastSnapshot = queue.isEmpty() ? null : ((Snapshot) (queue.toArray()[queue.size() - 1]));
            queueSize = queue.size();
          }
          infoLabel.setText(String.format(
                  "t.w=%.2fs [%.1fs--%.1fs] [%3.1fx] FPS=%4.1f",
                  worldTime,
                  (firstSnapshot == null) ? null : firstSnapshot.getTime(),
                  (lastSnapshot == null) ? null : lastSnapshot.getTime(),
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

  public void listen(Snapshot snapshot) {
    synchronized (queue) {
      queue.add(snapshot);
    }
  }

  private Set<GraphicsDrawer.VoxelVizMode> getVizModes() {
    Set<GraphicsDrawer.VoxelVizMode> vizModes = new HashSet<>();
    for (Map.Entry<GraphicsDrawer.VoxelVizMode, JCheckBox> entry : vizModeCheckBoxes.entrySet()) {
      if (entry.getValue().isSelected()) {
        vizModes.add(entry.getKey());
      }
    }
    return vizModes;
  }

}
