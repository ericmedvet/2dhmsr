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

import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.objects.BreakableVoxel;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.VoxelPoly;
import it.units.erallab.hmsrobots.util.Configurable;
import it.units.erallab.hmsrobots.util.ConfigurableField;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.List;

public class VoxelDrawer implements Drawer, Configurable<VoxelDrawer> {

  public enum FillType {APPLIED_FORCE, AREA_RATIO, NONE}

  @ConfigurableField(uiType = ConfigurableField.Type.BASIC)
  private FillType fillType = FillType.AREA_RATIO;
  @ConfigurableField
  private Color strokeColor = Color.BLUE;
  @ConfigurableField
  private Color restFillColor = GraphicsDrawer.alphaed(Color.YELLOW, 0.5f);
  @ConfigurableField
  private Color shrunkFillColor = GraphicsDrawer.alphaed(Color.RED, 0.5f);
  @ConfigurableField
  private Color expandedFillColor = GraphicsDrawer.alphaed(Color.GREEN, 0.5f);
  @ConfigurableField
  private Color malfunctionColor = GraphicsDrawer.alphaed(Color.BLACK, 0.75f);
  @ConfigurableField(uiMin = 5, uiMax = 20)
  private float malfunctionStrokeWidth = 3f;
  @ConfigurableField(uiMin = 0.1f, uiMax = 0.999f)
  private float shrunkRatio = 0.75f;
  @ConfigurableField(uiMin = 1.001f, uiMax = 2f)
  private float expandendRatio = 1.25f;

  public static VoxelDrawer build() {
    return new VoxelDrawer();
  }

  @Override
  public void draw(double t, List<Snapshot> lineage, Graphics2D g) {
    Snapshot last = lineage.get(lineage.size() - 1);
    if (!Drawer.match(last, VoxelPoly.class, Voxel.class)) {
      return;
    }
    VoxelPoly voxelPoly = (VoxelPoly) last.getContent();
    Path2D path = GraphicsDrawer.toPath(voxelPoly, true);
    g.setColor(strokeColor);
    g.draw(path);
    if (fillType.equals(FillType.AREA_RATIO)) {
      g.setColor(GraphicsDrawer.linear(
          shrunkFillColor, restFillColor, expandedFillColor,
          shrunkRatio, 1f, expandendRatio,
          (float) voxelPoly.getAreaRatio()
      ));
      g.fill(path);
    } else if (fillType.equals(FillType.APPLIED_FORCE)) {
      g.setColor(GraphicsDrawer.linear(
          shrunkFillColor, restFillColor, expandedFillColor,
          -1f, 0f, 1f,
          (float) voxelPoly.getLastAppliedForce()
      ));
      g.fill(path);
    }
    if (BreakableVoxel.class.isAssignableFrom(last.getSnapshottableClass())) {
      g.setColor(malfunctionColor);
      g.setStroke(new BasicStroke(malfunctionStrokeWidth / (float) g.getTransform().getScaleX()));
      if (!voxelPoly.getMalfunctions().get(BreakableVoxel.ComponentType.ACTUATOR).equals(BreakableVoxel.MalfunctionType.NONE)) {
        g.draw(GraphicsDrawer.toPath(voxelPoly.getVertexes()[0], voxelPoly.getVertexes()[2]));
      }
      if (!voxelPoly.getMalfunctions().get(BreakableVoxel.ComponentType.SENSORS).equals(BreakableVoxel.MalfunctionType.NONE)) {
        g.draw(GraphicsDrawer.toPath(voxelPoly.getVertexes()[1], voxelPoly.getVertexes()[3]));
      }
      if (!voxelPoly.getMalfunctions().get(BreakableVoxel.ComponentType.STRUCTURE).equals(BreakableVoxel.MalfunctionType.NONE)) {
        g.draw(GraphicsDrawer.toPath(
            Point2.average(voxelPoly.getVertexes()[0], voxelPoly.getVertexes()[3]),
            Point2.average(voxelPoly.getVertexes()[1], voxelPoly.getVertexes()[2])
        ));
      }
    }
  }

}
