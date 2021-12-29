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

import it.units.erallab.hmsrobots.core.snapshots.ScopedReadings;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.StackedScopedReadings;
import it.units.erallab.hmsrobots.viewers.DrawingUtils;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedMap;

/**
 * @author "Eric Medvet" on 2021/09/07 for 2dhmsr
 */
public class StackedScopedReadingsDrawer extends MemoryDrawer<StackedScopedReadings> {

  private final Color minColor;
  private final Color maxColor;

  public StackedScopedReadingsDrawer(Extractor extractor, double windowT, Color minColor, Color maxColor) {
    super(extractor, s -> (StackedScopedReadings) s.getContent(), windowT);
    this.minColor = minColor;
    this.maxColor = maxColor;
  }

  public StackedScopedReadingsDrawer(Extractor extractor, double windowT) {
    this(extractor, windowT, DrawingUtils.Colors.DATA_POSITIVE, DrawingUtils.Colors.DATA_NEGATIVE);
  }

  @Override
  protected void innerDraw(
      double t,
      Snapshot snapshot,
      SortedMap<Double, StackedScopedReadings> memory,
      Graphics2D g
  ) { //TODO rewrite like MLPState
    StackedScopedReadings currentReading = memory.get(memory.lastKey());
    //plot
    double clipX = g.getClip().getBounds2D().getX();
    double clipY = g.getClip().getBounds2D().getY();
    double clipW = g.getClip().getBounds2D().getWidth();
    double clipH = g.getClip().getBounds2D().getHeight();
    double deltaT = memory.size() == 1 ? (1d / 60d) : ((memory.lastKey() - memory.firstKey()) / (memory.size() - 1));
    double n = Arrays.stream(currentReading.getScopedReadings()).mapToInt(r -> r.getReadings().length).sum();
    double cellW = clipW * deltaT / windowT;
    double cellH = clipH / n;
    for (Map.Entry<Double, StackedScopedReadings> entry : memory.entrySet()) {
      double x = 1d - (t - entry.getKey()) / windowT;
      double c = 0;
      for (ScopedReadings scopedReadings : entry.getValue().getScopedReadings()) {
        for (int i = 0; i < scopedReadings.getReadings().length; i++) {
          double v = scopedReadings.getDomains()[i].normalize(scopedReadings.getReadings()[i]);
          double y = c / (n + currentReading.getScopedReadings().length - 1);
          c = c + 1;
          g.setColor(DrawingUtils.linear(minColor, maxColor, 0f, 1f, (float) v));
          g.fill(new Rectangle2D.Double(
              clipX + x * clipW - 2 * cellW, // 2* is for avoing gaps in the plot
              clipY + y * clipH,
              2 * cellW, //2* is for avoing gaps in the plot
              cellH
          ));
        }
        c = c + 1;
      }
    }
  }
}
