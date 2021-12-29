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
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.VoxelPoly;
import it.units.erallab.hmsrobots.viewers.DrawingUtils;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author "Eric Medvet" on 2021/08/27 for 2dhmsr
 */
public class InfoDrawer implements Drawer {

  private final String string;
  private final Set<RobotInfo> robotInfos;
  private final double windowT;
  private final List<SortedMap<Double, Point2>> centerPositions;

  public InfoDrawer(String string, Set<RobotInfo> robotInfos, double windowT) {
    this.string = string;
    this.robotInfos = robotInfos;
    this.windowT = windowT;
    centerPositions = new ArrayList<>();
  }

  public InfoDrawer(String string) {
    this(
        string,
        EnumSet.allOf(RobotInfo.class),
        5d
    );
  }

  public InfoDrawer() {
    this("");
  }

  public enum RobotInfo {CENTER_POSITION, CENTER_VELOCITY}

  @Override
  public void draw(double t, Snapshot snapshot, Graphics2D g) {
    //prepare string
    StringBuilder sb = new StringBuilder();
    if (!string.isEmpty()) {
      sb.append(string);
      sb.append("\n");
    }
    sb.append(String.format("t=%4.1f%n", t));
    //collect robots info
    if (!robotInfos.isEmpty()) {
      //get centers
      List<Point2> currentCenterPositions = SubtreeDrawer.Extractor.matches(null, Robot.class, null)
          .extract(snapshot)
          .stream()
          .map(s -> Point2.average(
              s.getChildren().stream()
                  .filter(c -> Voxel.class.isAssignableFrom(c.getSnapshottableClass()))
                  .map(c -> ((VoxelPoly) c.getContent()).center())
                  .toArray(Point2[]::new))
          )
          .toList();
      //add to maps
      for (int i = 0; i < currentCenterPositions.size(); i++) {
        if (centerPositions.size() <= i) {
          centerPositions.add(new TreeMap<>());
        }
        centerPositions.get(i).put(t, currentCenterPositions.get(i));
      }
      //clean maps
      centerPositions.forEach(m -> {
        while (m.firstKey() < (t - windowT)) {
          m.remove(m.firstKey());
        }
      });
      //print
      for (int i = 0; i < centerPositions.size(); i++) {
        Point2 currentPos = currentCenterPositions.get(i);
        Point2 oldestPos = centerPositions.get(i).get(centerPositions.get(i).firstKey());
        sb.append(String.format("robot %d:", i));
        if (robotInfos.contains(RobotInfo.CENTER_POSITION)) {
          sb.append(String.format(
              " pos=(%5.1f,%5.1f)",
              currentPos.x(),
              currentPos.y()
          ));
        }
        if (robotInfos.contains(RobotInfo.CENTER_VELOCITY)) {
          sb.append(String.format(
              " vel[%.0f]=(%+5.1f,%+5.1f)%n",
              windowT,
              (currentPos.x() - oldestPos.x()) / windowT,
              (currentPos.y() - oldestPos.y()) / windowT
          ));
        }
      }
    }
    //write
    g.setColor(DrawingUtils.Colors.TEXT);
    int relY = g.getClipBounds().y + 1;
    for (String line : sb.toString().split(String.format("%n"))) {
      g.drawString(line, g.getClipBounds().x + 1, relY + g.getFontMetrics().getMaxAscent());
      relY = relY + g.getFontMetrics().getMaxAscent() + 1;
    }
  }
}
