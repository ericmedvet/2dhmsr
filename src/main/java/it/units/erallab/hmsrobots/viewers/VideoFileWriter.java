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
import it.units.erallab.hmsrobots.objects.VoxelCompound;
import it.units.erallab.hmsrobots.objects.immutable.Compound;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Flushable;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class VideoFileWriter implements Listener, Flushable {

  private final int w = 600;
  private final int h = 300;
  private final double frameRate = 24;
  private final Set<GraphicsDrawer.RenderingMode> renderingModes = EnumSet.of(
          GraphicsDrawer.RenderingMode.VOXEL_POLY,
          GraphicsDrawer.RenderingMode.VOXEL_FILL_AREA,
          GraphicsDrawer.RenderingMode.GRID_MAJOR,
          GraphicsDrawer.RenderingMode.VIEWPORT_INFO,
          GraphicsDrawer.RenderingMode.TIME_INFO
  );

  private final File file;
  private final BlockingQueue<Snapshot> queue;

  public VideoFileWriter(File file) throws FileNotFoundException, IOException {
    this.file = file;
    queue = new LinkedBlockingQueue<>();
  }

  @Override
  public void listen(Snapshot snapshot) {
    queue.offer(snapshot);
  }

  @Override
  public void flush() throws IOException {
    //prepare things
    SeekableByteChannel channel = NIOUtils.writableChannel(file);
    AWTSequenceEncoder encoder = new AWTSequenceEncoder(channel, Rational.R((int) Math.round(frameRate), 1));
    GraphicsDrawer graphicsDrawer = GraphicsDrawer.Builder.create().build();
    final GraphicsDrawer.FrameFollower frameFollower = new GraphicsDrawer.FrameFollower(50, 1d);
    //iterate over queue
    double targetTime = 0d;
    while (!queue.isEmpty()) {
      Snapshot snapshot;
      while (true) {
        snapshot = queue.poll();
        if (snapshot==null) {
          break;
        }
        if (snapshot.getTime() >= targetTime) {
          break;
        }
      }
      if (snapshot==null) {
        break;
      }
      //update next time
      targetTime = targetTime + 1d / frameRate;
      //obtain viewport
      Compound voxelCompound = null;
      for (Compound compound : snapshot.getCompounds()) {
        if (compound.getObjectClass().equals(VoxelCompound.class)) {
          voxelCompound = compound;
          break;
        }
      }
      GraphicsDrawer.Frame frame = frameFollower.getFrame(voxelCompound, (double) w / h);
      //build image and graphics
      BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
      Graphics2D g = image.createGraphics();
      //draw
      graphicsDrawer.draw(snapshot, g, w, h, frame, renderingModes);
      //encode
      try {
        encoder.encodeImage(image);
      } catch (IOException ex) {
        Logger.getLogger(VideoFileWriter.class.getName()).log(Level.SEVERE, String.format("Cannot encode image due to %s", ex), ex);
      }
    }
    encoder.finish();
    NIOUtils.closeQuietly(channel);
  }

}
