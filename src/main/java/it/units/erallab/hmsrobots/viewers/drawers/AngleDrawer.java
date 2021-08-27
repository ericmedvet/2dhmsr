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
import it.units.erallab.hmsrobots.core.sensors.Angle;
import it.units.erallab.hmsrobots.core.snapshots.ScopedReadings;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import it.units.erallab.hmsrobots.core.snapshots.VoxelPoly;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;

import java.awt.*;
import java.util.List;


public class AngleDrawer implements Drawer {

  private final static Color COLOR = Color.BLACK;

  private final Color strokeColor;

  public AngleDrawer(Color strokeColor) {
    this.strokeColor = strokeColor;
  }

  public AngleDrawer() {
    this(COLOR);
  }


  @Override
  public void draw(double t, List<Snapshot> lineage, Graphics2D g) {
    Snapshot last = lineage.get(lineage.size() - 1);
    if (!Drawer.match(last, ScopedReadings.class, Angle.class)) {
      return;
    }
    double angle = ((ScopedReadings) last.getContent()).getReadings()[0];
    Snapshot voxel = Drawer.lastMatching(lineage, VoxelPoly.class, Snapshottable.class);
    if (voxel == null) {
      //should not happen
      return;
    }
    VoxelPoly voxelPoly = (VoxelPoly) voxel.getContent();
    Point2 center = voxelPoly.center();
    double radius = Math.sqrt(voxelPoly.area()) / 2d;
    g.setColor(strokeColor);
    g.draw(GraphicsDrawer.toPath(
        center,
        Point2.build(
            center.x + radius * Math.cos(angle),
            center.y + radius * Math.sin(angle)
        )
    ));
  }
}
