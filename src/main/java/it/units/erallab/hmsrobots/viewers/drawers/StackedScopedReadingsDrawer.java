/*
 * Copyright (c) "Eric Medvet" 2021.
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
import java.util.TreeMap;

/**
 * @author "Eric Medvet" on 2021/09/07 for 2dhmsr
 */
public class StackedScopedReadingsDrawer extends SubtreeDrawer {

  private final static Color MIN_COLOR = Color.GREEN;
  private final static Color MAX_COLOR = Color.RED;
  private final static Color LINE_COLOR = Color.BLUE;

  private final double windowT;
  private final Color minColor;
  private final Color maxColor;
  private final Color lineColor;

  private final SortedMap<Double, StackedScopedReadings> readings;

  public StackedScopedReadingsDrawer(Extractor extractor, double windowT, Color minColor, Color maxColor, Color lineColor) {
    super(extractor);
    this.windowT = windowT;
    this.minColor = minColor;
    this.maxColor = maxColor;
    this.lineColor = lineColor;
    readings = new TreeMap<>();
  }

  public StackedScopedReadingsDrawer(Extractor extractor, double windowT) {
    this(extractor, windowT, MIN_COLOR, MAX_COLOR, LINE_COLOR);
  }

  @Override
  protected void innerDraw(double t, Snapshot snapshot, Graphics2D g) {
    if (!(snapshot.getContent() instanceof StackedScopedReadings)) {
      return;
    }
    StackedScopedReadings currentReading = (StackedScopedReadings) snapshot.getContent();
    //update memory
    readings.put(t, currentReading);
    while (readings.firstKey() < (t - windowT)) {
      readings.remove(readings.firstKey());
    }
    //plot
    double clipX = g.getClip().getBounds2D().getX();
    double clipY = g.getClip().getBounds2D().getY();
    double clipW = g.getClip().getBounds2D().getWidth();
    double clipH = g.getClip().getBounds2D().getHeight();
    double deltaT = readings.size() == 1 ? (1d / 60d) : ((readings.lastKey() - readings.firstKey()) / (readings.size() - 1));
    double n = Arrays.stream(currentReading.getScopedReadings()).mapToInt(r -> r.getReadings().length).sum();
    double cellW = clipW * deltaT / windowT;
    double cellH = clipH / n;
    for (Map.Entry<Double, StackedScopedReadings> entry : readings.entrySet()) {
      double x = 1d - (t - entry.getKey()) / windowT;
      double c = 0;
      for (ScopedReadings scopedReadings : entry.getValue().getScopedReadings()) {
        for (int i = 0; i < scopedReadings.getReadings().length; i++) {
          double v = (scopedReadings.getReadings()[i] - scopedReadings.getDomains()[i].getMin()) / (scopedReadings.getDomains()[i].getMax() - scopedReadings.getDomains()[i].getMin());
          double y = c / (n + currentReading.getScopedReadings().length - 1);
          c = c + 1;
          g.setColor(DrawingUtils.linear(minColor, maxColor, 0f, 1f, (float) v));
          g.fill(new Rectangle2D.Double(
              clipX + x * clipW - cellW,
              clipY + y * clipH,
              cellW,
              cellH
          ));
        }
        c = c + 1;
      }
    }
  }
}
