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
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.SnapshotListener;
import it.units.erallab.hmsrobots.tasks.Task;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.viewers.drawers.Drawer;
import it.units.erallab.hmsrobots.viewers.drawers.Drawers;
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
import java.util.function.Function;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class GridOnlineViewer extends JFrame implements GridSnapshotListener {

  private final static int FRAME_RATE = 20;
  private final static int INIT_WIN_WIDTH = 1000;
  private final static int INIT_WIN_HEIGHT = 600;
  private final Grid<Drawer> drawersGrid;
  private final Queue<Grid<TimedSnapshot>> gridQueue;
  private final Grid<Queue<TimedSnapshot>> queueGrid;
  private final Canvas canvas;
  private final ScheduledExecutorService executor;
  private final boolean running;
  private double t;

  public GridOnlineViewer(Grid<String> namesGrid, Grid<Drawer> drawersGrid, ScheduledExecutorService executor) {
    super("World viewer");
    if (namesGrid.getW() != drawersGrid.getW() || namesGrid.getH() != drawersGrid.getH()) {
      throw new IllegalArgumentException("Names grid and drawers grid should have the same size");
    }
    this.drawersGrid = Grid.create(
        namesGrid.getW(),
        namesGrid.getH(),
        (x, y) -> Drawer.clip(
            BoundingBox.of(
                (double) x / (double) namesGrid.getW(),
                (double) y / (double) namesGrid.getH(),
                (double) (x + 1) / (double) namesGrid.getW(),
                (double) (y + 1) / (double) namesGrid.getH()
            ),
            drawersGrid.get(x, y)
        )
    );
    this.executor = executor;
    //create things
    gridQueue = new LinkedList<>();
    queueGrid = Grid.create(namesGrid);
    //create drawer
    for (int x = 0; x < namesGrid.getW(); x++) {
      for (int y = 0; y < namesGrid.getH(); y++) {
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
            Grid<TimedSnapshot> snapshotGrid = Grid.create(queueGrid);
            synchronized (queueGrid) {
              for (Grid.Entry<Queue<TimedSnapshot>> entry : queueGrid) {
                TimedSnapshot snapshot;
                while ((snapshot = entry.value().peek()) != null) {
                  if (snapshot.t < t) {
                    entry.value().poll();
                  } else {
                    break;
                  }
                }
                snapshotGrid.set(entry.key().x(), entry.key().y(), snapshot);
              }
            }
            boolean ready = true;
            for (Grid.Entry<Queue<TimedSnapshot>> entry : queueGrid) {
              ready = ready && ((namesGrid.get(entry.key().x(), entry.key().y()) == null) || (snapshotGrid.get(
                  entry.key().x(),
                  entry.key().y()
              ) != null));
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

  private record TimedSnapshot(double t, Snapshot snapshot) {
  }

  public static <S> void run(
      Task<S, ?> task,
      Grid<NamedValue<S>> namedSolutions,
      Function<String, Drawer> drawerSupplier
  ) {
    ScheduledExecutorService uiExecutor = Executors.newScheduledThreadPool(4);
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    GridOnlineViewer gridOnlineViewer = new GridOnlineViewer(
        Grid.create(namedSolutions, p -> p == null ? null : p.name()),
        Grid.create(namedSolutions, p -> drawerSupplier.apply(p.name())),
        uiExecutor
    );
    gridOnlineViewer.start(3);
    GridEpisodeRunner<S> runner = new GridEpisodeRunner<>(
        Grid.create(namedSolutions, s -> Pair.of(s, task)),
        gridOnlineViewer,
        executor
    );
    runner.run();
  }

  public static <S> void run(Task<S, ?> task, Grid<NamedValue<S>> namedSolutions) {
    run(task, namedSolutions, Drawers::basicWithMiniWorld);
  }

  public static <S> void run(Task<S, ?> task, List<S> ss) {
    int nRows = (int) Math.ceil(Math.sqrt(ss.size()));
    int nCols = (int) Math.ceil((double) ss.size() / (double) nRows);
    Grid<NamedValue<S>> namedSolutions = Grid.create(nRows, nCols);
    for (int i = 0; i < ss.size(); i++) {
      namedSolutions.set(i % nRows, Math.floorDiv(i, nRows), new NamedValue<>(Integer.toString(i), ss.get(i)));
    }
    run(task, namedSolutions);
  }

  public static <S> void run(Task<S, ?> task, S s) {
    run(task, Grid.create(1, 1, new NamedValue<>("", s)));
  }

  @Override
  public SnapshotListener listener(final int lX, final int lY) {
    return (double t, Snapshot snapshot) -> {
      synchronized (queueGrid) {
        queueGrid.get(lX, lY).offer(new TimedSnapshot(t, snapshot));
        queueGrid.notifyAll();
      }
    };
  }

  private void renderFrame(Grid<TimedSnapshot> localSnapshotGrid) {
    //get graphics
    Graphics2D g = (Graphics2D) canvas.getBufferStrategy().getDrawGraphics();
    g.setClip(0, 0, canvas.getWidth(), canvas.getHeight());
    //iterate over snapshot grid
    for (Grid.Entry<TimedSnapshot> entry : localSnapshotGrid) {
      if (entry.value() != null) {
        drawersGrid.get(entry.key().x(), entry.key().y()).draw(entry.value().t, entry.value().snapshot, g);
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
        Grid<TimedSnapshot> localSnapshotGrid = null;
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
    executor.scheduleAtFixedRate(
        drawer,
        Math.round(delay * 1000d),
        Math.round(1000d / (double) FRAME_RATE),
        TimeUnit.MILLISECONDS
    );
  }

}
