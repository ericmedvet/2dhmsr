/*
 * Copyright (C) 2020 Eric Medvet <eric.medvet@gmail.com> (as eric)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.viewers.drawers;

import it.units.erallab.hmsrobots.core.objects.BreakableVoxel;
import it.units.erallab.hmsrobots.core.objects.immutable.ControllableVoxel;
import it.units.erallab.hmsrobots.core.objects.immutable.Immutable;
import it.units.erallab.hmsrobots.util.Configurable;
import it.units.erallab.hmsrobots.util.ConfigurableField;
import it.units.erallab.hmsrobots.util.Point2;
import it.units.erallab.hmsrobots.util.Poly;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;

import java.awt.*;
import java.awt.geom.Path2D;

public class Voxel extends Drawer<it.units.erallab.hmsrobots.core.objects.immutable.Voxel> implements Configurable<Voxel> {

  public enum FillType {APPLIED_FORCE, AREA_RATIO, CONTROL_ENERGY_DELTA, NONE}

  @ConfigurableField(uiType = ConfigurableField.Type.BASIC)
  private final FillType fillType = FillType.AREA_RATIO;
  @ConfigurableField
  private final Color strokeColor = Color.BLUE;
  @ConfigurableField
  private final Color restFillColor = GraphicsDrawer.alphaed(Color.YELLOW, 0.5f);
  @ConfigurableField
  private final Color shrunkFillColor = GraphicsDrawer.alphaed(Color.RED, 0.5f);
  @ConfigurableField
  private final Color expandedFillColor = GraphicsDrawer.alphaed(Color.GREEN, 0.5f);
  @ConfigurableField
  private final Color malfunctionColor = GraphicsDrawer.alphaed(Color.BLACK, 0.75f);
  @ConfigurableField(uiMin = 5, uiMax = 20)
  private final float malfunctionStrokeWidth = 10f;
  @ConfigurableField(uiMin = 0.1f, uiMax = 0.999f)
  private final float shrunkRatio = 0.75f;
  @ConfigurableField(uiMin = 1.001f, uiMax = 2f)
  private final float expandendRatio = 1.25f;

  private Voxel() {
    super(it.units.erallab.hmsrobots.core.objects.immutable.Voxel.class);
  }

  public static Voxel build() {
    return new Voxel();
  }

  @Override
  public boolean draw(it.units.erallab.hmsrobots.core.objects.immutable.Voxel immutable, Immutable parent, Graphics2D g) {
    Poly poly = (Poly) immutable.getShape();
    Path2D path = GraphicsDrawer.toPath(poly, true);
    g.setColor(strokeColor);
    g.draw(path);
    if (fillType.equals(FillType.AREA_RATIO)) {
      g.setColor(GraphicsDrawer.linear(
          shrunkFillColor, restFillColor, expandedFillColor,
          shrunkRatio, 1f, expandendRatio,
          (float) immutable.getAreaRatio()
      ));
      g.fill(path);
    } else if (fillType.equals(FillType.APPLIED_FORCE) && (immutable instanceof ControllableVoxel)) {
      g.setColor(GraphicsDrawer.linear(
          shrunkFillColor, restFillColor, expandedFillColor,
          -1f, 0f, 1f,
          (float) ((ControllableVoxel) immutable).getAppliedForce()
      ));
      g.fill(path);
    } else if (fillType.equals(FillType.CONTROL_ENERGY_DELTA) && (immutable instanceof ControllableVoxel)) {
      g.setColor(GraphicsDrawer.linear(restFillColor, expandedFillColor,
          0f, 1f,
          (float) ((ControllableVoxel) immutable).getControlEnergyDelta()
      ));
      g.fill(path);
    }
    if (immutable instanceof it.units.erallab.hmsrobots.core.objects.immutable.BreakableVoxel) {
      g.setColor(malfunctionColor);
      g.setStroke(new BasicStroke(malfunctionStrokeWidth / (float) g.getTransform().getScaleX()));
      it.units.erallab.hmsrobots.core.objects.immutable.BreakableVoxel breakableVoxel = (it.units.erallab.hmsrobots.core.objects.immutable.BreakableVoxel) immutable;
      if (!breakableVoxel.getActuatorMalfunctionType().equals(BreakableVoxel.MalfunctionType.NONE)) {
        g.draw(GraphicsDrawer.toPath(poly.getVertexes()[0], poly.getVertexes()[2]));
      }
      if (!breakableVoxel.getSensorsMalfunctionType().equals(BreakableVoxel.MalfunctionType.NONE)) {
        g.draw(GraphicsDrawer.toPath(poly.getVertexes()[1], poly.getVertexes()[3]));
      }
      if (!breakableVoxel.getStructureMalfunctionType().equals(BreakableVoxel.MalfunctionType.NONE)) {
        g.draw(GraphicsDrawer.toPath(
            Point2.average(poly.getVertexes()[0], poly.getVertexes()[3]),
            Point2.average(poly.getVertexes()[1], poly.getVertexes()[2])
        ));
      }
    }
    return true;
  }
}
