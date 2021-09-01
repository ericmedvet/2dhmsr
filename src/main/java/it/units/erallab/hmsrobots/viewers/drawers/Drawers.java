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

import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.objects.Ground;
import it.units.erallab.hmsrobots.viewers.RobotFollower;

public class Drawers {
  private Drawers() {
  }

  private static Drawer world() {
    return Drawer.transform(
        new RobotFollower(60, 1.5d, 100, RobotFollower.AggregateType.MAX),
        Drawer.of(
            new PolyDrawer(PolyDrawer.TEXTURE_PAINT, RecursiveDrawer.Filter.matches(null, Ground.class, null), false),
            new VoxelDrawer(),
            new SensorReadingsSectorDrawer(),
            new LidarDrawer()
        )
    );
  }

  private static Drawer miniWorld() {
    return Drawer.transform(
        new RobotFollower(120, 5d, 100, RobotFollower.AggregateType.MAX),
        Drawer.of(
            new PolyDrawer(RecursiveDrawer.Filter.matches(null, Ground.class, null), false),
            new VoxelDrawer()
            //new BoundingBoxDrawer(RecursiveDrawer.Filter.matches(null, Robot.class, null), false)
        )
    );
  }

  public static Drawer basic(String string) {
    return Drawer.of(
        Drawer.clear(),
        world(),
        new InfoDrawer(string)
    );
  }

  public static Drawer basicWithMiniWorld(String string) {
    return Drawer.of(
        Drawer.clear(),
        world(),
        Drawer.clip(
            BoundingBox.build(0.5d, 0.01d, 0.95d, 0.2d),
            miniWorld()
        ),
        new InfoDrawer(string)
    );
  }

  public static Drawer basic() {
    return basic("");
  }

  public static Drawer basicWithMiniWorld() {
    return basicWithMiniWorld("");

  }


}
