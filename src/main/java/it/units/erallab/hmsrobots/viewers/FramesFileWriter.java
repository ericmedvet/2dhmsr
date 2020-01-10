/*
 * Copyright (C) 2020 Eric Medvet <eric.medvet@gmail.com>
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

import it.units.erallab.hmsrobots.objects.immutable.Snapshot;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class FramesFileWriter implements Flushable, SnapshotListener {

  public static enum Direction {
    HORIZONTAL, VERTICAL
  };

  private final double initialT;
  private final double finalT;
  private final double dT;
  private final int w;
  private final int h;
  private final Direction direction;
  private final File file;
  private final ExecutorService executor;
  private final GraphicsDrawer.RenderingDirectives renderingDirectives;

  private final GraphicsDrawer graphicsDrawer;
  private final Framer framer;
  private final BufferedImage image;

  private Snapshot lastSnapshot;
  private int frameCount;

  private static final Logger L = Logger.getLogger(FramesFileWriter.class.getName());

  public FramesFileWriter(double initialT, double finalT, double dT, int w, int h, Direction direction, File file, ExecutorService executor, GraphicsDrawer.RenderingDirectives renderingDirectives) {
    this.initialT = initialT;
    this.finalT = finalT;
    this.dT = dT;
    this.w = w;
    this.h = h;
    this.direction = direction;
    this.file = file;
    this.executor = executor;
    this.renderingDirectives = renderingDirectives;
    int frames = (int) Math.floor((finalT - initialT) / dT);
    int overallW = w;
    int overallH = h;
    if (direction.equals(Direction.HORIZONTAL)) {
      overallW = w * frames;
    } else {
      overallH = h * frames;
    }
    image = new BufferedImage(overallW, overallH, BufferedImage.TYPE_3BYTE_BGR);
    graphicsDrawer = GraphicsDrawer.Builder.create().build();
    framer = new VoxelCompoundFollower((int) frames, 1.5d, 100, VoxelCompoundFollower.AggregateType.MAX);
    frameCount = 0;
  }

  @Override
  public void flush() throws IOException {
    L.info(String.format("Saving image with %d frames", frameCount));
    ImageIO.write(image, "png", file);
  }

  @Override
  public void listen(final Snapshot snapshot) {
      Frame worldFrame = framer.getFrame(snapshot, (double) w / (double) h);
      if ((snapshot.getTime() < initialT) || (snapshot.getTime() >= finalT)) { //out of time window
        return;
      }
      if ((lastSnapshot != null) && ((snapshot.getTime() - lastSnapshot.getTime()) < dT)) { //wait for next snapshot
        return;
      }
      lastSnapshot = snapshot;
      Frame imageFrame;
      if (direction.equals(Direction.HORIZONTAL)) {
        imageFrame = new Frame(w * frameCount, w * (frameCount + 1), 0, h);
      } else {
        imageFrame = new Frame(0, w, h * frameCount, h * (frameCount + 1));
      }
      L.info(String.format("Rendering frame %d: %s to %s", frameCount, worldFrame, imageFrame));
      frameCount = frameCount + 1;
      Graphics2D g = image.createGraphics();
      graphicsDrawer.draw(snapshot, g, imageFrame, worldFrame, renderingDirectives, String.format("%d", frameCount));
      g.dispose();
  }

}
