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
package it.units.erallab.hmsrobots.viewers.drawers;

import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.sensors.Lidar;
import it.units.erallab.hmsrobots.core.snapshots.LidarReadings;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import it.units.erallab.hmsrobots.core.snapshots.VoxelPoly;
import it.units.erallab.hmsrobots.util.Configurable;
import it.units.erallab.hmsrobots.util.ConfigurableField;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;

public class LidarDrawer implements Drawer, Configurable<LidarDrawer> {

  @ConfigurableField
  private Color strokeColor = Color.RED;

  public static LidarDrawer build() {
    return new LidarDrawer();
  }

  @Override
  public void draw(List<Snapshot> lineage, Graphics2D g) {
    Snapshot last = lineage.get(lineage.size() - 1);
    if (!Drawer.match(last, LidarReadings.class, Lidar.class)) {
      return;
    }
    LidarReadings lidarReadings = (LidarReadings) last.getContent();
    Snapshot voxel = Drawer.lastMatching(lineage, VoxelPoly.class, Snapshottable.class);
    if (voxel == null) {
      //should not happen
      return;
    }
    VoxelPoly voxelPoly = (VoxelPoly) voxel.getContent();
    Point2 center = voxelPoly.center();
    double angle = lidarReadings.getVoxelAngle();
    double rayLength = lidarReadings.getDomains()[0].getMax();
    double[] rayDirections = lidarReadings.getRayDirections();
    double[] rayHits = lidarReadings.getReadings();
    for (int rayIdx = 0; rayIdx < rayDirections.length; rayIdx++) {
      double direction = rayDirections[rayIdx];
      // take into account rotation angle
      direction += angle;
      // Draw a ray from the given start point towards the given direction
      g.setColor(strokeColor);
      g.draw(GraphicsDrawer.toPath(
          center,
          Point2.build(
              center.x + rayLength * Math.cos(direction),
              center.y + rayLength * Math.sin(direction)
          )
      ));
      // draw only hits
      if (rayHits[rayIdx] < 1d) {
        double width = 0.5;
        double height = 0.5;
        // transform the center point to the upper left corner
        g.draw(new Ellipse2D.Double(
            (center.x + rayHits[rayIdx] * rayLength * Math.cos(direction)) - width / 2d,
            (center.y + rayHits[rayIdx] * rayLength * Math.sin(direction)) - height / 2d,
            width,
            height
        ));
      }
    }
  }

}
