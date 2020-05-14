/*
 * Copyright (C) 2020 Eric Medvet <eric.medvet@gmail.com> (as eric)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.viewers;

import com.google.common.base.Stopwatch;
import it.units.erallab.hmsrobots.core.objects.immutable.Snapshot;
import it.units.erallab.hmsrobots.util.BoundingBox;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.Point2;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class GridOnlineViewer extends JFrame implements GridSnapshotListener {

  private final static int FRAME_RATE = 20;
  private final static int INIT_WIN_WIDTH = 1000;
  private final static int INIT_WIN_HEIGHT = 600;

  private final Grid<String> namesGrid;
  private final Queue<Grid<Snapshot>> gridQueue;
  private final Grid<Queue<Snapshot>> queueGrid;
  private final Grid<Framer> framerGrid;

  private final Canvas canvas;
  private final GraphicsDrawer graphicsDrawer;
  private final ScheduledExecutorService executor;

  private double t;
  private final boolean running;

  public GridOnlineViewer(Grid<String> namesGrid, ScheduledExecutorService executor) {
    super("World viewer");
    this.namesGrid = namesGrid;
    this.executor = executor;
    //create things
    framerGrid = Grid.create(namesGrid);
    gridQueue = new LinkedList<>();
    queueGrid = Grid.create(namesGrid);
    //create drawer
    graphicsDrawer = GraphicsDrawer.build();
    for (int x = 0; x < namesGrid.getW(); x++) {
      for (int y = 0; y < namesGrid.getH(); y++) {
        framerGrid.set(x, y, new RobotFollower(FRAME_RATE * 3, 1.5d, 100, RobotFollower.AggregateType.MAX));
        queueGrid.set(x, y, new LinkedList<>());
      }
    }
    //create/set ui components
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Dimension dimension = new Dimension(INIT_WIN_WIDTH, INIT_WIN_HEIGHT);
    canvas = new Canvas();
    canvas.setPreferredSize(dimension);
    canvas.setMinimumSize(dimension);
    canvas.setMaximumSize(dimension);
    getContentPane().add(new ConfigurablePane(graphicsDrawer), BorderLayout.LINE_END);
    getContentPane().add(canvas, BorderLayout.CENTER);
    //pack
    pack();
    //init time and grid
    t = 0d;
    running = true;
    //start consumer of single frames
    executor.submit(() -> {
          while (running) {
            //check if ready
            Grid<Snapshot> snapshotGrid = Grid.create(queueGrid);
            synchronized (queueGrid) {
              for (Grid.Entry<Queue<Snapshot>> entry : queueGrid) {
                Snapshot snapshot;
                while ((snapshot = entry.getValue().peek()) != null) {
                  if (snapshot.getTime() < t) {
                    entry.getValue().poll();
                  } else {
                    break;
                  }
                }
                snapshotGrid.set(entry.getX(), entry.getY(), snapshot);
              }
            }
            boolean ready = true;
            for (Grid.Entry<Queue<Snapshot>> entry : queueGrid) {
              ready = ready && ((namesGrid.get(entry.getX(), entry.getY()) == null) || (snapshotGrid.get(entry.getX(), entry.getY()) != null));
            }
            if (ready) {
              //update time
              t = t + 1d / FRAME_RATE;
              //render asynchronously
              synchronized (gridQueue) {
                gridQueue.offer(Grid.copy(snapshotGrid));
                gridQueue.notifyAll();
              }
            } else {
              synchronized (queueGrid) {
                try {
                  queueGrid.wait();
                } catch (InterruptedException ex) {
                  //ignore
                }
              }
            }
          }
        }
    );
  }

  public void start(int delay) {
    setVisible(true);
    canvas.setIgnoreRepaint(true);
    canvas.createBufferStrategy(2);
    //start consumer of composed frames
    Runnable drawer = new Runnable() {
      final Stopwatch stopwatch = Stopwatch.createUnstarted();

      @Override
      public void run() {
        if (!stopwatch.isRunning()) {
          stopwatch.start();
        }
        double currentTime = (double) stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000d;
        Grid<Snapshot> localSnapshotGrid = null;
        synchronized (gridQueue) {
          while (!gridQueue.isEmpty()) {
            localSnapshotGrid = gridQueue.poll();
            if (gridQueue.isEmpty() || (gridQueue.peek().get(0, 0).getTime() > currentTime)) {
              break;
            }
          }
        }
        if (localSnapshotGrid != null) {
          try {
            renderFrame(localSnapshotGrid);
          } catch (Throwable t) {
            t.printStackTrace();
            System.exit(0);
          }
          synchronized (gridQueue) {
            gridQueue.notifyAll();
          }
        } else {
          synchronized (gridQueue) {
            try {
              gridQueue.wait();
            } catch (InterruptedException ex) {
              //ignore
            }
          }
        }

      }
    };
    executor.scheduleAtFixedRate(drawer, Math.round(delay * 1000d), Math.round(1000d / (double) FRAME_RATE), TimeUnit.MILLISECONDS);
  }

  private String name(Enum<?> e) {
    return e.name().replace("_", " ").toLowerCase();
  }

  @Override
  public SnapshotListener listener(final int lX, final int lY) {
    return (Snapshot snapshot) -> {
      synchronized (queueGrid) {
        queueGrid.get(lX, lY).offer(snapshot);
        queueGrid.notifyAll();
      }
    };
  }

  private void renderFrame(Grid<Snapshot> localSnapshotGrid) {
    //set local clip size
    double localW = (double) canvas.getWidth() / (double) namesGrid.getW();
    double localH = (double) canvas.getHeight() / (double) namesGrid.getH();
    //get graphics
    Graphics2D g = (Graphics2D) canvas.getBufferStrategy().getDrawGraphics();
    //iterate over snapshot grid
    for (Grid.Entry<Snapshot> entry : localSnapshotGrid) {
      if (entry.getValue() != null) {
        //obtain viewport
        BoundingBox frame = framerGrid.get(entry.getX(), entry.getY()).getFrame(entry.getValue(), localW / localH);
        //draw
        graphicsDrawer.draw(
            entry.getValue(), g,
            BoundingBox.build(
                Point2.build(localW * entry.getX(), localH * entry.getY()),
                Point2.build(localW * (entry.getX() + 1), localH * (entry.getY() + 1))
            ),
            frame, namesGrid.get(entry.getX(), entry.getY())
        );
      }
    }
    //dispose and encode
    g.dispose();
    BufferStrategy strategy = canvas.getBufferStrategy();
    if (!strategy.contentsLost()) {
      strategy.show();
    }
    Toolkit.getDefaultToolkit().sync();
  }

}
