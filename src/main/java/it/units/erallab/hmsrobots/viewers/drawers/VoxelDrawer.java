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

import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.geometry.Poly;
import it.units.erallab.hmsrobots.core.objects.BreakableVoxel;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.VoxelPoly;
import it.units.erallab.hmsrobots.viewers.DrawingUtils;

import java.awt.*;
import java.awt.geom.Path2D;

public class VoxelDrawer extends SubtreeDrawer {

  private final static Color STROKE_COLOR = Color.BLUE;
  private final static Color REST_FILL_COLOR = DrawingUtils.alphaed(Color.YELLOW, 0.75f);
  private final static Color SHRUNK_FILL_COLOR = DrawingUtils.alphaed(Color.RED, 0.75f);
  private final static Color EXPANDED_FILL_COLOR = DrawingUtils.alphaed(Color.GREEN, 0.75f);
  private final static Color MALFUNCTION_COLOR = DrawingUtils.alphaed(Color.BLACK, 0.95f);
  private final static float MALFUNCTION_STROKE_WIDTH = 3f;
  private final static float SHRUNK_RATIO = 0.75f;
  private final static float EXPANDEND_RATIO = 1.25f;
  private final FillType fillType;

  public VoxelDrawer(FillType fillType) {
    super(Extractor.matches(VoxelPoly.class, Voxel.class, null));
    this.fillType = fillType;
  }

  public VoxelDrawer() {
    this(FillType.AREA_RATIO);
  }

  public enum FillType {APPLIED_FORCE, AREA_RATIO, NONE}

  @Override
  protected void innerDraw(double t, Snapshot snapshot, Graphics2D g) {
    VoxelPoly voxelPoly = (VoxelPoly) snapshot.getContent();
    Path2D path = DrawingUtils.toPath(Poly.of(voxelPoly.vertexes()), true);
    g.setColor(STROKE_COLOR);
    g.draw(path);
    if (fillType.equals(FillType.AREA_RATIO)) {
      g.setColor(DrawingUtils.linear(
          SHRUNK_FILL_COLOR, REST_FILL_COLOR, EXPANDED_FILL_COLOR,
          SHRUNK_RATIO, 1f, EXPANDEND_RATIO,
          (float) voxelPoly.getAreaRatio()
      ));
      g.fill(path);
    } else if (fillType.equals(FillType.APPLIED_FORCE)) {
      g.setColor(DrawingUtils.linear(
          SHRUNK_FILL_COLOR, REST_FILL_COLOR, EXPANDED_FILL_COLOR,
          -1f, 0f, 1f,
          (float) voxelPoly.getLastAppliedForce()
      ));
      g.fill(path);
    }
    if (BreakableVoxel.class.isAssignableFrom(snapshot.getSnapshottableClass())) {
      g.setColor(MALFUNCTION_COLOR);
      g.setStroke(new BasicStroke(MALFUNCTION_STROKE_WIDTH / (float) g.getTransform().getScaleX()));
      if (!voxelPoly.getMalfunctions()
          .get(BreakableVoxel.ComponentType.ACTUATOR)
          .equals(BreakableVoxel.MalfunctionType.NONE)) {
        g.draw(DrawingUtils.toPath(voxelPoly.vertexes()[0], voxelPoly.vertexes()[2]));
      }
      if (!voxelPoly.getMalfunctions()
          .get(BreakableVoxel.ComponentType.SENSORS)
          .equals(BreakableVoxel.MalfunctionType.NONE)) {
        g.draw(DrawingUtils.toPath(voxelPoly.vertexes()[1], voxelPoly.vertexes()[3]));
      }
      if (!voxelPoly.getMalfunctions()
          .get(BreakableVoxel.ComponentType.STRUCTURE)
          .equals(BreakableVoxel.MalfunctionType.NONE)) {
        g.draw(DrawingUtils.toPath(
            Point2.average(voxelPoly.vertexes()[0], voxelPoly.vertexes()[3]),
            Point2.average(voxelPoly.vertexes()[1], voxelPoly.vertexes()[2])
        ));
      }
    }
  }

}
