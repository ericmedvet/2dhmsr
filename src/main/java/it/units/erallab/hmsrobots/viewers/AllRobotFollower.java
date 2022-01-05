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
import it.units.erallab.hmsrobots.core.geometry.Shape;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.viewers.drawers.SubtreeDrawer;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class AllRobotFollower implements Framer {

  private final double sizeRelativeMargin;
  private final double windowT;

  private final SortedMap<Double, BoundingBox> boundingBoxes;

  public AllRobotFollower(double sizeRelativeMargin, double windowT) {
    this.sizeRelativeMargin = sizeRelativeMargin;
    this.windowT = windowT;
    boundingBoxes = new TreeMap<>();
  }

  @Override
  public BoundingBox getFrame(double t, Snapshot snapshot, double ratio) {
    //get current bounding box
    SubtreeDrawer.Extractor.matches(Shape.class, Robot.class, null).extract(snapshot).stream()
        .map(s -> ((Shape) s.getContent()).boundingBox())
        .reduce(BoundingBox::largest)
        .ifPresent(boundingBox -> boundingBoxes.put(t, boundingBox));
    //clean
    if (boundingBoxes.firstKey() < (t - windowT)) {
      boundingBoxes.remove(boundingBoxes.firstKey());
    }
    //aggregate
    BoundingBox aggregated = boundingBoxes.values().stream()
        .reduce(BoundingBox::largest)
        .orElse(BoundingBox.of(0, 0, 1, 1));
    //enlarge
    double cx = aggregated.center().x();
    double cy = aggregated.center().y();
    double w = aggregated.width();
    double h = aggregated.height();
    BoundingBox enlarged = BoundingBox.of(
        cx - w / 2d * sizeRelativeMargin,
        cy - h / 2d * sizeRelativeMargin,
        cx + w / 2d * sizeRelativeMargin,
        cy + h / 2d * sizeRelativeMargin
    );
    //adjust
    BoundingBox adjusted = enlarged;
    double fRatio = enlarged.width() / enlarged.height();
    if (fRatio > ratio) {
      //enlarge h
      adjusted = BoundingBox.of(
          enlarged.min().x(),
          cy - h / 2d * sizeRelativeMargin * fRatio / ratio,
          enlarged.max().x(),
          cy + h / 2d * sizeRelativeMargin * fRatio / ratio
      );
    } else if (fRatio < ratio) {
      //enlarge w
      adjusted = BoundingBox.of(
          cx - w / 2d * sizeRelativeMargin * ratio / fRatio,
          enlarged.min().y(),
          cx + w / 2d * sizeRelativeMargin * ratio / fRatio,
          enlarged.max().y()
      );
    }
    return adjusted;
  }

}
