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
package it.units.erallab.hmsrobots.viewers.drawers;

import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.core.snapshots.LidarReadings;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.VoxelPoly;
import it.units.erallab.hmsrobots.viewers.DrawingUtils;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;

public class LidarDrawer extends SubtreeDrawer {

  private final static Color COLOR = Color.RED;
  private final static double CIRCLE_SIZE = 0.5d;
  private final static Extractor LIDAR_EXTRACTOR = Extractor.matches(LidarReadings.class, null, null);

  private final Color strokeColor;

  public LidarDrawer(Color strokeColor) {
    super(Extractor.matches(null, Voxel.class, null));
    this.strokeColor = strokeColor;
  }

  public LidarDrawer() {
    this(COLOR);
  }


  @Override
  protected void innerDraw(double t, Snapshot snapshot, Graphics2D g) {
    List<Snapshot> lidarSnapshots = LIDAR_EXTRACTOR.extract(snapshot);
    if (lidarSnapshots.isEmpty()) {
      return;
    }
    VoxelPoly voxelPoly = (VoxelPoly) snapshot.getContent();
    Point2 center = voxelPoly.center();
    for (Snapshot lidarSnapshot : lidarSnapshots) {
      LidarReadings lidarReadings = (LidarReadings) lidarSnapshot.getContent();
      double angle = lidarReadings.getVoxelAngle();
      double rayLength = lidarReadings.getDomains()[0].max();
      double[] rayDirections = lidarReadings.getRayDirections();
      double[] rayHits = lidarReadings.getReadings();
      for (int rayIdx = 0; rayIdx < rayDirections.length; rayIdx++) {
        double direction = rayDirections[rayIdx];
        // take into account rotation angle
        direction += angle;
        // Draw a ray from the given start point towards the given direction
        g.setColor(strokeColor);
        g.draw(DrawingUtils.toPath(
            center,
            Point2.of(
                center.x() + rayLength * Math.cos(direction),
                center.y() + rayLength * Math.sin(direction)
            )
        ));
        // draw only hits
        if (rayHits[rayIdx] < rayLength) {
          g.draw(new Ellipse2D.Double(
              center.x() + rayHits[rayIdx] * Math.cos(direction) - CIRCLE_SIZE / 2d,
              center.y() + rayHits[rayIdx] * Math.sin(direction) - CIRCLE_SIZE / 2d,
              CIRCLE_SIZE,
              CIRCLE_SIZE
          ));
        }
      }
    }
  }

}
