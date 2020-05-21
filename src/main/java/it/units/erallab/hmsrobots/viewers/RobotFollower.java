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

import com.google.common.collect.EvictingQueue;
import it.units.erallab.hmsrobots.core.objects.immutable.Robot;
import it.units.erallab.hmsrobots.core.objects.immutable.Snapshot;
import it.units.erallab.hmsrobots.core.objects.immutable.Voxel;
import it.units.erallab.hmsrobots.util.BoundingBox;
import it.units.erallab.hmsrobots.util.Point2;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class RobotFollower implements Framer {

  public enum AggregateType {
    MAX, AVG
  }

  private double sizeRelativeMargin;
  private final int compounds;
  private final AggregateType aggregateType;

  private final EvictingQueue<BoundingBox> enclosingFrames;

  public RobotFollower(int windowSize, double sizeRelativeMargin, int compounds, AggregateType aggregateType) {
    this.sizeRelativeMargin = sizeRelativeMargin;
    this.compounds = compounds;
    this.aggregateType = aggregateType;
    enclosingFrames = EvictingQueue.create(windowSize);
  }

  @Override
  public BoundingBox getFrame(Snapshot snapshot, double ratio) {
    //get enclosing bounding box
    BoundingBox enclosing = snapshot.getObjects().stream()
        .filter(o -> o instanceof Robot)
        .limit(compounds)
        .map(o -> o.getChildren().stream()
            .filter(c -> c instanceof Voxel)
            .map(c -> ((Voxel) c).getShape().boundingBox())
            .reduce((b1, b2) -> BoundingBox.largest(b1, b2))
            .get())
        .reduce((b1, b2) -> BoundingBox.largest(b1, b2))
        .get();
    //add to queue
    enclosingFrames.offer(enclosing);
    //compute aggregate frame
    BoundingBox aggregated = enclosingFrames.stream().reduce((b1, b2) -> {
      if (aggregateType.equals(AggregateType.AVG)) {
        return average(b1, b2);
      }
      return BoundingBox.largest(b1, b2);
    }).get();
    //enlarge
    double cx = (aggregated.min.x + aggregated.max.x) / 2d;
    double cy = (aggregated.min.y + aggregated.max.y) / 2d;
    double w = aggregated.max.x - aggregated.min.x;
    double h = aggregated.max.y - aggregated.min.y;
    BoundingBox enlarged = BoundingBox.build(
        Point2.build(
            cx - w / 2d * sizeRelativeMargin,
            cy - h / 2d * sizeRelativeMargin
        ),
        Point2.build(
            cx + w / 2d * sizeRelativeMargin,
            cy + h / 2d * sizeRelativeMargin
        )
    );
    //adjust
    BoundingBox adjusted = enlarged;
    double fRatio = (enlarged.max.x - enlarged.min.x) / (enlarged.max.y - enlarged.min.y);
    if (fRatio > ratio) {
      //enlarge h
      adjusted = BoundingBox.build(
          Point2.build(
              enlarged.min.x,
              cy - h / 2d * sizeRelativeMargin * fRatio / ratio
          ),
          Point2.build(
              enlarged.max.x,
              cy + h / 2d * sizeRelativeMargin * fRatio / ratio
          )
      );
    } else if (fRatio < ratio) {
      //enlarge w
      adjusted = BoundingBox.build(
          Point2.build(
              cx - w / 2d * sizeRelativeMargin * ratio / fRatio,
              enlarged.min.y
          ),
          Point2.build(
              cx + w / 2d * sizeRelativeMargin * ratio / fRatio,
              enlarged.max.y
          )
      );
    }
    return adjusted;
  }

  private static BoundingBox average(BoundingBox b1, BoundingBox b2) {
    return BoundingBox.build(
        Point2.average(b1.min, b2.min),
        Point2.average(b1.max, b2.max)
    );
  }

  public void setSizeRelativeMargin(double sizeRelativeMargin) {
    this.sizeRelativeMargin = sizeRelativeMargin;
  }

  public double getSizeRelativeMargin() {
    return sizeRelativeMargin;
  }

}
