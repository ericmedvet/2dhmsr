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

import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.viewers.DrawingUtils;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.function.Function;

/**
 * @author "Eric Medvet" on 2021/09/17 for 2dhmsr
 */
public class SignalDrawer extends MemoryDrawer<Double> {

  private final Color signalColor;
  private final Color axesColor;
  private final Color textColor;

  public SignalDrawer(
      Extractor extractor,
      Function<Snapshot, Double> function,
      double windowT,
      Color signalColor,
      Color axesColor,
      Color textColor
  ) {
    super(extractor, function, windowT);
    this.signalColor = signalColor;
    this.axesColor = axesColor;
    this.textColor = textColor;
  }

  public SignalDrawer(Extractor extractor, Function<Snapshot, Double> function, double windowT) {
    this(extractor, function, windowT, DrawingUtils.Colors.DATA, DrawingUtils.Colors.AXES, DrawingUtils.Colors.TEXT);
  }

  @Override
  protected void innerDraw(double t, Snapshot snapshot, SortedMap<Double, Double> memory, Graphics2D g) {
    //prepare clips
    double textH = g.getFontMetrics().getMaxAscent();
    double textW = g.getFontMetrics().charWidth('m');
    BoundingBox oBB = BoundingBox.of(
        g.getClip().getBounds2D().getX(),
        g.getClip().getBounds2D().getY(),
        g.getClip().getBounds2D().getMaxX(),
        g.getClip().getBounds2D().getMaxY()
    );
    BoundingBox pBB = BoundingBox.of(
        oBB.min().x() + 5 * textW,
        oBB.min().y() + textH,
        oBB.max().x() - textW,
        oBB.max().y() - 3 * textH
    );
    //draw data
    g.setColor(signalColor);
    double[] ts = memory.keySet().stream().mapToDouble(v -> v).toArray();
    double[] vs = memory.values().stream().mapToDouble(v -> v).toArray();
    double minV = Arrays.stream(vs).min().orElse(0d);
    double maxV = Arrays.stream(vs).max().orElse(0d);
    for (int i = 1; i < ts.length; i++) {
      double x1 = pBB.max().x() - (ts[ts.length - 1] - ts[i - 1]) / windowT * pBB.width();
      double x2 = pBB.max().x() - (ts[ts.length - 1] - ts[i]) / windowT * pBB.width();
      double y1 = pBB.max().y() - (vs[i - 1] - minV) / (maxV - minV) * pBB.height();
      double y2 = pBB.max().y() - (vs[i] - minV) / (maxV - minV) * pBB.height();
      g.draw(new Line2D.Double(x1, y1, x2, y2));
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
    //draw 0-line
    if (maxV > 0 && minV < 0) {
      g.setColor(axesColor);
      double y = pBB.max().y() - (0 - minV) / (maxV - minV) * pBB.height();
      g.draw(new Line2D.Double(pBB.min().x(), y, pBB.max().x(), y));
    }
    //draw y-axis
    g.setColor(axesColor);
    g.draw(new Line2D.Double(pBB.min().x(), pBB.max().y(), pBB.min().x(), pBB.min().y()));
    g.draw(new Line2D.Double(pBB.min().x() - textW, pBB.max().y(), pBB.min().x(), pBB.max().y()));
    g.draw(new Line2D.Double(pBB.min().x() - textW, pBB.min().y(), pBB.min().x(), pBB.min().y()));
    g.setColor(textColor);
    String s = String.format("%.1f", maxV);
    g.drawString(
        s,
        (float) (pBB.min().x() - 2d * textW - g.getFontMetrics().stringWidth(s)),
        (float) (pBB.min().y() + textH / 2d)
    );
    s = String.format("%.1f", minV);
    g.drawString(
        s,
        (float) (pBB.min().x() - 2d * textW - g.getFontMetrics().stringWidth(s)),
        (float) (pBB.max().y() + textH / 2d)
    );
  }
}
