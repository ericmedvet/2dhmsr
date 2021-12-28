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
import org.apache.commons.math3.stat.descriptive.moment.Variance;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * @author "Eric Medvet" on 2021/09/10 for 2dhmsr
 */
public class MLPDrawer extends MemoryDrawer<MLPState> {

  private static final Set<Part> PLOTS = Set.of(
      Part.ACTIVATION_VALUES,
      Part.WEIGHTS,
      Part.USAGE,
      Part.VARIANCE,
      Part.VARIANCE_AND_WEIGHTS
  );
  private final static double VARIANCE_WINDOW = 2d;
  private final static double LEGEND_COLORS = 15;
  private final static int N_OF_BINS = 15;
  private final static double HISTOGRAM_PLOT_RATIO = 0.1;
  private final static double HISTOGRAM_BLANK_RATIO = 0.9;
  private final Set<Part> parts;
  private final List<Part> plotParts;
  private final BoundingBox[] boundingBoxes;
  private final Color minColor;
  private final Color zeroColor;
  private final Color maxColor;
  private final Color axesColor;
  private final Color textColor;

  public MLPDrawer(
      Extractor extractor,
      double windowT,
      Set<Part> parts,
      Color minColor,
      Color zeroColor,
      Color maxColor,
      Color axesColor,
      Color textColor
  ) {
    super(extractor, s -> (MLPState) s.getContent(), windowT);
    this.parts = parts;
    this.plotParts = parts.stream().filter(PLOTS::contains).sorted().toList();
    this.boundingBoxes = new BoundingBox[plotParts.size()];
    this.minColor = minColor;
    this.zeroColor = zeroColor;
    this.maxColor = maxColor;
    this.axesColor = axesColor;
    this.textColor = textColor;
  }


  public MLPDrawer(Extractor extractor, double windowT, Set<Part> parts) {
    this(
        extractor,
        windowT,
        parts,
        DrawingUtils.Colors.DATA_NEGATIVE,
        DrawingUtils.Colors.DATA_ZERO,
        DrawingUtils.Colors.DATA_POSITIVE,
        DrawingUtils.Colors.AXES,
        DrawingUtils.Colors.TEXT
    );
  }

  public MLPDrawer(Extractor extractor, double windowT) {
    this(extractor, windowT, EnumSet.of(Part.ACTIVATION_VALUES, Part.LEGEND, Part.T_AXIS, Part.STRUCTURE_AXIS));
  }

  public enum Part {ACTIVATION_VALUES, WEIGHTS, USAGE, VARIANCE, VARIANCE_AND_WEIGHTS, LEGEND, T_AXIS, STRUCTURE_AXIS, HISTOGRAM}

  private static double[][] abs(double[][] v) {
    double[][] absV = new double[v.length][];
    IntStream.range(0, v.length).forEach(i -> absV[i] = Arrays.stream(v[i]).map(Math::abs).toArray());
    return absV;
  }

  private static double[][] flat(double[][][] v3) {
    return Arrays.stream(v3)
        .map(v2 -> Arrays.stream(v2).reduce(ArrayUtils::addAll).orElse(new double[0]))
        .toArray(double[][]::new);
  }

  private static double[][][] mapActivationValuesToWeights(double[][] activationValues, double[][][] weights) {
    if (weights[0][0].length > activationValues[0].length) {
      return mapActivationValuesToWeightsForMLP(activationValues, weights);
    } else {
      return mapActivationValuesToWeightsForSNN(activationValues, weights);
    }
  }

  private static double[][][] mapActivationValuesToWeightsForMLP(double[][] activationValues, double[][][] weights) {
    double[][][] mapped = new double[weights.length][][];
    for (int startingLayer = 0; startingLayer < weights.length; startingLayer++) {
      mapped[startingLayer] = new double[weights[startingLayer].length][];
      for (int destNeuron = 0; destNeuron < weights[startingLayer].length; destNeuron++) {
        mapped[startingLayer][destNeuron] = new double[weights[startingLayer][destNeuron].length];
        for (int startNeuron = 0; startNeuron < weights[startingLayer][destNeuron].length; startNeuron++) {
          mapped[startingLayer][destNeuron][startNeuron] = activationValues[startingLayer + 1][destNeuron];
        }
      }
    }
    return mapped;
  }

  private static double[][][] mapActivationValuesToWeightsForSNN(double[][] activationValues, double[][][] weights) {
    double[][][] mapped = new double[weights.length][][];
    for (int startingLayer = 0; startingLayer < weights.length; startingLayer++) {
      mapped[startingLayer] = new double[weights[startingLayer].length][];
      for (int startingNeuron = 0; startingNeuron < weights[startingLayer].length; startingNeuron++) {
        mapped[startingLayer][startingNeuron] = new double[weights[startingLayer][startingNeuron].length];
        for (int destinationNeuron = 0; destinationNeuron < weights[startingLayer][startingNeuron].length; destinationNeuron++) {
          mapped[startingLayer][startingNeuron][destinationNeuron] = activationValues[startingLayer + 1][destinationNeuron];
        }
      }
    }
    return mapped;
  }

  private static double max(double[][][] v) {
    return Arrays.stream(v).mapToDouble(MLPDrawer::max).max().orElse(0d);
  }

  private static double max(double[][] v) {
    return Arrays.stream(v).mapToDouble(w -> DoubleStream.of(w).max().orElse(0d)).max().orElse(0d);
  }

  private static double min(double[][][] v) {
    return Arrays.stream(v).mapToDouble(MLPDrawer::min).min().orElse(0d);
  }

  private static double min(double[][] v) {
    return Arrays.stream(v).mapToDouble(w -> DoubleStream.of(w).min().orElse(0d)).min().orElse(0d);
  }

  private void draw(
      double fT,
      SortedMap<Double, MLPState> states,
      Function<MLPState, double[][]> f,
      double min,
      double max,
      BoundingBox bb,
      Graphics2D g
  ) {
    draw(fT, 0d, states, f, min, max, bb, g);
  }

  private void draw(
      double fT,
      double vT,
      SortedMap<Double, MLPState> states,
      Function<MLPState, double[][]> f,
      double min,
      double max,
      BoundingBox bb,
      Graphics2D g
  ) {
    Variance variance = new Variance();
    double bbW = bb.max().x() - bb.min().x();
    double bbH = bb.max().y() - bb.min().y();
    double deltaT = states.size() == 1 ? (1d / 10d) : ((states.lastKey() - states.firstKey()) / (states.size() - 1));
    double[][] last = f.apply(states.get(states.lastKey()));
    double cellW = bbW * deltaT / windowT;
    double n = Arrays.stream(last).mapToInt(v -> v.length).sum();
    double cellH = bbH / n;
    double iT = fT - windowT;
    states.forEach((t, state) -> {
      double c = 0;
      double x = bb.min().x() + (t - iT) / windowT * bbW;
      if (x - 2 * cellW < bb.min().x()) {
        return;
      }
      double[][] valuesToPlot = f.apply(state);
      if (vT > 0) {
        List<double[][]> valuesList = states.keySet()
            .stream()
            .filter(t1 -> t1 <= t && t1 >= t - vT)
            .map(t1 -> f.apply(states.get(t1)))
            .toList();
        IntStream.range(0, valuesToPlot.length).forEach(i -> IntStream.range(0, valuesToPlot[i].length).forEach(j -> {
          variance.clear();
          valuesToPlot[i][j] = variance.evaluate(valuesList.stream().mapToDouble(array -> array[i][j]).toArray());
        }));
      }
      for (double[] doubles : valuesToPlot) {
        for (double aDouble : doubles) {
          double y = bb.min().y() + c / n * bbH;
          c = c + 1;
          g.setColor(DrawingUtils.linear(minColor, zeroColor, maxColor, (float) min, 0, (float) max, (float) aDouble));
          g.fill(new Rectangle2D.Double(x - 2 * cellW, y, 2 * cellW, cellH));
        }
      }
    });
  }

  private void drawDoubleChannel(
      double fT,
      double vT1,
      double vT2,
      SortedMap<Double, MLPState> states,
      Function<MLPState, double[][]> f1,
      double min1,
      double max1,
      Function<MLPState, double[][]> f2,
      double min2,
      double max2,
      BoundingBox bb,
      Graphics2D g
  ) {
    Variance variance = new Variance();
    double bbW = bb.max().x() - bb.min().x();
    double bbH = bb.max().y() - bb.min().y();
    double deltaT = states.size() == 1 ? (1d / 10d) : ((states.lastKey() - states.firstKey()) / (states.size() - 1));
    double[][] last = f1.apply(states.get(states.lastKey()));
    double cellW = bbW * deltaT / windowT;
    double n = Arrays.stream(last).mapToInt(v -> v.length).sum();
    double cellH = bbH / n;
    double iT = fT - windowT;
    states.forEach((t, state) -> {
      double c = 0;
      double x = bb.min().x() + (t - iT) / windowT * bbW;
      if (x - 2 * cellW < bb.min().x()) {
        return;
      }
      double[][] valuesToPlot1 = f1.apply(state);
      if (vT1 > 0) {
        List<double[][]> valuesList = states.keySet()
            .stream()
            .filter(t1 -> t1 <= t && t1 >= t - vT1)
            .map(t1 -> f1.apply(states.get(t1)))
            .toList();
        IntStream.range(0, valuesToPlot1.length).forEach(i -> IntStream.range(0, valuesToPlot1[i].length).forEach(j -> {
          variance.clear();
          valuesToPlot1[i][j] = variance.evaluate(valuesList.stream().mapToDouble(array -> array[i][j]).toArray());
        }));
      }
      double[][] valuesToPlot2 = f2.apply(state);
      if (vT2 > 0) {
        List<double[][]> valuesList = states.keySet()
            .stream()
            .filter(t1 -> t1 <= t && t1 >= t - vT2)
            .map(t1 -> f2.apply(states.get(t1)))
            .toList();
        IntStream.range(0, valuesToPlot1.length).forEach(i -> IntStream.range(0, valuesToPlot2[i].length).forEach(j -> {
          variance.clear();
          valuesToPlot2[i][j] = variance.evaluate(valuesList.stream().mapToDouble(array -> array[i][j]).toArray());
        }));
      }
      for (int i = 0; i < valuesToPlot1.length; i++) {
        for (int j = 0; j < valuesToPlot1[i].length; j++) {
          double y = bb.min().y() + c / n * bbH;
          c = c + 1;
          float red = (float) ((valuesToPlot1[i][j] - min1) / (max1 - min1));
          float green = (float) ((valuesToPlot2[i][j] - min2) / (max2 - min2));
          g.setColor(new Color(red, green, 0f));
          g.fill(new Rectangle2D.Double(x - 2 * cellW, y, 2 * cellW, cellH));
        }
      }
    });
  }

  private void drawDoubleChannelLegend(
      double min1,
      double max1,
      Color channel1,
      double min2,
      double max2,
      Color channel2,
      BoundingBox bb,
      double textW,
      double textH,
      Graphics2D g
  ) {
    Color minColor = Color.BLACK;
    double legendMiddle = (bb.max().y() + bb.min().y()) / 2;
    drawLegend(
        min1,
        max1,
        BoundingBox.of(bb.min().x(), bb.min().y() + textH / 2, bb.max().x(), legendMiddle - textH / 2),
        textW,
        g,
        minColor,
        minColor,
        channel1
    );
    drawLegend(
        min2,
        max2,
        BoundingBox.of(bb.min().x(), legendMiddle + textH / 2, bb.max().x(), bb.max().y() - textH / 2),
        textW,
        g,
        minColor,
        minColor,
        channel2
    );
  }

  private void drawHistogram(
      SortedMap<Double, MLPState> states,
      Function<MLPState, double[][]> f,
      double min,
      double max,
      BoundingBox bb,
      Graphics2D g
  ) {
    double[][] values = f.apply(states.get(states.lastKey()));
    double binSize = (max - min) / N_OF_BINS;
    int[] nOfValuesPerBin = new int[N_OF_BINS];
    Arrays.stream(values)
        .flatMapToDouble(Arrays::stream)
        .map(d -> Math.max(min, Math.min(d, max)) - min)
        .forEach(d -> nOfValuesPerBin[(int) Math.min(Math.floor(d / binSize), N_OF_BINS - 1)] += 1);
    int maxAmountPerBin = Arrays.stream(nOfValuesPerBin).max().orElse(10);
    double[] barSizes = Arrays.stream(nOfValuesPerBin)
        .mapToDouble(i -> i * bb.width() * HISTOGRAM_BLANK_RATIO / maxAmountPerBin)
        .toArray();
    double barHeight = (bb.max().y() - bb.min().y()) / N_OF_BINS;
    for (int i = 0; i < barSizes.length; i++) {
      double color = min + (0.5 + i) * binSize;
      Rectangle2D bar = new Rectangle2D.Double(
          bb.max().x() - barSizes[i],
          bb.min().y() + barHeight * i,
          barSizes[i],
          barHeight
      );
      g.setColor(DrawingUtils.linear(minColor, zeroColor, maxColor, (float) min, 0, (float) max, (float) color));
      g.fill(bar);
    }
  }

  private void drawLegend(
      double min,
      double max,
      BoundingBox bb,
      double textW,
      Graphics2D g,
      Color minColor,
      Color zeroColor,
      Color maxColor
  ) {
    double deltaY = (bb.max().y() - bb.min().y()) / LEGEND_COLORS;
    double deltaV = (max - min) / LEGEND_COLORS;
    double colorX = bb.max().x() - textW;
    for (int i = 0; i < LEGEND_COLORS; i++) {
      double vMin = min + deltaV * i;
      double vMax = vMin + deltaV;
      double yMin = bb.min().y() + deltaY * i;
      double numberHeight = g.getFontMetrics().getHeight() / 2d;
      g.setColor(DrawingUtils.linear(minColor, zeroColor, maxColor, (float) min, 0f, (float) max, (float) vMin));
      g.fill(new Rectangle2D.Double(colorX, yMin, textW, deltaY));
      if (i == 0) {
        g.setColor(textColor);
        String s = String.format("%.1f", vMin);
        g.drawString(
            s,
            (float) (colorX - textW - g.getFontMetrics().stringWidth(s)),
            (float) (yMin + numberHeight / 2d)
        );
      } else if (vMin <= 0 && vMax >= 0) {
        g.setColor(textColor);
        String s = "0";
        g.drawString(
            s,
            (float) (colorX - textW - g.getFontMetrics().stringWidth(s)),
            (float) (yMin + deltaY / 2d + numberHeight / 2d)
        );
      } else if (i >= LEGEND_COLORS - 1) {
        g.setColor(textColor);
        String s = String.format("%.1f", vMax);
        g.drawString(
            s,
            (float) (colorX - textW - g.getFontMetrics().stringWidth(s)),
            (float) (yMin + deltaY + numberHeight / 2d)
        );
      }
    }
  }

  private void drawLegend(double min, double max, BoundingBox bb, double textW, Graphics2D g) {
    drawLegend(min, max, bb, textW, g, minColor, zeroColor, maxColor);
  }

  private void drawStructure(int[] sizes, IntFunction<String> namer, BoundingBox bb, double textW, Graphics2D g) {
    double n = Arrays.stream(sizes).sum();
    double bbH = bb.max().y() - bb.min().y();
    g.setColor(axesColor);
    double c = 0;
    for (int i = 0; i < sizes.length; i++) {
      double minY = bb.min().y() + c / n * bbH;
      c = c + sizes[i];
      double maxY = bb.min().y() + c / n * bbH;
      g.setColor(axesColor);
      g.draw(new Line2D.Double(bb.min().x(), minY, bb.min().x() + textW, minY));
      g.draw(new Line2D.Double(bb.min().x(), maxY, bb.min().x() + textW, maxY));
      g.draw(new Line2D.Double(bb.min().x() + textW, minY, bb.min().x() + textW, maxY));
      g.setColor(textColor);
      String s = namer.apply(i);
      g.drawString(
          s,
          (float) (bb.min().x() + 2 * textW),
          (float) ((minY + maxY) / 2d + g.getFontMetrics().getHeight() / 2)
      );
    }
  }

  @Override
  protected void innerDraw(double t, Snapshot snapshot, SortedMap<Double, MLPState> memory, Graphics2D g) {
    MLPState current = memory.get(memory.lastKey());
    //prepare clips
    double textH = g.getFontMetrics().getMaxAscent();
    double textW = g.getFontMetrics().charWidth('m');
    double oBBOffset = parts.contains(Part.HISTOGRAM) ? HISTOGRAM_PLOT_RATIO * (g.getClip()
        .getBounds2D()
        .getMaxX() - g.getClip().getBounds2D().getX()) : 0;
    BoundingBox oBB = BoundingBox.of(
        g.getClip().getBounds2D().getX() + oBBOffset,
        g.getClip().getBounds2D().getY(),
        g.getClip().getBounds2D().getMaxX(),
        g.getClip().getBounds2D().getMaxY()
    );
    BoundingBox hBB = BoundingBox.of(g.getClip().getBounds2D().getX(), oBB.min().y(), oBB.min().x(), oBB.max().y());
    BoundingBox pBB = BoundingBox.of(
        parts.contains(Part.LEGEND) ? (oBB.min().x() + 6 * textW) : oBB.min().x(),
        oBB.min().y(),
        parts.contains(Part.STRUCTURE_AXIS) ? (oBB.max().x() - 5 * textW) : oBB.max().x(),
        parts.contains(Part.T_AXIS) ? (oBB.max().y() - 3 * textH) : oBB.max().y()
    );
    if (plotParts.size() == 1) {
      boundingBoxes[0] = pBB;
    }
    double size = (pBB.max().y() - pBB.min().y() - textH * (boundingBoxes.length - 1) / 2) / boundingBoxes.length;
    IntStream.range(0, boundingBoxes.length).forEach(i -> {
      double minY = i == 0 ? pBB.min().y() : boundingBoxes[i - 1].max().y() + textH / 2d;
      double maxY = (i == boundingBoxes.length - 1) ? pBB.max().y() : minY + size;
      boundingBoxes[i] = BoundingBox.of(pBB.min().x(), minY, pBB.max().x(), maxY);
    });
    if (parts.contains(Part.T_AXIS)) {
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
    if (parts.contains(Part.ACTIVATION_VALUES)) {
      double min = current.getActivationDomain().min() > Double.NEGATIVE_INFINITY ? current.getActivationDomain()
          .min() : memory.values().stream().mapToDouble(s -> min(s.getActivationValues())).min().orElse(0d);
      double max = current.getActivationDomain().max() < Double.POSITIVE_INFINITY ? current.getActivationDomain()
          .max() : memory.values().stream().mapToDouble(s -> max(s.getActivationValues())).max().orElse(0d);
      int j = plotParts.indexOf(Part.ACTIVATION_VALUES);
      draw(t, memory, MLPState::getActivationValues, min, max, boundingBoxes[j], g);
      if (parts.contains(Part.LEGEND)) {
        drawLegend(min, max, BoundingBox.of(
            oBB.min().x(),
            boundingBoxes[j].min().y() + textH,
            boundingBoxes[j].min().x() - textW,
            boundingBoxes[j].max().y() - textH
        ), textW, g);
      }
      if (parts.contains(Part.STRUCTURE_AXIS)) {
        drawStructure(
            Arrays.stream(current.getActivationValues()).mapToInt(v -> v.length).toArray(),
            i -> "l" + i,
            BoundingBox.of(
                boundingBoxes[j].max().x(),
                boundingBoxes[j].min().y(),
                oBB.max().x(),
                boundingBoxes[j].max().y()
            ),
            textW,
            g
        );
      }
      if (parts.contains(Part.HISTOGRAM)) {
        drawHistogram(
            memory,
            MLPState::getActivationValues,
            min,
            max,
            BoundingBox.of(
                hBB.min().x(),
                boundingBoxes[j].min().y() + textH,
                hBB.max().x(),
                boundingBoxes[j].max().y() - textH
            ),
            g
        );
      }
    }
    if (parts.contains(Part.WEIGHTS)) {
      double min = memory.values().stream().mapToDouble(s -> min(s.getWeights())).min().orElse(0d);
      double max = memory.values().stream().mapToDouble(s -> max(s.getWeights())).max().orElse(0d);
      int j = plotParts.indexOf(Part.WEIGHTS);
      draw(t, memory, s -> flat(s.getWeights()), min, max, boundingBoxes[j], g);
      if (parts.contains(Part.LEGEND)) {
        drawLegend(min, max, BoundingBox.of(
            oBB.min().x(),
            boundingBoxes[j].min().y() + textH,
            boundingBoxes[j].min().x() - textW,
            boundingBoxes[j].max().y() - textH
        ), textW, g);
      }
      if (parts.contains(Part.STRUCTURE_AXIS)) {
        drawStructure(
            Arrays.stream(flat(current.getWeights())).mapToInt(v -> v.length).toArray(),
            i -> "w" + i + (i + 1),
            BoundingBox.of(
                boundingBoxes[j].max().x(),
                boundingBoxes[j].min().y(),
                oBB.max().x(),
                boundingBoxes[j].max().y()
            ),
            textW,
            g
        );
      }
      if (parts.contains(Part.HISTOGRAM)) {
        drawHistogram(
            memory,
            s -> flat(s.getWeights()),
            min,
            max,
            BoundingBox.of(
                hBB.min().x(),
                boundingBoxes[j].min().y() + textH,
                hBB.max().x(),
                boundingBoxes[j].max().y() - textH
            ),
            g
        );
      }
    }
    if (parts.contains(Part.USAGE)) {
      double min = memory.values().stream().mapToDouble(s -> min(s.getActivationValues())).min().orElse(0d);
      double max = memory.values().stream().mapToDouble(s -> max(s.getActivationValues())).max().orElse(0d);
      int j = plotParts.indexOf(Part.USAGE);
      draw(
          t,
          memory,
          s -> flat(mapActivationValuesToWeights(s.getActivationValues(), s.getWeights())),
          min,
          max,
          boundingBoxes[j],
          g
      );
      if (parts.contains(Part.LEGEND)) {
        drawLegend(min, max, BoundingBox.of(
            oBB.min().x(),
            boundingBoxes[j].min().y() + textH,
            boundingBoxes[j].min().x() - textW,
            boundingBoxes[j].max().y() - textH
        ), textW, g);
      }
      if (parts.contains(Part.STRUCTURE_AXIS)) {
        drawStructure(
            Arrays.stream(flat(current.getWeights())).mapToInt(v -> v.length).toArray(),
            i -> "n-w" + i + (i + 1),
            BoundingBox.of(
                boundingBoxes[j].max().x(),
                boundingBoxes[j].min().y(),
                oBB.max().x(),
                boundingBoxes[j].max().y()
            ),
            textW,
            g
        );
      }
    }
    if (parts.contains(Part.VARIANCE)) {
      double min = memory.values().stream().mapToDouble(s -> min(s.getActivationValues())).min().orElse(0d);
      double max = memory.values().stream().mapToDouble(s -> max(s.getActivationValues())).max().orElse(0d);
      int j = plotParts.indexOf(Part.VARIANCE);
      draw(
          t,
          VARIANCE_WINDOW,
          memory,
          s -> flat(mapActivationValuesToWeights(s.getActivationValues(), s.getWeights())),
          min,
          max,
          boundingBoxes[j],
          g
      );
      if (parts.contains(Part.LEGEND)) {
        drawLegend(min, max, BoundingBox.of(
            oBB.min().x(),
            boundingBoxes[j].min().y() + textH,
            boundingBoxes[j].min().x() - textW,
            boundingBoxes[j].max().y() - textH
        ), textW, g);
      }
      if (parts.contains(Part.STRUCTURE_AXIS)) {
        drawStructure(
            Arrays.stream(flat(current.getWeights())).mapToInt(v -> v.length).toArray(),
            i -> "n-w" + i + (i + 1),
            BoundingBox.of(
                boundingBoxes[j].max().x(),
                boundingBoxes[j].min().y(),
                oBB.max().x(),
                boundingBoxes[j].max().y()
            ),
            textW,
            g
        );
      }
    }
    if (parts.contains(Part.VARIANCE_AND_WEIGHTS)) {
      double minWeights = 0d;
      double maxWeights = memory.values().stream().mapToDouble(s -> max(abs(flat(s.getWeights())))).max().orElse(0d);
      double minVariance = 0;
      double maxVariance = 2;
      int j = plotParts.indexOf(Part.VARIANCE_AND_WEIGHTS);
      drawDoubleChannel(
          t,
          0d,
          VARIANCE_WINDOW,
          memory,
          s -> abs(flat(s.getWeights())),
          minWeights,
          maxWeights,
          s -> flat(mapActivationValuesToWeights(s.getActivationValues(), s.getWeights())),
          minVariance,
          maxVariance,
          boundingBoxes[j],
          g
      );
      if (parts.contains(Part.LEGEND)) {
        drawDoubleChannelLegend(
            minWeights,
            maxWeights,
            Color.RED,
            minVariance,
            maxVariance,
            Color.GREEN,
            BoundingBox.of(
                oBB.min().x(),
                boundingBoxes[j].min().y(),
                boundingBoxes[j].min().x() - textW,
                boundingBoxes[j].max().y()
            ),
            textW,
            textH,
            g
        );
      }
      if (parts.contains(Part.STRUCTURE_AXIS)) {
        drawStructure(
            Arrays.stream(flat(current.getWeights())).mapToInt(v -> v.length).toArray(),
            i -> "w-v" + i + (i + 1),
            BoundingBox.of(
                boundingBoxes[j].max().x(),
                boundingBoxes[j].min().y(),
                oBB.max().x(),
                boundingBoxes[j].max().y()
            ),
            textW,
            g
        );
      }
    }
  }
}
