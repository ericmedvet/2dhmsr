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
import it.units.erallab.hmsrobots.core.snapshots.MLPState;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.viewers.DrawingUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.DoubleStream;

/**
 * @author "Eric Medvet" on 2021/09/10 for 2dhmsr
 */
public class MLPDrawer extends SubtreeDrawer {

  public enum Part {ACTIVATION_VALUES, WEIGHTS, LEGEND, T_AXIS, STRUCTURE_AXIS}

  private final static Color MIN_COLOR = Color.RED;
  private final static Color ZERO_COLOR = Color.BLACK;
  private final static Color MAX_COLOR = Color.GREEN;

  private final static double LEGEND_COLORS = 15;

  private final double windowT;
  private final Set<Part> parts;
  private final Color minColor;
  private final Color zeroColor;
  private final Color maxColor;
  private final Color axesColor;
  private final Color textColor;

  private final SortedMap<Double, MLPState> states;

  public MLPDrawer(Extractor extractor, double windowT, Set<Part> parts, Color minColor, Color zeroColor, Color maxColor, Color axesColor, Color textColor) {
    super(extractor);
    this.windowT = windowT;
    this.parts = parts;
    this.minColor = minColor;
    this.zeroColor = zeroColor;
    this.maxColor = maxColor;
    this.axesColor = axesColor;
    this.textColor = textColor;
    states = new TreeMap<>();
  }

  public MLPDrawer(Extractor extractor, double windowT, Set<Part> parts) {
    this(extractor, windowT, parts, MIN_COLOR, ZERO_COLOR, MAX_COLOR, DrawingUtils.Colors.axes, DrawingUtils.Colors.text);
  }

  public MLPDrawer(Extractor extractor, double windowT) {
    this(extractor, windowT, EnumSet.of(Part.ACTIVATION_VALUES, Part.LEGEND, Part.T_AXIS, Part.STRUCTURE_AXIS));
  }

  @Override
  protected void innerDraw(double t, Snapshot snapshot, Graphics2D g) {
    if (!(snapshot.getContent() instanceof MLPState)) {
      return;
    }
    MLPState current = (MLPState) snapshot.getContent();
    //update memory
    states.put(t, current);
    while (states.firstKey() < (t - windowT)) {
      states.remove(states.firstKey());
    }
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
        parts.contains(Part.LEGEND) ? (oBB.min.x + 6 * textW) : oBB.min.x,
        oBB.min.y,
        parts.contains(Part.STRUCTURE_AXIS) ? (oBB.max.x - 5 * textW) : oBB.max.x,
        parts.contains(Part.T_AXIS) ? (oBB.max.y - 3 * textH) : oBB.max.y
    );
    BoundingBox aBB = parts.contains(Part.WEIGHTS) ? BoundingBox.of(
        pBB.min.x,
        pBB.min.y,
        pBB.max.x,
        pBB.min.y + (pBB.max.y - pBB.min.y) / 2d - textH / 2d
    ) : pBB;
    BoundingBox wBB = parts.contains(Part.ACTIVATION_VALUES) ? BoundingBox.of(
        pBB.min.x,
        aBB.max.y + textH / 2d,
        pBB.max.x,
        pBB.max.y
    ) : pBB;
    if (parts.contains(Part.T_AXIS)) {
      g.setColor(axesColor);
      g.draw(new Line2D.Double(pBB.min.x, pBB.max.y, pBB.max.x, pBB.max.y));
      double maxT = states.lastKey();
      for (double tickT = Math.ceil(maxT - windowT); tickT < maxT; tickT++) {
        g.setColor(axesColor);
        double x = (tickT - maxT + windowT) / windowT * (pBB.max.x - pBB.min.x) + pBB.min.x;
        g.draw(new Line2D.Double(x, pBB.max.y, x, pBB.max.y + textH));
        g.setColor(textColor);
        String s = String.format("%.0f", tickT);
        g.drawString(s, (float) (x - g.getFontMetrics().stringWidth(s) / 2f), (float) (pBB.max.y + 2 * textH));
      }
    }
    if (parts.contains(Part.ACTIVATION_VALUES)) {
      double min = current.getActivationDomain().getMin() > Double.NEGATIVE_INFINITY ? current.getActivationDomain().getMin() : states.values().stream()
          .mapToDouble(s -> min(s.getActivationValues()))
          .min().orElse(0d);
      double max = current.getActivationDomain().getMax() < Double.POSITIVE_INFINITY ? current.getActivationDomain().getMax() : states.values().stream()
          .mapToDouble(s -> max(s.getActivationValues()))
          .max().orElse(0d);
      draw(t, states, MLPState::getActivationValues, min, max, aBB, g);
      if (parts.contains(Part.LEGEND)) {
        drawLegend(
            min, max,
            BoundingBox.of(
                oBB.min.x,
                aBB.min.y + textH,
                aBB.min.x - textW,
                aBB.max.y - textH
            ),
            textW, g
        );
      }
      if (parts.contains(Part.STRUCTURE_AXIS)) {
        drawStructure(
            Arrays.stream(current.getActivationValues()).mapToInt(v -> v.length).toArray(),
            i -> "l" + i,
            BoundingBox.of(aBB.max.x, aBB.min.y, oBB.max.x, aBB.max.y),
            textW, g
        );
      }
    }
    if (parts.contains(Part.WEIGHTS)) {
      double min = states.values().stream()
          .mapToDouble(s -> min(s.getWeights()))
          .min().orElse(0d);
      double max = states.values().stream()
          .mapToDouble(s -> max(s.getWeights()))
          .max().orElse(0d);
      draw(t, states, s -> flat(s.getWeights()), min, max, wBB, g);
      if (parts.contains(Part.LEGEND)) {
        drawLegend(
            min, max,
            BoundingBox.of(
                oBB.min.x,
                wBB.min.y + textH,
                wBB.min.x - textW,
                wBB.max.y - textH
            ),
            textW, g
        );
      }
      if (parts.contains(Part.STRUCTURE_AXIS)) {
        drawStructure(
            Arrays.stream(flat(current.getWeights())).mapToInt(v -> v.length).toArray(),
            i -> "w" + i + (i + 1),
            BoundingBox.of(wBB.max.x, wBB.min.y, oBB.max.x, wBB.max.y),
            textW, g
        );
      }
    }
  }

  private void drawLegend(double min, double max, BoundingBox bb, double textW, Graphics2D g) {
    double deltaY = (bb.max.y - bb.min.y) / LEGEND_COLORS;
    double deltaV = (max - min) / LEGEND_COLORS;
    double colorX = bb.max.x - textW;
    for (int i = 0; i < LEGEND_COLORS; i++) {
      double vMin = min + deltaV * i;
      double vMax = vMin + deltaV;
      double yMin = bb.min.y + deltaY * i;
      g.setColor(DrawingUtils.linear(minColor, zeroColor, maxColor, (float) min, 0f, (float) max, (float) vMin));
      g.fill(new Rectangle2D.Double(colorX, yMin, textW, deltaY));
      if (i == 0) {
        g.setColor(textColor);
        String s = String.format("%.1f", vMin);
        g.drawString(s,
            (float) (colorX - textW - g.getFontMetrics().stringWidth(s)),
            (float) (yMin + g.getFontMetrics().getHeight()));
      } else if (vMin <= 0 && vMax >= 0) {
        g.setColor(textColor);
        String s = "0";
        g.drawString(s,
            (float) (colorX - textW - g.getFontMetrics().stringWidth(s)),
            (float) (yMin + deltaY / 2d + g.getFontMetrics().getHeight() / 2d));
      } else if (i >= LEGEND_COLORS - 1) {
        g.setColor(textColor);
        String s = String.format("%.1f", vMax);
        g.drawString(s,
            (float) (colorX - textW - g.getFontMetrics().stringWidth(s)),
            (float) (yMin + deltaY));
      }
    }
  }

  private void drawStructure(int[] sizes, IntFunction<String> namer, BoundingBox bb, double textW, Graphics2D g) {
    double n = Arrays.stream(sizes).sum();
    double bbH = bb.max.y - bb.min.y;
    g.setColor(axesColor);
    double c = 0;
    for (int i = 0; i < sizes.length; i++) {
      double minY = bb.min.y + c / n * bbH;
      c = c + sizes[i];
      double maxY = bb.min.y + c / n * bbH;
      g.setColor(axesColor);
      g.draw(new Line2D.Double(bb.min.x, minY, bb.min.x + textW, minY));
      g.draw(new Line2D.Double(bb.min.x, maxY, bb.min.x + textW, maxY));
      g.draw(new Line2D.Double(bb.min.x + textW, minY, bb.min.x + textW, maxY));
      g.setColor(textColor);
      String s = namer.apply(i);
      g.drawString(s, (float) (bb.min.x + 2 * textW), (float) ((minY + maxY) / 2d + g.getFontMetrics().getHeight() / 2));
    }
  }

  private void draw(double fT, SortedMap<Double, MLPState> states, Function<MLPState, double[][]> f, double min, double max, BoundingBox bb, Graphics2D g) {
    double bbW = bb.max.x - bb.min.x;
    double bbH = bb.max.y - bb.min.y;
    double deltaT = states.size() == 1 ? (1d / 10d) : ((states.lastKey() - states.firstKey()) / (states.size() - 1));
    double[][] last = f.apply(states.get(states.lastKey()));
    double cellW = bbW * deltaT / windowT;
    double n = Arrays.stream(last).mapToInt(v -> v.length).sum();
    double cellH = bbH / n;
    double iT = fT - windowT;
    states.forEach((t, state) -> {
      double c = 0;
      double x = bb.min.x + (t - iT) / windowT * bbW;
      if (x - 2 * cellW < bb.min.x) {
        return;
      }
      for (double[] vs : f.apply(state)) {
        for (double v : vs) {
          double y = bb.min.y + c / n * bbH;
          c = c + 1;
          g.setColor(DrawingUtils.linear(minColor, zeroColor, maxColor, (float) min, 0, (float) max, (float) v));
          g.fill(new Rectangle2D.Double(x - 2 * cellW, y, 2 * cellW, cellH));
        }
      }
    });
  }

  private static double max(double[][] v) {
    return Arrays.stream(v)
        .mapToDouble(w -> DoubleStream.of(w).max().orElse(0d))
        .max().orElse(0d);
  }

  private static double min(double[][] v) {
    return Arrays.stream(v)
        .mapToDouble(w -> DoubleStream.of(w).min().orElse(0d))
        .min().orElse(0d);
  }

  private static double max(double[][][] v) {
    return Arrays.stream(v)
        .mapToDouble(MLPDrawer::max)
        .max().orElse(0d);
  }

  private static double min(double[][][] v) {
    return Arrays.stream(v)
        .mapToDouble(MLPDrawer::min)
        .min().orElse(0d);
  }

  private static double[][] flat(double[][][] v3) {
    return Arrays.stream(v3)
        .map(v2 -> Arrays.stream(v2).reduce(ArrayUtils::addAll).orElse(new double[0]))
        .toArray(double[][]::new);
  }
}
