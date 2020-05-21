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

import it.units.erallab.hmsrobots.core.objects.immutable.Snapshot;
import it.units.erallab.hmsrobots.util.BoundingBox;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.Point2;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class GridFileWriter implements Flushable, GridSnapshotListener {

  private final int w;
  private final int h;

  private final Grid<String> namesGrid;
  private final Queue<Grid<Snapshot>> gridQueue;
  private final Grid<Queue<Snapshot>> queueGrid;
  private final Grid<Framer> framerGrid;

  private final SeekableByteChannel channel;
  private final AWTSequenceEncoder encoder;
  private final GraphicsDrawer graphicsDrawer;

  private double t;
  private boolean running;
  private int drawnCount;

  private static final Logger L = Logger.getLogger(GridFileWriter.class.getName());

  public GridFileWriter(int w, int h, double frameRate, File file, Grid<String> namesGrid, ExecutorService executor) throws IOException {
    this.w = w;
    this.h = h;
    this.namesGrid = namesGrid;
    framerGrid = Grid.create(namesGrid);
    gridQueue = new LinkedList<>();
    queueGrid = Grid.create(namesGrid);
    //prepare things
    channel = NIOUtils.writableChannel(file);
    encoder = new AWTSequenceEncoder(channel, Rational.R((int) Math.round(frameRate), 1));
    graphicsDrawer = GraphicsDrawer.build();
    for (int x = 0; x < namesGrid.getW(); x++) {
      for (int y = 0; y < namesGrid.getH(); y++) {
        framerGrid.set(x, y, new RobotFollower((int) frameRate * 3, 1.5d, 100, RobotFollower.AggregateType.MAX));
        queueGrid.set(x, y, new LinkedList<>());
      }
    }
    //init time and grid
    t = 0d;
    running = true;
    drawnCount = 0;
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
      synchronized (queueGrid) {
        queueGrid.get(lX, lY).offer(snapshot);
        queueGrid.notifyAll();
      }
    };
  }

  private void renderFrame(Grid<Snapshot> localSnapshotGrid) {
    L.finer(String.format("Writing frame %d/%d", drawnCount, drawnCount + gridQueue.size()));
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
    //encode
    try {
      encoder.encodeImage(image);
    } catch (IOException ex) {
      L.severe(String.format("Cannot encode image due to %s", ex));
    }
    drawnCount = drawnCount + 1;
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
    L.fine("Flushing data");
    encoder.finish();
    NIOUtils.closeQuietly(channel);
    running = false;
  }

}
