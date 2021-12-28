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
import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.util.DoubleRange;
import it.units.erallab.hmsrobots.viewers.DrawingUtils;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.SortedMap;
import java.util.function.Function;

/**
 * @author "Eric Medvet" on 2021/09/17 for 2dhmsr
 */
public class SpectrumDrawer extends MemoryDrawer<Double> {

  private final double minF;
  private final double maxF;
  private final int nBins;

  private final Color barFillColor;
  private final Color barLineColor;
  private final Color axesColor;
  private final Color textColor;

  public SpectrumDrawer(
      Extractor extractor,
      Function<Snapshot, Double> function,
      double windowT,
      double minF,
      double maxF,
      int nBins,
      Color barFillColor,
      Color barLineColor,
      Color axesColor,
      Color textColor
  ) {
    super(extractor, function, windowT);
    this.minF = minF;
    this.maxF = maxF;
    this.nBins = nBins;
    this.barFillColor = barFillColor;
    this.barLineColor = barLineColor;
    this.axesColor = axesColor;
    this.textColor = textColor;
  }

  public SpectrumDrawer(
      Extractor extractor,
      Function<Snapshot, Double> function,
      double windowT,
      double minF,
      double maxF,
      int nBins
  ) {
    this(
        extractor,
        function,
        windowT,
        minF,
        maxF,
        nBins,
        DrawingUtils.alphaed(DrawingUtils.Colors.DATA, .5f),
        DrawingUtils.Colors.DATA,
        DrawingUtils.Colors.AXES,
        DrawingUtils.Colors.TEXT
    );
  }

  @Override
  protected void innerDraw(double t, Snapshot snapshot, SortedMap<Double, Double> memory, Graphics2D g) {
    //compute spectrum
    SortedMap<DoubleRange, Double> spectrum = BehaviorUtils.computeQuantizedSpectrum(memory, minF, maxF, nBins);
    double maxValue = spectrum.values().stream().mapToDouble(d -> d).max().orElse(0d);
    DoubleRange[] domains = spectrum.keySet().toArray(DoubleRange[]::new);
    double[] values = spectrum.values().stream().mapToDouble(d -> d).toArray();
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
    double binW = pBB.width() / (double) nBins;
    //draw bars
    for (int i = 0; i < nBins; i++) {
      double minX = pBB.min().x() + (double) i * binW;
      double barH = pBB.height() * values[i] / maxValue;
      Shape shape = new Rectangle2D.Double(minX, pBB.min().y() + pBB.height() - barH, binW, barH);
      g.setColor(barFillColor);
      g.fill(shape);
      g.setColor(barLineColor);
      g.draw(shape);
    }
    //draw x-axis
    g.setColor(axesColor);
    g.draw(new Line2D.Double(pBB.min().x(), pBB.max().y(), pBB.max().x(), pBB.max().y()));
    for (int i = 0; i <= nBins; i++) {
      double x = pBB.min().x() + (double) i * binW;
      g.setColor(axesColor);
      g.draw(new Line2D.Double(x, pBB.max().y(), x, pBB.max().y() + textH));
      g.setColor(textColor);
      String s = String.format("%.1f", (i < nBins) ? domains[i].min() : domains[i - 1].max());
      g.drawString(s, (float) x - g.getFontMetrics().stringWidth(s) / 2f, (float) (pBB.max().y() + 2 * textH));
    }
    //draw y-axis
    g.setColor(axesColor);
    g.draw(new Line2D.Double(pBB.min().x(), pBB.max().y(), pBB.min().x(), pBB.min().y()));
    g.draw(new Line2D.Double(pBB.min().x() - textW, pBB.max().y(), pBB.min().x(), pBB.max().y()));
    g.draw(new Line2D.Double(pBB.min().x() - textW, pBB.min().y(), pBB.min().x(), pBB.min().y()));
    g.setColor(textColor);
    String s = String.format("%.1f", maxValue);
    g.drawString(
        s,
        (float) (pBB.min().x() - 2d * textW - g.getFontMetrics().stringWidth(s)),
        (float) (pBB.min().y() + textH / 2d)
    );
    g.drawString(
        "0",
        (float) (pBB.min().x() - 2d * textW - g.getFontMetrics().stringWidth("0")),
        (float) (pBB.max().y() + textH / 2d)
    );
  }
}
