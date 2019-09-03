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

import it.units.erallab.hmsrobots.Snapshot;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.image.BufferStrategy;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class MultiViewer extends JFrame {

  private final static Logger L = Logger.getLogger(MultiViewer.class.getName());

  private final static int TARGET_FPS = 50;
  private final static int CANVAS_W = 800;
  private final static int CANVAS_H = 400;
  private static final int TIME_SLIDER_MAX = 1000;

  private final Map<String, Canvas> namedCanvases;
  private final JLabel infoLabel;
  private final Map<GraphicsDrawer.VoxelVizMode, JCheckBox> vizModeCheckBoxes;
  private final JSlider timeSlider;
  private final JButton playButton;
  private final JButton pauseButton;

  private final ScheduledExecutorService scheduledExecutorService;
  private final Map<String, List<Snapshot>> namedSimulations;
  private final double startTime;
  private final double endTime;
  private final GraphicsDrawer graphicsDrawer;

  private double timeScale = 1d;
  private double time = 0d;
  private boolean paused = false;

  public static void main(String[] args) {
    Map<String, List<Snapshot>> namedSimulations = new LinkedHashMap<>();
    String[] fileNames = new String[]{
      //"/home/eric/experiments/2dhmsr/prova10s.serial",
      "/home/eric/experiments/2dhmsr/prova30s-dense.serial"
    };
    for (String fileName : fileNames) {
      try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
        List<Snapshot> events = (List<Snapshot>) ois.readObject();
        namedSimulations.put(fileName, events);
      } catch (FileNotFoundException ex) {
        L.log(Level.SEVERE, String.format("Cannot find file %s", fileName), ex);
      } catch (IOException ex) {
        L.log(Level.SEVERE, String.format("Cannot read file %s", fileName), ex);
      } catch (ClassNotFoundException ex) {
        L.log(Level.SEVERE, String.format("Cannot read object from file %s", fileName), ex);
      }
    }
    MultiViewer multiViewer = new MultiViewer(namedSimulations);
    multiViewer.start();
  }

  public MultiViewer(Map<String, List<Snapshot>> namedSimulations) {
    super("World viewer");
    //create drawer
    graphicsDrawer = GraphicsDrawer.Builder.create().build();
    //create data to be visualized
    this.namedSimulations = new LinkedHashMap<>();
    double localStartTime = Double.POSITIVE_INFINITY;
    double localEndTime = Double.NEGATIVE_INFINITY;
    for (Map.Entry<String, List<Snapshot>> simulationEntry : namedSimulations.entrySet()) {
      Collections.sort(simulationEntry.getValue());
      if (simulationEntry.getValue().get(0).getTime() < localStartTime) {
        localStartTime = simulationEntry.getValue().get(0).getTime();
      }
      if (simulationEntry.getValue().get(simulationEntry.getValue().size() - 1).getTime() > localEndTime) {
        localEndTime = simulationEntry.getValue().get(simulationEntry.getValue().size() - 1).getTime();
      }
      this.namedSimulations.put(simulationEntry.getKey(), simulationEntry.getValue());
    }
    startTime = localStartTime;
    endTime = localEndTime;
    time = startTime;
    //create things
    scheduledExecutorService = Executors.newScheduledThreadPool(1);
    vizModeCheckBoxes = new EnumMap<>(GraphicsDrawer.VoxelVizMode.class);
    namedCanvases = new LinkedHashMap<>();
    //create/set ui components
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    JPanel centralPanel = new JPanel(new GridLayout(namedSimulations.keySet().size(), 1));
    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    //add canvases
    for (String name : namedSimulations.keySet()) {
      Canvas canvas = new Canvas();
      Dimension dimension = new Dimension(CANVAS_W, CANVAS_H);
      canvas.setPreferredSize(dimension);
      namedCanvases.put(name, canvas);
      JPanel panel = new JPanel(new BorderLayout());
      panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), name));
      panel.add(canvas, BorderLayout.CENTER);
      centralPanel.add(panel);
    }
    //components
    infoLabel = new JLabel();
    timeSlider = new JSlider(JSlider.HORIZONTAL, 0, TIME_SLIDER_MAX, 0);
    timeSlider.addChangeListener((ChangeEvent e) -> {
      time = startTime + (endTime - startTime) * (double) timeSlider.getValue() / (double) TIME_SLIDER_MAX;
    });
    playButton = new JButton("Play");
    pauseButton = new JButton("Pause");
    playButton.setEnabled(paused);
    pauseButton.setEnabled(!paused);
    playButton.addActionListener((ActionEvent e) -> {
      paused = !paused;
      playButton.setEnabled(paused);
      pauseButton.setEnabled(!paused);
    });
    pauseButton.addActionListener((ActionEvent e) -> {
      paused = !paused;
      playButton.setEnabled(paused);
      pauseButton.setEnabled(!paused);
    });
    bottomPanel.add(playButton);
    bottomPanel.add(pauseButton);
    bottomPanel.add(timeSlider);
    bottomPanel.add(infoLabel);
    for (GraphicsDrawer.VoxelVizMode mode : GraphicsDrawer.VoxelVizMode.values()) {
      final JCheckBox checkBox = new JCheckBox(
              mode.name().toLowerCase().replace('_', ' '),
              mode.equals(GraphicsDrawer.VoxelVizMode.FILL_AREA)
      );
      vizModeCheckBoxes.put(mode, checkBox);
      topPanel.add(checkBox);
    }
    //set layout and put components
    getContentPane().add(centralPanel, BorderLayout.CENTER);
    getContentPane().add(bottomPanel, BorderLayout.PAGE_END);
    getContentPane().add(topPanel, BorderLayout.PAGE_START);
    //pack
    pack();
  }

  public void start() {
    setVisible(true);
    for (Canvas canvas : namedCanvases.values()) {
      canvas.setIgnoreRepaint(true);
      canvas.createBufferStrategy(2);
    }
    Runnable drawer = new Runnable() {
      private long lastUpdateMillis = System.currentTimeMillis();

      @Override
      public void run() {
        try {
          //get ui params
          Set<GraphicsDrawer.VoxelVizMode> vizModes = getVizModes();
          timeSlider.setValue((int) Math.round((time - startTime) / (endTime - startTime) * (double) TIME_SLIDER_MAX));
          //draw
          long elapsedMillis = System.currentTimeMillis() - lastUpdateMillis;
          lastUpdateMillis = System.currentTimeMillis();
          if (!paused && (time < endTime)) {
            time = time + (double) (elapsedMillis) / 1000d * timeScale;
            for (Map.Entry<String, List<Snapshot>> entry : namedSimulations.entrySet()) {
              Snapshot event = findEvent(time, entry.getValue());
              if (event != null) {
                Canvas canvas = namedCanvases.get(entry.getKey());
                Graphics2D g = (Graphics2D) canvas.getBufferStrategy().getDrawGraphics();
                graphicsDrawer.draw(event, g, canvas.getWidth(), canvas.getHeight(), new GraphicsDrawer.Frame(-5, -5, 100, 75), vizModes);
                BufferStrategy strategy = canvas.getBufferStrategy();
                if (!strategy.contentsLost()) {
                  strategy.show();
                }
                Toolkit.getDefaultToolkit().sync();
              }
            }
          }
          //print info
          infoLabel.setText(String.format(
                  "t=%.3fs [%3.1fx] FPS=%4.1f",
                  time,
                  timeScale,
                  (double) (1000d / (double) (elapsedMillis))
          ));
        } catch (Throwable t) {
          t.printStackTrace();
          System.exit(0);
        }

      }
    };
    scheduledExecutorService.scheduleAtFixedRate(drawer, 0, Math.round(1000d / (double) TARGET_FPS), TimeUnit.MILLISECONDS);
  }

  private Snapshot findEvent(double time, List<Snapshot> events) {
    Snapshot event = null;
    double startT = events.get(0).getTime();
    double endT = events.get(events.size() - 1).getTime();
    int index = (int) Math.min(Math.round((time - startT) / (endT - startT) * events.size()), events.size() - 1);
    while ((index >= 0) && (index + 1 < events.size())) {
      if ((events.get(index).getTime() <= time) && (events.get(index + 1).getTime() > time)) {
        event = events.get(index);
        break;
      }
      if (events.get(index).getTime() > time) {
        index = index - 1;
      } else {
        index = index + 1;
      }
    }
    return event;
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
