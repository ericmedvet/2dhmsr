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

package it.units.erallab.hmsrobots.viewers.drawers;

import it.units.erallab.hmsrobots.core.geometry.Poly;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.snapshots.RobotShape;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.viewers.DrawingUtils;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;

public class GhostRobotDrawer extends MemoryDrawer<RobotShape> {

  private final static Color STROKE_COLOR = Color.BLUE;
  private final static Color FILL_COLOR = DrawingUtils.alphaed(STROKE_COLOR, 0.5f);
  private final static float FIRST_ALPHA = 0.05f;
  private final static float LAST_ALPHA = 0.25f;
  private final double deltaT;
  private final boolean moving;

  public GhostRobotDrawer(double windowT, double deltaT, int index, boolean moving) {
    super(
        Extractor.matches(RobotShape.class, Robot.class, index),
        s -> (RobotShape) s.getContent(),
        windowT
    );
    this.deltaT = deltaT;
    this.moving = moving;
  }

  @Override
  protected void innerDraw(double t, Snapshot snapshot, SortedMap<Double, RobotShape> memory, Graphics2D g) {
    double nextT = memory.firstKey();
    if (!moving) {
      nextT = Math.ceil(memory.firstKey() / deltaT) * deltaT;
    }
    for (Map.Entry<Double, RobotShape> entry : memory.entrySet()) {
      double currentT = entry.getKey();
      if (currentT >= nextT) {
        nextT = currentT + deltaT;
        //compute alpha
        float alpha = (float) ((currentT - t + windowT) / windowT) * (LAST_ALPHA - FIRST_ALPHA) + FIRST_ALPHA;
        //draw
        entry.getValue().getPolies().values().stream().filter(Objects::nonNull).forEach(p -> {
          Path2D path = DrawingUtils.toPath(Poly.of(p.vertexes()), true);
          g.setColor(DrawingUtils.alphaed(STROKE_COLOR, alpha));
          g.draw(path);
          g.setColor(DrawingUtils.alphaed(FILL_COLOR, alpha));
          g.fill(path);
        });
      }
    }
  }
}