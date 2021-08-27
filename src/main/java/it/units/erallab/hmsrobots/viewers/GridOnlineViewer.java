/*
 * Copyright (C) 2021 Eric Medvet <eric.medvet@gmail.com> (as Eric Medvet <eric.medvet@gmail.com>)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.viewers;

import com.google.common.base.Stopwatch;
import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.SnapshotListener;
import it.units.erallab.hmsrobots.tasks.Task;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.lang3.tuple.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class GridOnlineViewer extends JFrame implements GridSnapshotListener {

  private class TimedSnapshots {
    private final double t;
    private final List<Snapshot> snapshots;

    public TimedSnapshots(double t, List<Snapshot> snapshots) {
      this.t = t;
      this.snapshots = snapshots;
    }
  }

  private final static int FRAME_RATE = 20;
  private final static int INIT_WIN_WIDTH = 1000;
  private final static int INIT_WIN_HEIGHT = 600;

  private final Grid<String> namesGrid;
  private final Queue<Grid<TimedSnapshots>> gridQueue;
  private final Grid<Queue<TimedSnapshots>> queueGrid;
  private final Grid<Framer> framerGrid;

  private final Canvas canvas;
  private final GraphicsDrawer graphicsDrawer;
  private final ScheduledExecutorService executor;

  private double t;
  private final boolean running;

  public GridOnlineViewer(Grid<String> namesGrid, ScheduledExecutorService executor, GraphicsDrawer graphicsDrawer) {
    super("World viewer");
    this.namesGrid = namesGrid;
    this.executor = executor;
    //create things
    framerGrid = Grid.create(namesGrid);
    gridQueue = new LinkedList<>();
    queueGrid = Grid.create(namesGrid);
    //create drawer
    this.graphicsDrawer = graphicsDrawer;
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
            Grid<TimedSnapshots> snapshotGrid = Grid.create(queueGrid);
            synchronized (queueGrid) {
              for (Grid.Entry<Queue<TimedSnapshots>> entry : queueGrid) {
                TimedSnapshots snapshot;
                while ((snapshot = entry.getValue().peek()) != null) {
                  if (snapshot.t < t) {
                    entry.getValue().poll();
                  } else {
                    break;
                  }
                }
                snapshotGrid.set(entry.getX(), entry.getY(), snapshot);
              }
            }
            boolean ready = true;
            for (Grid.Entry<Queue<TimedSnapshots>> entry : queueGrid) {
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

  public GridOnlineViewer(Grid<String> namesGrid, ScheduledExecutorService executor) {
    this(namesGrid, executor, GraphicsDrawer.build());
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
        Grid<TimedSnapshots> localSnapshotGrid = null;
        synchronized (gridQueue) {
          while (!gridQueue.isEmpty()) {
            localSnapshotGrid = gridQueue.poll();
            if (gridQueue.isEmpty() || (gridQueue.peek().get(0, 0).t > currentTime)) {
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
    return (double t, List<Snapshot> snapshots) -> {
      synchronized (queueGrid) {
        queueGrid.get(lX, lY).offer(new TimedSnapshots(t, snapshots));
        queueGrid.notifyAll();
      }
    };
  }

  private void renderFrame(Grid<TimedSnapshots> localSnapshotGrid) {
    //set local clip size
    double localW = (double) canvas.getWidth() / (double) namesGrid.getW();
    double localH = (double) canvas.getHeight() / (double) namesGrid.getH();
    //get graphics
    Graphics2D g = (Graphics2D) canvas.getBufferStrategy().getDrawGraphics();
    //iterate over snapshot grid
    for (Grid.Entry<TimedSnapshots> entry : localSnapshotGrid) {
      if (entry.getValue() != null) {
        //obtain viewport
        BoundingBox frame = framerGrid.get(entry.getX(), entry.getY()).getFrame(entry.getValue().snapshots, localW / localH);
        //draw
        graphicsDrawer.draw(
            entry.getValue().t, entry.getValue().snapshots, g,
            it.units.erallab.hmsrobots.core.geometry.BoundingBox.build(
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

  public static <S> void run(Task<S, ?> task, Grid<Pair<String, S>> namedSolutions) {
    ScheduledExecutorService uiExecutor = Executors.newScheduledThreadPool(4);
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    GridOnlineViewer gridOnlineViewer = new GridOnlineViewer(
        Grid.create(namedSolutions, p -> p == null ? null : p.getLeft()),
        uiExecutor,
        GraphicsDrawer.build()
    );
    gridOnlineViewer.start(3);
    GridEpisodeRunner<S> runner = new GridEpisodeRunner<>(
        namedSolutions,
        task,
        gridOnlineViewer,
        executor
    );
    runner.run();
  }

  public static <S> void run(Task<S, ?> task, List<S> ss) {
    int nRows = (int) Math.ceil(Math.sqrt(ss.size()));
    int nCols = (int) Math.ceil((double) ss.size() / (double) nRows);
    Grid<Pair<String, S>> namedSolutions = Grid.create(nRows, nCols);
    for (int i = 0; i < ss.size(); i++) {
      namedSolutions.set(i % nRows, Math.floorDiv(i, nRows), Pair.of(Integer.toString(i), ss.get(i)));
    }
    run(task, namedSolutions);
  }

  public static <S> void run(Task<S, ?> task, S s) {
    run(task, Grid.create(1, 1, Pair.of("solution", s)));
  }

}
