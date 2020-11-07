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

import it.units.erallab.hmsrobots.core.controllers.TimeFunctions;
import it.units.erallab.hmsrobots.core.objects.ControllableVoxel;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.core.objects.immutable.Snapshot;
import it.units.erallab.hmsrobots.tasks.locomotion.Locomotion;
import it.units.erallab.hmsrobots.util.*;
import it.units.erallab.hmsrobots.viewers.drawers.Ground;
import it.units.erallab.hmsrobots.viewers.drawers.SensorReading;
import it.units.erallab.hmsrobots.viewers.drawers.Voxel;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.dyn4j.dynamics.Settings;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class GridFileWriter implements Flushable, GridSnapshotListener {

  private final int w;
  private final int h;
  private final double startTime;
  private final double frameRate;
  final private File file;

  private final Grid<String> namesGrid;
  private final Queue<Grid<Snapshot>> gridQueue;

  private final Grid<Queue<Snapshot>> queueGrid;
  private final Grid<Framer> framerGrid;
  private final List<BufferedImage> images;

  private final GraphicsDrawer graphicsDrawer;

  private double t;
  private boolean running;

  private static final Logger L = Logger.getLogger(GridFileWriter.class.getName());

  public GridFileWriter(int w, int h, double startTime, double frameRate, File file, Grid<String> namesGrid, ExecutorService executor) throws IOException {
    this(w, h, startTime, frameRate, file, namesGrid, executor, GraphicsDrawer.build());
  }

  public GridFileWriter(int w, int h, double startTime, double frameRate, File file, Grid<String> namesGrid, ExecutorService executor, GraphicsDrawer graphicsDrawer) throws IOException {
    this.w = w;
    this.h = h;
    this.startTime = startTime;
    this.namesGrid = namesGrid;
    this.frameRate = frameRate;
    this.file = file;
    framerGrid = Grid.create(namesGrid);
    gridQueue = new LinkedList<>();
    queueGrid = Grid.create(namesGrid);
    images = new ArrayList<>();
    this.graphicsDrawer = graphicsDrawer;
    for (int x = 0; x < namesGrid.getW(); x++) {
      for (int y = 0; y < namesGrid.getH(); y++) {
        framerGrid.set(x, y, new RobotFollower((int) frameRate * 3, 1.5d, 100, RobotFollower.AggregateType.MAX));
        queueGrid.set(x, y, new LinkedList<>());
      }
    }
    //init time and grid
    t = startTime;
    running = true;
    //start consumer of composed frames
    executor.submit(() -> {
      while (running) {
        Grid<Snapshot> localSnapshotGrid;
        localSnapshotGrid = gridQueue.poll();
        if (localSnapshotGrid != null) {
          renderFrame(localSnapshotGrid);
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
    });
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
              t = t + 1d / frameRate;
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

  @Override
  public SnapshotListener listener(final int lX, final int lY) {
    return (Snapshot snapshot) -> {
      if (snapshot.getTime() >= startTime) {
        synchronized (queueGrid) {
          queueGrid.get(lX, lY).offer(snapshot);
          queueGrid.notifyAll();
        }
      }
    };
  }

  private void renderFrame(Grid<Snapshot> localSnapshotGrid) {
    L.finer(String.format("Writing frame %d/%d", images.size(), images.size() + gridQueue.size()));
    //set local clip size
    double localW = (double) w / (double) namesGrid.getW();
    double localH = (double) h / (double) namesGrid.getH();
    //build image and graphics
    BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
    Graphics2D g = image.createGraphics();
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
    images.add(image);
  }

  @Override
  public void flush() throws IOException {
    while (!gridQueue.isEmpty()) {
      synchronized (gridQueue) {
        try {
          gridQueue.wait();
        } catch (InterruptedException ex) {
          //ignore
        }
      }
    }
    running = false;
    L.info(String.format("Saving video on %s", file));
    StopWatch stopWatch = StopWatch.createStarted();
    VideoUtils.encodeAndSave(images, frameRate, file, VideoUtils.EncoderFramework.JCODEC);
    long millis = stopWatch.getTime(TimeUnit.MILLISECONDS);
    L.info(String.format(
        "Video saved: %.1fMB written in %.2fs",
        Files.size(file.toPath()) / 1024f / 1024f,
        millis / 1000f
    ));
  }

  public static void main(String[] args) throws IOException {
    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    Grid<? extends SensingVoxel> body = Utils.buildBody("worm-4x3-f-t");
    double f = 1d;
    Robot<ControllableVoxel> robot = new Robot<>(
        new TimeFunctions(Grid.create(
            body.getW(),
            body.getH(),
            (final Integer x, final Integer y) -> (Double t) -> Math.sin(-2 * Math.PI * f * t + Math.PI * ((double) x / (double) body.getW()))
        )),
        SerializationUtils.clone(body)
    );
    Locomotion locomotion = new Locomotion(
        10,
        Locomotion.createTerrain("hilly-0.5-5-0"),
        new Settings()
    );
    Grid<Pair<String, Robot<?>>> grid = Grid.create(2, 1);
    grid.set(0, 0, Pair.of("ok", robot));
    grid.set(1, 0, Pair.of("broken", Utils.buildRobotTransformation("breakable-area-1000/500-3/0.5-0").apply(SerializationUtils.clone(robot))));
    //grid.set(1, 0, Pair.of("broken", SerializationUtils.clone(robot)));
    GridSnapshotListener gridSnapshotListener = new GridFileWriter(
        1000, 600, 0d, 30d,
        new File("/home/eric/experiments/2dhmsr/provettina.mp4"),
        Grid.create(grid, Pair::getLeft),
        executorService,
        GraphicsDrawer.build().setConfigurable("drawers", List.of(
            it.units.erallab.hmsrobots.viewers.drawers.Robot.build(),
            Voxel.build(),
            Ground.build(),
            SensorReading.build()
        )).setConfigurable("generalRenderingModes", Set.of(
            GraphicsDrawer.GeneralRenderingMode.TIME_INFO,
            GraphicsDrawer.GeneralRenderingMode.VOXEL_COMPOUND_CENTERS_INFO
        ))
    );
    GridEpisodeRunner<Robot<?>> runner = new GridEpisodeRunner<>(grid, locomotion, gridSnapshotListener, executorService);
    runner.run();
    executorService.shutdownNow();
  }

}
