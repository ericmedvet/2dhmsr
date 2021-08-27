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
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.core.sensors.Sensor;
import it.units.erallab.hmsrobots.core.snapshots.ScopedReadings;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.VoxelPoly;
import it.units.erallab.hmsrobots.util.Configurable;
import it.units.erallab.hmsrobots.util.ConfigurableField;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.stream.Collectors;


public class SensorReadingsSectorDrawer implements Drawer, Configurable<SensorReadingsSectorDrawer> {

  @ConfigurableField
  private Color fillColor = GraphicsDrawer.alphaed(Color.BLACK, 0.5f);
  @ConfigurableField
  private Color strokeColor = Color.BLACK;
  @ConfigurableField(uiMin = 0f, uiMax = 2f * (float) Math.PI)
  private float spanAngle = (float) Math.PI;
  @ConfigurableField(uiMin = 0.001f * (float) Math.PI, uiMax = 0.5f * (float) Math.PI)
  private float angleResolution = 0.01f * (float) Math.PI;
  @ConfigurableField
  private boolean sensorFrame = true;
  @ConfigurableField
  private boolean rotated = true;

  public static SensorReadingsSectorDrawer build() {
    return new SensorReadingsSectorDrawer();
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
  public void draw(double t, List<Snapshot> lineage, Graphics2D g) {
    Snapshot last = lineage.get(lineage.size() - 1);
    if (!Drawer.match(last, VoxelPoly.class, SensingVoxel.class)) {
      return;
    }
    VoxelPoly voxelPoly = (VoxelPoly) last.getContent();
    List<ScopedReadings> readings = last.getChildren().stream()
        .filter(s -> s.getContent() instanceof ScopedReadings)
        .map(s -> (ScopedReadings) s.getContent())
        .collect(Collectors.toList());
    if (readings.isEmpty()) {
      return;
    }
    double radius = Math.sqrt(voxelPoly.area()) / 2d;
    Point2 center = voxelPoly.center();
    double voxelAngle = Math.atan2((voxelPoly.getVertexes()[1].y - voxelPoly.getVertexes()[0].y), (voxelPoly.getVertexes()[1].x - voxelPoly.getVertexes()[0].x)) / 2d +
        Math.atan2((voxelPoly.getVertexes()[2].y - voxelPoly.getVertexes()[3].y), (voxelPoly.getVertexes()[2].x - voxelPoly.getVertexes()[3].x)) / 2d;
    double angle = rotated ? voxelAngle : 0d;
    double sensorSliceAngle = spanAngle / (double) readings.size();
    for (int i = 0; i < readings.size(); i++) {
      double sensorStartingAngle = angle + (double) i * sensorSliceAngle;
      double valueSliceAngle = sensorSliceAngle / (double) readings.get(i).getReadings().length;
      if (sensorFrame) {
        g.setColor(strokeColor);
        Path2D sector = getSector(center, radius, sensorStartingAngle, sensorStartingAngle + sensorSliceAngle);
        g.draw(sector);
      }
      g.setColor(fillColor);
      for (int j = 0; j < readings.get(i).getReadings().length; j++) {
        double value = readings.get(i).getReadings()[j];
        Sensor.Domain d = readings.get(i).getDomains()[j];
        double normalizedRadius = radius * Math.min(1d, Math.max(0d, (value - d.getMin()) / (d.getMax() - d.getMin())));
        double valueStartingAngle = sensorStartingAngle + (double) j * valueSliceAngle;
        double valueEndingAngle = valueStartingAngle + valueSliceAngle;
        Path2D sector = getSector(center, normalizedRadius, valueStartingAngle, valueEndingAngle);
        g.fill(sector);
      }
    }
  }
}
