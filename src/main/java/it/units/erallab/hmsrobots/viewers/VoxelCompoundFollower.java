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

import com.google.common.collect.EvictingQueue;
import it.units.erallab.hmsrobots.objects.VoxelCompound;
import it.units.erallab.hmsrobots.objects.immutable.Component;
import it.units.erallab.hmsrobots.objects.immutable.Compound;
import it.units.erallab.hmsrobots.objects.immutable.Point2;
import it.units.erallab.hmsrobots.objects.immutable.Snapshot;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class VoxelCompoundFollower implements Framer {

  public static enum AggregateType {
    MAX, AVG
  };

  private final double sizeRelativeMargin;
  private final int compounds;
  private final AggregateType aggregateType;

  private final EvictingQueue<Frame> enclosingFrames;

  public VoxelCompoundFollower(int windowSize, double sizeRelativeMargin, int compounds, AggregateType aggregateType) {
    this.sizeRelativeMargin = sizeRelativeMargin;
    this.compounds = compounds;
    this.aggregateType = aggregateType;
    enclosingFrames = EvictingQueue.create(windowSize);
  }

  @Override
  public Frame getFrame(Snapshot snapshot, double ratio) {
    //get one frame per voxel compound
    List<Frame> frames = snapshot.getCompounds().stream()
            .filter(c -> c.getObjectClass().equals(VoxelCompound.class))
            .limit(compounds)
            .map((Compound c) -> {
              double x1 = Double.POSITIVE_INFINITY;
              double x2 = Double.NEGATIVE_INFINITY;
              double y1 = Double.POSITIVE_INFINITY;
              double y2 = Double.NEGATIVE_INFINITY;
              for (Component component : c.getComponents()) {
                for (Point2 p : component.getPoly().getVertexes()) {
                  x1 = Math.min(x1, p.x);
                  x2 = Math.max(x2, p.x);
                  y1 = Math.min(y1, p.y);
                  y2 = Math.max(y2, p.y);
                }
              }
              return new Frame(x1, x2, y1, y2);
            }).collect(Collectors.toList());
    //get enclosing frame
    Frame enclosingFrame = frames.stream().reduce((f1, f2) -> largest(f1, f2)).get();
    //add to queue
    enclosingFrames.offer(enclosingFrame);
    //compute aggregate frame
    Frame aggregatedFrame = enclosingFrames.stream().reduce((f1, f2) -> {
      if (aggregateType.equals(AggregateType.AVG)) {
        return average(f1, f2);
      }
      return largest(f1, f2);
    }).get();
    //enlarge
    double cx = (aggregatedFrame.getX1() + aggregatedFrame.getX2()) / 2d;
    double cy = (aggregatedFrame.getY1() + aggregatedFrame.getY2()) / 2d;
    double w = aggregatedFrame.getX2() - aggregatedFrame.getX1();
    double h = aggregatedFrame.getY2() - aggregatedFrame.getY1();
    Frame enlargedFrame = new Frame(
            cx - w / 2d * sizeRelativeMargin,
            cx + w / 2d * sizeRelativeMargin,
            cy - h / 2d * sizeRelativeMargin,
            cy + h / 2d * sizeRelativeMargin
    );
    //adjust
    Frame adjustedFrame = enlargedFrame;
    double fRatio = (enlargedFrame.getX2()-enlargedFrame.getX1())/(enlargedFrame.getY2()-enlargedFrame.getY1());
    if (fRatio>ratio) {
      //enlarge h
      adjustedFrame = new Frame(
              enlargedFrame.getX1(),
              enlargedFrame.getX2(),
              cy - h / 2d * sizeRelativeMargin*fRatio/ratio,
              cy + h / 2d * sizeRelativeMargin*fRatio/ratio
      );
    } else if (fRatio<ratio) {
      //enlarge w
      adjustedFrame = new Frame(
              cx - w / 2d * sizeRelativeMargin*ratio/fRatio,
              cx + w / 2d * sizeRelativeMargin*ratio/fRatio,
              enlargedFrame.getY1(),
              enlargedFrame.getY2()
      );
    }
    return adjustedFrame;
  }

  private static Frame largest(Frame f1, Frame f2) {
    return new Frame(
            Math.min(f1.getX1(), f2.getX1()),
            Math.max(f1.getX2(), f2.getX2()),
            Math.min(f1.getY1(), f2.getY1()),
            Math.max(f1.getY2(), f2.getY2())
    );
  }

  private static Frame average(Frame f1, Frame f2) {
    return new Frame(
            (f1.getX1() + f2.getX1()) / 2d,
            (f1.getX2() + f2.getX2()) / 2d,
            (f1.getY1() + f2.getY1()) / 2d,
            (f1.getY2() + f2.getY2()) / 2d
    );
  }
  
  private static double ratio(Frame f) {
    return (f.getX2()-f.getX1())/(f.getY2()-f.getY1());
  }

}
