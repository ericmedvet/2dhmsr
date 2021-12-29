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
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.viewers.DrawingUtils;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Objects;
import java.util.SortedMap;

/**
 * @author "Eric Medvet" on 2021/09/22 for 2dhmsr
 */
public class PostureDrawer extends MemoryDrawer<Grid<Boolean>> {

  private final int n;
  private final boolean isBoolean;
  private final Color dataColor;
  private final Color axesColor;

  public PostureDrawer(
      Extractor extractor, double windowT, int n, boolean isBoolean, Color dataColor, Color axesColor
  ) {
    super(
        extractor,
        BehaviorUtils.voxelPolyGrid()
            .andThen(g -> BehaviorUtils.computePosture(g.values()
                .stream()
                .filter(Objects::nonNull)
                .toList(), n)),
        windowT
    );
    this.n = n;
    this.isBoolean = isBoolean;
    this.dataColor = dataColor;
    this.axesColor = axesColor;
  }

  public PostureDrawer(Extractor extractor, double windowT, int n, boolean isBoolean) {
    this(extractor, windowT, n, isBoolean, DrawingUtils.Colors.DATA, DrawingUtils.Colors.AXES);
  }

  @Override
  protected void innerDraw(double t, Snapshot snapshot, SortedMap<Double, Grid<Boolean>> memory, Graphics2D g) {
    //compute aggregate
    Grid<Double> average = Grid.create(
        n,
        n,
        (x, y) -> memory.values().stream().mapToDouble(grid -> grid.get(x, y) ? 1d : 0d).average().orElse(0d)
    );
    //prepare clips
    double textH = g.getFontMetrics().getMaxAscent();
    BoundingBox oBB = BoundingBox.of(
        g.getClip().getBounds2D().getX(),
        g.getClip().getBounds2D().getY(),
        g.getClip().getBounds2D().getMaxX(),
        g.getClip().getBounds2D().getMaxY()
    );
    BoundingBox pBB = (oBB.width() > oBB.height()) ? BoundingBox.of(
        oBB.min()
            .x() + (oBB.width() - oBB.height()) / 2d + textH,
        oBB.min().y() + textH,
        oBB.max().x() - (oBB.width() - oBB.height()) / 2d - textH,
        oBB.max().y() - textH
    ) : BoundingBox.of(
        oBB.min().x() + textH,
        oBB.min().y() + (oBB.height() - oBB.width()) / 2d + textH,
        oBB.max().x() - textH,
        oBB.max().y() - (oBB.height() - oBB.width()) / 2d - textH
    );
    //draw data
    double l = pBB.width() / (double) n;
    average.forEach(e -> {
      if (!isBoolean || e.value() > 0.5d) {
        double minX = pBB.min().x() + (double) e.key().x() / (double) n * pBB.width();
        double minY = pBB.min().y() + (n - (double) e.key().y() - 1) / (double) n * pBB.width();
        g.setColor(isBoolean ? dataColor : DrawingUtils.alphaed(dataColor, e.value().floatValue()));
        g.fill(new Rectangle2D.Double(minX, minY, l, l));
      }
    });
    //draw box
    g.setColor(axesColor);
    g.draw(new Rectangle2D.Double(pBB.min().x(), pBB.min().y(), pBB.width(), pBB.height()));
  }
}
