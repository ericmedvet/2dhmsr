/*
 * Copyright (C) 2022 Giorgia Nadizar <giorgia.nadizar@gmail.com> (as Giorgia Nadizar)
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
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.viewers.DrawingUtils;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.SortedMap;

import static it.units.erallab.hmsrobots.core.controllers.DistributedSensing.DistributedSensingState;

public class DistributedDrawer extends MemoryDrawer<DistributedSensingState> {

  private final Color minColor;
  private final Color zeroColor;
  private final Color maxColor;
  private final Color textColor;

  private static final double CENTER_RATIO = 0.4;
  private static final double BORDER_RATIO = 0.02;
  private final static double LEGEND_COLORS = 15;

  public DistributedDrawer(
      Extractor extractor,
      double windowT,
      Color minColor,
      Color zeroColor,
      Color maxColor,
      Color textColor
  ) {
    super(extractor, s -> (DistributedSensingState) s.getContent(), windowT);
    this.minColor = minColor;
    this.zeroColor = zeroColor;
    this.maxColor = maxColor;
    this.textColor = textColor;
  }

  public DistributedDrawer(Extractor extractor, double windowT) {
    this(extractor,
        windowT,
        Color.GREEN,
        Color.YELLOW,
        Color.RED,
        DrawingUtils.Colors.AXES
    );
  }

  @Override
  protected void innerDraw(double t, Snapshot snapshot, SortedMap<Double, DistributedSensingState> memory, Graphics2D g) {
    DistributedSensingState current = memory.get(memory.lastKey());
    double textWidth = g.getFontMetrics().charWidth('m');
    double legendWidth = 6 * textWidth;
    double min = current.signalsDomain().min();
    double max = current.signalsDomain().max();
    BoundingBox robotBB = BoundingBox.of(
        g.getClip().getBounds2D().getMinX() + 2 * BORDER_RATIO * g.getClip().getBounds2D().getWidth() + legendWidth,
        g.getClip().getBounds2D().getY() + BORDER_RATIO * g.getClip().getBounds2D().getHeight(),
        g.getClip().getBounds2D().getMaxX() - 2 * BORDER_RATIO * g.getClip().getBounds2D().getWidth() - legendWidth,
        g.getClip().getBounds2D().getMaxY() - BORDER_RATIO * g.getClip().getBounds2D().getHeight()
    );
    BoundingBox usedRobotBB = drawRobot(
        current.body(),
        current.controlSignalsGrid(),
        current.lastSignalsGrid(),
        min,
        max,
        robotBB,
        g
    );
    BoundingBox legendBB = BoundingBox.of(
        usedRobotBB.min().x() - legendWidth - BORDER_RATIO * g.getClip().getBounds2D().getWidth(),
        g.getClip().getBounds2D().getY() + BORDER_RATIO * g.getClip().getBounds2D().getHeight(),
        usedRobotBB.min().x() - BORDER_RATIO * g.getClip().getBounds2D().getWidth(),
        g.getClip().getBounds2D().getMaxY() - BORDER_RATIO * g.getClip().getBounds2D().getHeight()
    );
    drawLegend(
        min,
        max,
        legendBB,
        textWidth,
        g,
        minColor,
        zeroColor,
        maxColor
    );

  }

  private BoundingBox drawRobot(
      Grid<Boolean> body,
      Grid<Double> controlSignalsGrid,
      Grid<double[]> lastSignalsGrid,
      double min,
      double max,
      BoundingBox bb,
      Graphics2D g
  ) {
    double voxelSide = Math.min(
        bb.width() / controlSignalsGrid.getW(),
        bb.height() / controlSignalsGrid.getH()
    );
    double offsetX = (bb.width() - (voxelSide * controlSignalsGrid.getW())) / 2;
    double offsetY = (bb.height() - (voxelSide * controlSignalsGrid.getH())) / 2;
    for (int x = 0; x < controlSignalsGrid.getW(); x++) {
      for (int y = 0; y < controlSignalsGrid.getH(); y++) {
        BoundingBox voxelBB = BoundingBox.of(
            offsetX + bb.min().x() + x * voxelSide,
            offsetY + bb.min().y() + y * voxelSide,
            offsetX + bb.min().x() + (x + 1) * voxelSide,
            offsetY + bb.min().y() + (y + 1) * voxelSide
        );
        if (body.get(x, body.getH() - y - 1)) {
          drawVoxel(
              controlSignalsGrid.get(x, controlSignalsGrid.getH() - y - 1),
              lastSignalsGrid.get(x, lastSignalsGrid.getH() - y - 1),
              min,
              max,
              voxelBB,
              g
          );
        }
      }
    }
    return BoundingBox.of(
        bb.min().x() + offsetX,
        bb.min().y() + offsetY,
        bb.max().x() - offsetX,
        bb.max().y() - offsetY
    );
  }

  private void drawVoxel(
      double controlSignal,
      double[] signals,
      double min,
      double max,
      BoundingBox bb,
      Graphics2D g
  ) {
    // inner rectangle
    g.setColor(DrawingUtils.linear(minColor, zeroColor, maxColor, (float) min, 0, (float) max, (float) controlSignal));
    Rectangle2D cell = new Rectangle2D.Double(
        bb.min().x() + bb.width() * (1 - CENTER_RATIO) / 2,
        bb.min().y() + bb.height() * (1 - CENTER_RATIO) / 2,
        bb.width() * CENTER_RATIO,
        bb.height() * CENTER_RATIO
    );
    g.fill(cell);

    int nSignals = signals.length / 4;
    double[] top = Arrays.stream(signals, 0, nSignals).toArray();
    double[] bottom = Arrays.stream(signals, 2 * nSignals, 3 * nSignals).toArray();
    double[] right = Arrays.stream(signals, nSignals, 2 * nSignals).toArray();
    double[] left = Arrays.stream(signals, 3 * nSignals, 4 * nSignals).toArray();
    double width = bb.width() / nSignals;
    double height = bb.height() / nSignals;
    double centerOffsetY = bb.height() * (1 - CENTER_RATIO) / 2;
    double centerOffsetX = bb.width() * (1 - CENTER_RATIO) / 2;

    for (int i = 0; i < nSignals; i++) {
      double[] x = new double[5];
      x[0] = bb.min().x() + i * width;
      x[1] = x[0];
      x[3] = bb.min().x() + (i + 1) * width;
      x[4] = x[3];
      double[] h = new double[5];
      h[1] = computeCellHeight(i * width, bb.width(), bb.height());
      h[3] = computeCellHeight((i + 1) * width, bb.width(), bb.height());
      if (h[1] == 0 && (h[3] == centerOffsetY || h[3] == 0)) {
        x[1] = bb.min().x() + centerOffsetX;
        h[1] = centerOffsetY;
      }
      if (h[3] == 0 && (h[1] == centerOffsetY || h[1] == 0)) {
        x[3] = bb.max().x() - centerOffsetX;
        h[3] = centerOffsetY;
      }
      if (h[1] != h[3] && (h[1] == centerOffsetY || h[3] == centerOffsetY)) {
        h[2] = centerOffsetY;
        if (h[1] == centerOffsetY) {
          x[2] = bb.max().x() - centerOffsetX;
        } else {
          x[2] = bb.min().x() + centerOffsetX;
        }
      } else {
        x[2] = (x[1] + x[3]) / 2;
        h[2] = (h[1] + h[3]) / 2;
      }

      double[] y = new double[5];
      y[0] = bb.min().y() + i * height;
      y[1] = y[0];
      y[3] = bb.min().y() + (i + 1) * height;
      y[4] = y[3];
      double[] w = new double[5];
      w[1] = computeCellWidth(i * height, bb.width(), bb.height());
      w[3] = computeCellWidth((i + 1) * height, bb.width(), bb.height());
      if (w[1] == 0 && (w[3] == centerOffsetX || w[3] == 0)) {
        y[1] = bb.min().y() + centerOffsetY;
        w[1] = centerOffsetX;
      }
      if (w[3] == 0 && (w[1] == centerOffsetX || w[1] == 0)) {
        y[3] = bb.max().y() - centerOffsetY;
        w[3] = centerOffsetX;
      }
      if (w[1] != w[3] && (w[1] == centerOffsetX || w[3] == centerOffsetX)) {
        w[2] = centerOffsetX;
        if (w[1] == centerOffsetX) {
          y[2] = bb.max().y() - centerOffsetY;
        } else {
          y[2] = bb.min().y() + centerOffsetY;
        }
      } else {
        y[2] = (y[1] + y[3]) / 2;
        w[2] = (w[1] + w[3]) / 2;
      }

      // create polygons
      Path2D topPath = new Path2D.Double();
      Path2D bottomPath = new Path2D.Double();
      Path2D leftPath = new Path2D.Double();
      Path2D rightPath = new Path2D.Double();
      topPath.moveTo(x[0], bb.min().y() + h[0]);
      bottomPath.moveTo(x[0], bb.max().y() - h[0]);
      leftPath.moveTo(bb.min().x() + w[0], y[0]);
      rightPath.moveTo(bb.max().x() - w[0], y[0]);
      for (int v = 1; v < x.length; v++) {
        topPath.lineTo(x[v], bb.min().y() + h[v]);
        bottomPath.lineTo(x[v], bb.max().y() - h[v]);
        leftPath.lineTo(bb.min().x() + w[v], y[v]);
        rightPath.lineTo(bb.max().x() - w[v], y[v]);
      }
      topPath.closePath();
      bottomPath.closePath();
      leftPath.closePath();
      rightPath.closePath();

      // color shapes
      g.setColor(DrawingUtils.linear(minColor, zeroColor, maxColor, (float) min, 0, (float) max, (float) top[i]));
      g.fill(topPath);
      g.setColor(DrawingUtils.linear(minColor, zeroColor, maxColor, (float) min, 0, (float) max, (float) bottom[i]));
      g.fill(bottomPath);
      g.setColor(DrawingUtils.linear(minColor, zeroColor, maxColor, (float) min, 0, (float) max, (float) left[i]));
      g.fill(leftPath);
      g.setColor(DrawingUtils.linear(minColor, zeroColor, maxColor, (float) min, 0, (float) max, (float) right[i]));
      g.fill(rightPath);
    }

  }

  private double computeCellHeight(double x, double width, double height) {
    if (x > width / 2) {
      x = width - x;
    }
    return Math.min(
        height * (1 - CENTER_RATIO) / 2,
        x * height / width
    );
  }

  private double computeCellWidth(double y, double width, double height) {
    if (y > height / 2) {
      y = height - y;
    }
    return Math.min(
        height * (1 - CENTER_RATIO) / 2,
        y * width / height
    );
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

}
