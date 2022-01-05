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

import it.units.erallab.hmsrobots.behavior.BehaviorUtils;
import it.units.erallab.hmsrobots.behavior.Footprint;
import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.viewers.DrawingUtils;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;
import java.util.SortedMap;

/**
 * @author "Eric Medvet" on 2021/09/22 for 2dhmsr
 */
public class FootprintDrawer extends MemoryDrawer<Footprint> {

  private final int n;
  private final Color touchColor;
  private final Color axesColor;
  private final Color textColor;

  public FootprintDrawer(
      Extractor extractor,
      double windowT,
      int n,
      Color touchColor,
      Color axesColor,
      Color textColor
  ) {
    super(
        extractor,
        BehaviorUtils.voxelPolyGrid()
            .andThen(g -> BehaviorUtils.computeFootprint(g.values()
                .stream()
                .filter(Objects::nonNull)
                .toList(), n)),
        windowT
    );
    this.n = n;
    this.touchColor = touchColor;
    this.axesColor = axesColor;
    this.textColor = textColor;
  }

  public FootprintDrawer(Extractor extractor, double windowT, int n) {
    this(extractor, windowT, n, DrawingUtils.Colors.DATA, DrawingUtils.Colors.AXES, DrawingUtils.Colors.TEXT);
  }

  @Override
  protected void innerDraw(double t, Snapshot snapshot, SortedMap<Double, Footprint> memory, Graphics2D g) {
    //prepare clips
    double textH = g.getFontMetrics().getMaxAscent();
    BoundingBox oBB = BoundingBox.of(
        g.getClip().getBounds2D().getX(),
        g.getClip().getBounds2D().getY(),
        g.getClip().getBounds2D().getMaxX(),
        g.getClip().getBounds2D().getMaxY()
    );
    BoundingBox pBB = BoundingBox.of(
        oBB.min().x(),
        oBB.min().y() + textH,
        oBB.max().x(),
        oBB.max().y() - 3 * textH
    );
    //draw data
    g.setColor(touchColor);
    double[] ts = memory.keySet().stream().mapToDouble(v -> v).toArray();
    Footprint[] footprints = memory.values().toArray(Footprint[]::new);
    double h = pBB.height() / (double) n;
    for (int i = 1; i < ts.length; i++) {
      double x1 = pBB.max().x() - (ts[ts.length - 1] - ts[i - 1]) / windowT * pBB.width();
      double x2 = pBB.max().x() - (ts[ts.length - 1] - ts[i]) / windowT * pBB.width();
      boolean[] mask = footprints[i].getMask();
      for (int j = 0; j < n; j++) {
        if (mask[j]) {
          double y1 = pBB.min().y() + (double) j / (double) n * pBB.height();
          g.fill(new Rectangle2D.Double(x1, y1, x2 - x1, h));
        }
      }
    }
    //draw x-axis
    g.setColor(axesColor);
    g.draw(new Line2D.Double(pBB.min().x(), pBB.max().y(), pBB.max().x(), pBB.max().y()));
    double maxT = memory.lastKey();
    for (double tickT = Math.ceil(maxT - windowT); tickT < maxT; tickT++) {
      g.setColor(axesColor);
      double x = (tickT - maxT + windowT) / windowT * (pBB.max().x() - pBB.min().x()) + pBB.min().x();
      g.draw(new Line2D.Double(x, pBB.max().y(), x, pBB.max().y() + textH));
      g.setColor(textColor);
      String s = String.format("%.0f", tickT);
      g.drawString(s, (float) (x - g.getFontMetrics().stringWidth(s) / 2f), (float) (pBB.max().y() + 2 * textH));
    }

  }
}
