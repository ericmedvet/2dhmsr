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

import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.SnapshotListener;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class FramesImageBuilder implements SnapshotListener {

  public enum Direction {
    HORIZONTAL, VERTICAL
  }

  private final double initialT;
  private final double finalT;
  private final double dT;
  private final int w;
  private final int h;
  private final Direction direction;

  private final GraphicsDrawer graphicsDrawer;
  private final Framer framer;
  private final BufferedImage image;

  private int frameCount;
  private double lastT = Double.NEGATIVE_INFINITY;

  private static final Logger L = Logger.getLogger(FramesImageBuilder.class.getName());

  public FramesImageBuilder(double initialT, double finalT, double dT, int w, int h, Direction direction) {
    this.initialT = initialT;
    this.finalT = finalT;
    this.dT = dT;
    this.w = w;
    this.h = h;
    this.direction = direction;
    int frames = (int) Math.floor((finalT - initialT) / dT);
    int overallW = w;
    int overallH = h;
    if (direction.equals(Direction.HORIZONTAL)) {
      overallW = w * frames;
    } else {
      overallH = h * frames;
    }
    image = new BufferedImage(overallW, overallH, BufferedImage.TYPE_3BYTE_BGR);
    graphicsDrawer = new GraphicsDrawer();
    framer = new RobotFollower(frames, 1.5d, 100, RobotFollower.AggregateType.MAX);
    frameCount = 0;
  }

  public BufferedImage getImage() {
    return image;
  }

  @Override
  public void listen(double t, List<Snapshot> snapshots) {
    BoundingBox worldFrame = framer.getFrame(snapshots, (double) w / (double) h);
    if ((t < initialT) || (t >= finalT)) { //out of time window
      return;
    }
    if ((t - lastT) < dT) { //wait for next snapshot
      return;
    }
    lastT = t;
    BoundingBox imageFrame;
    if (direction.equals(Direction.HORIZONTAL)) {
      imageFrame = BoundingBox.build(
          Point2.build(w * frameCount, 0),
          Point2.build(w * (frameCount + 1), h)
      );
    } else {
      imageFrame = BoundingBox.build(
          Point2.build(0, h * frameCount),
          Point2.build(w, h * (frameCount + 1))
      );
    }
    L.fine(String.format("Rendering frame %d: %s to %s", frameCount, worldFrame, imageFrame));
    frameCount = frameCount + 1;
    Graphics2D g = image.createGraphics();
    graphicsDrawer.draw(t, snapshots, g, imageFrame, worldFrame, String.format("%d", frameCount));
    g.dispose();
  }

}
