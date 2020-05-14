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

import it.units.erallab.hmsrobots.core.objects.immutable.Immutable;
import it.units.erallab.hmsrobots.core.objects.immutable.Voxel;
import it.units.erallab.hmsrobots.core.sensors.Sensor;
import it.units.erallab.hmsrobots.util.Configurable;
import it.units.erallab.hmsrobots.util.ConfigurableField;
import it.units.erallab.hmsrobots.util.Point2;
import it.units.erallab.hmsrobots.util.Poly;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;

import java.awt.*;
import java.awt.geom.Path2D;


public class SensorReading extends Drawer<it.units.erallab.hmsrobots.core.sensors.immutable.SensorReading> implements Configurable<SensorReading> {

  @ConfigurableField
  private final Color fillColor = GraphicsDrawer.alphaed(Color.BLACK, 0.5f);
  @ConfigurableField
  private final Color strokeColor = Color.BLACK;
  @ConfigurableField(uiMin = 0f, uiMax = 2f * (float) Math.PI)
  private final float spanAngle = (float) Math.PI;
  @ConfigurableField(uiMin = 0.001f * (float) Math.PI, uiMax = 0.5f * (float) Math.PI)
  private final float angleResolution = 0.01f * (float) Math.PI;
  @ConfigurableField
  private final boolean sensorFrame = true;

  private SensorReading() {
    super(it.units.erallab.hmsrobots.core.sensors.immutable.SensorReading.class);
  }

  public static SensorReading build() {
    return new SensorReading();
  }

  private Path2D getSector(Point2 c, double r, double a1, double a2) {
    Path2D sector = new Path2D.Double();
    sector.moveTo(c.x, c.y);
    for (double a = a1; a < a2; a = a + angleResolution) {
      sector.lineTo(c.x + r * Math.cos(a), c.y + r * Math.sin(a));
    }
    sector.lineTo(c.x + r * Math.cos(a2), c.y + r * Math.sin(a2));
    sector.closePath();
    return sector;
  }

  @Override
  public boolean draw(it.units.erallab.hmsrobots.core.sensors.immutable.SensorReading immutable, Immutable parent, Graphics2D g) {
    Poly voxelPoly = (Poly) ((Voxel) parent).getShape();
    double radius = Math.sqrt(voxelPoly.area()) / 2d;
    Point2 center = voxelPoly.center();
    double angle = 0;
    double sensorSliceAngle = spanAngle / (double) immutable.getnOfSensors();
    double sensorStartingAngle = angle + (double) immutable.getSensorIndex() * sensorSliceAngle;
    double valueSliceAngle = sensorSliceAngle / (double) immutable.getValues().length;
    if (sensorFrame) {
      g.setColor(strokeColor);
      Path2D sector = getSector(center, radius, sensorStartingAngle, sensorStartingAngle + sensorSliceAngle);
      g.draw(sector);
    }
    g.setColor(fillColor);
    for (int i = 0; i < immutable.getValues().length; i++) {
      double value = immutable.getValues()[i];
      Sensor.Domain d = immutable.getDomains()[i];
      double normalizedRadius = radius * Math.min(1d, Math.max(0d, (value - d.getMin()) / (d.getMax() - d.getMin())));
      double valueStartingAngle = sensorStartingAngle + (double) i * valueSliceAngle;
      double valueEndingAngle = valueStartingAngle + valueSliceAngle;
      Path2D sector = getSector(center, normalizedRadius, valueStartingAngle, valueEndingAngle);
      g.fill(sector);
    }
    return false;
  }
}
