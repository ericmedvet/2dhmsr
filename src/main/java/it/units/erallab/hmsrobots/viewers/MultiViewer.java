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

import it.units.erallab.hmsrobots.WorldEvent;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
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
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class MultiViewer extends JFrame {

  private final static Logger L = Logger.getLogger(MultiViewer.class.getName());

  private final static int TARGET_FPS = 25;
  private final static boolean VERTICAL = true;
  private final static int CANVAS_W = 500;
  private final static int CANVAS_H = 400;

  private final Map<String, Canvas> namedCanvases;
  private final JLabel infoLabel;
  private final JSlider timeScaleSlider;
  private final JCheckBox playCheckBox;
  private final Map<CanvasDrawer.VoxelVizMode, JCheckBox> vizModeCheckBoxes;

  private final ScheduledExecutorService scheduledExecutorService;
  private final Map<String, List<WorldEvent>> namedSimulations;

  private double timeScale = 1d;
  private double worldTime = 0d;
  private double localTime = 0d;

  public static void main(String[] args) {
    Map<String, List<WorldEvent>> namedSimulations = new LinkedHashMap<>();
    String[] fileNames = new String[]{
      "/home/eric/experiments/2dhmsr/prova10s.serial",
      "/home/eric/experiments/2dhmsr/prova10s-small.serial"
    };
    for (String fileName : fileNames) {
      try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
        List<WorldEvent> events = (List<WorldEvent>) ois.readObject();
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

  public MultiViewer(Map<String, List<WorldEvent>> namedSimulations) {
    super("World viewer");
    //create data to be visualized
    this.namedSimulations = new LinkedHashMap<>();
    for (Map.Entry<String, List<WorldEvent>> simulationEntry : namedSimulations.entrySet()) {
      Collections.sort(simulationEntry.getValue());
      this.namedSimulations.put(simulationEntry.getKey(), simulationEntry.getValue());
    }
    //create things
    scheduledExecutorService = Executors.newScheduledThreadPool(1);
    vizModeCheckBoxes = new EnumMap<>(CanvasDrawer.VoxelVizMode.class);
    namedCanvases = new LinkedHashMap<>();
    //create/set ui components
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    JPanel centralPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
    for (String name : namedSimulations.keySet()) {
      Canvas canvas = new Canvas();
      Dimension dimension = new Dimension(CANVAS_W, CANVAS_H);
      canvas.setPreferredSize(dimension);
      canvas.setMinimumSize(dimension);
      canvas.setMaximumSize(dimension);
      namedCanvases.put(name, canvas);
      JPanel panel = new JPanel();
      panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), name));
      panel.add(canvas);
      centralPanel.add(panel);
    }
    infoLabel = new JLabel();
    timeScaleSlider = new JSlider(JSlider.HORIZONTAL, 1, 50, 10);
    playCheckBox = new JCheckBox("Play", true);
    for (CanvasDrawer.VoxelVizMode mode : CanvasDrawer.VoxelVizMode.values()) {
      final JCheckBox checkBox = new JCheckBox(mode.name().toLowerCase().replace('_', ' '), true);
      vizModeCheckBoxes.put(mode, checkBox);
      topPanel.add(checkBox);
    }
    //set layout and put components
    bottomPanel.add(infoLabel);
    bottomPanel.add(timeScaleSlider);
    bottomPanel.add(playCheckBox);
    getContentPane().add(centralPanel, BorderLayout.CENTER);
    getContentPane().add(bottomPanel, BorderLayout.PAGE_END);
    getContentPane().add(topPanel, BorderLayout.PAGE_START);
    //add(label);
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
          timeScale = (double) timeScaleSlider.getValue() / 10d;
          Set<CanvasDrawer.VoxelVizMode> vizModes = getVizModes();
          //draw
          long elapsedMillis = System.currentTimeMillis() - lastUpdateMillis;
          lastUpdateMillis = System.currentTimeMillis();
          if (playCheckBox.isSelected()) {
            localTime = localTime + (double) (elapsedMillis) / 1000d;
            worldTime = worldTime + (double) (elapsedMillis) / 1000d * timeScale;
            for (Map.Entry<String, List<WorldEvent>> entry : namedSimulations.entrySet()) {
              WorldEvent event = findEvent(worldTime, entry.getValue());
              if (event != null) {
                CanvasDrawer.draw(event, namedCanvases.get(entry.getKey()), -5, -5, 100, 75, vizModes);
              }
            }
          }
          //print info
          WorldEvent firstEvent, lastEvent;
          int queueSize;
          infoLabel.setText(String.format(
                  "t.w=%.3fs [%3.1fx] FPS=%4.1f",
                  worldTime,
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

  private WorldEvent findEvent(double time, List<WorldEvent> events) {
    WorldEvent event = null;
    while (!events.isEmpty()) {
      event = events.get(0);
      events.remove(0);
      if (events.isEmpty() || (events.get(0).getTime() > time)) {
        break;
      }
    }
    return event;
  }

  private Set<CanvasDrawer.VoxelVizMode> getVizModes() {
    Set<CanvasDrawer.VoxelVizMode> vizModes = new HashSet<>(); //TODO replace with EnumSet
    for (Map.Entry<CanvasDrawer.VoxelVizMode, JCheckBox> entry : vizModeCheckBoxes.entrySet()) {
      if (entry.getValue().isSelected()) {
        vizModes.add(entry.getKey());
      }
    }
    return vizModes;
  }

}
