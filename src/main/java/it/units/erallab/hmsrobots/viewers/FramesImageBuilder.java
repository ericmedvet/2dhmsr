/*
 * Copyright (C) 2022 Eric Medvet <eric.medvet@gmail.com> (as Eric Medvet <eric.medvet@gmail.com>)
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
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.SnapshotListener;
import it.units.erallab.hmsrobots.viewers.drawers.Drawer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class FramesImageBuilder implements SnapshotListener {

  private static final Logger L = Logger.getLogger(FramesImageBuilder.class.getName());
  private final double initialT;
  private final double finalT;
  private final double dT;
  private final Direction direction;

  private final Drawer drawer;
  private final BufferedImage image;

  private final int nOfFrames;

  private int frameCount;
  private double lastT = Double.NEGATIVE_INFINITY;

  public FramesImageBuilder(
      double initialT,
      double finalT,
      double dT,
      int w,
      int h,
      Direction direction,
      Drawer drawer
  ) {
    this.initialT = initialT;
    this.finalT = finalT;
    this.dT = dT;
    this.direction = direction;
    this.drawer = drawer;
    nOfFrames = (int) Math.ceil((finalT - initialT) / dT);
    int overallW = w;
    int overallH = h;
    if (direction.equals(Direction.HORIZONTAL)) {
      overallW = w * nOfFrames;
    } else {
      overallH = h * nOfFrames;
    }
    image = new BufferedImage(overallW, overallH, BufferedImage.TYPE_3BYTE_BGR);
    frameCount = 0;
  }

  public enum Direction {
    HORIZONTAL, VERTICAL
  }

  public BufferedImage getImage() {
    return image;
  }

  @Override
  public void listen(double t, Snapshot snapshot) {
    BoundingBox imageFrame;
    if ((t < initialT) || (t >= finalT) || ((t - lastT) < dT)) { //out of time window
      imageFrame = BoundingBox.of(-10, -10, -1, -1);
    } else {
      if (direction.equals(Direction.HORIZONTAL)) {
        imageFrame = BoundingBox.of(
            (double) frameCount / (double) nOfFrames,
            0,
            (double) (frameCount + 1) / (double) nOfFrames,
            1d
        );
      } else {
        imageFrame = BoundingBox.of(
            0,
            (double) frameCount / (double) nOfFrames,
            1,
            (double) (frameCount + 1) / (double) nOfFrames
        );
      }
      L.info(String.format("Rendering frame %d on %s", frameCount, imageFrame));
      frameCount = frameCount + 1;
      lastT = t;
    }
    Graphics2D g = image.createGraphics();
    g.setClip(0, 0, image.getWidth(), image.getHeight());
    Drawer.clip(imageFrame, drawer).draw(t, snapshot, g);
    g.dispose();
  }

}
