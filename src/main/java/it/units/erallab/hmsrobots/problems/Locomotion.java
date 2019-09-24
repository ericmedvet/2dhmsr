/*
 * Copyright (c) 2019 Eric Medvet <eric.medvet@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.units.erallab.hmsrobots.problems;

import it.units.erallab.hmsrobots.Snapshot;
import it.units.erallab.hmsrobots.objects.Ground;
import it.units.erallab.hmsrobots.objects.VoxelCompound;
import it.units.erallab.hmsrobots.objects.WorldObject;
import it.units.erallab.hmsrobots.objects.immutable.Component;
import it.units.erallab.hmsrobots.objects.immutable.Compound;
import it.units.erallab.hmsrobots.objects.immutable.Point2;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Locomotion implements Episode<VoxelCompound> {

  public static enum Metric {
    CENTER_FINAL_X, CENTER_AVG_Y
  }

  private final double finalT;
  private final double[][] groundProfile;
  private final Metric[] metrics;

  private World world;
  private List<WorldObject> worldObjects;
  private List<Point2> centerPositions;
  private double t;
  private VoxelCompound voxelCompound;

  public Locomotion(double finalT, double[][] groundProfile, Metric[] metrics) {
    this.finalT = finalT;
    this.groundProfile = groundProfile;
    this.metrics = metrics;
  }

  @Override
  public void init(VoxelCompound voxelCompound) {
    centerPositions = new ArrayList<>();
    //init time
    t = 0;
    //init world
    world = new World();
    worldObjects = new ArrayList<>();
    Ground ground = new Ground(groundProfile[0], groundProfile[1]);
    ground.addTo(world);
    worldObjects.add(ground);
    //position robot: x of rightmost point is on 2nd point of profile
    this.voxelCompound = voxelCompound;
    Point2[] boundingBox = boundingBox(voxelCompound);
    double xLeft = groundProfile[0][1];
    double yGroundLeft = groundProfile[1][1];
    double xRight = xLeft + boundingBox[1].x - boundingBox[0].x;
    double yGroundRight = yGroundLeft + (groundProfile[1][2] - yGroundLeft) * (xRight - xLeft)/(groundProfile[0][2]-xLeft);
    double topmostGroundY = Math.max(yGroundLeft, yGroundRight);
    Vector2 targetPoint = new Vector2(xLeft, topmostGroundY + 1d);
    Vector2 currentPoint = new Vector2(boundingBox[0].x, boundingBox[0].y);
    Vector2 movement = targetPoint.subtract(currentPoint);
    voxelCompound.translate(movement);
    //add robot to world
    voxelCompound.addTo(world);
    worldObjects.add(voxelCompound);
  }

  @Override
  public Snapshot step(double dt, boolean withSnapshot) {
    t = t + dt;
    voxelCompound.control(t, dt);
    world.update(dt);
    centerPositions.add(new Point2(voxelCompound.getCenter()));
    Snapshot snapshot = null;
    if (withSnapshot) {
      snapshot = new Snapshot(t, worldObjects.stream().map(WorldObject::getSnapshot).collect(Collectors.toList()));
    }
    return snapshot;
  }

  @Override
  public boolean isDone() {
    return t >= finalT;
  }

  @Override
  public double[] getMetrics() {
    double[] values = new double[metrics.length];
    for (int i = 0; i<metrics.length; i++) {
      if (metrics[i].equals(Metric.CENTER_FINAL_X)) {
        values[i] = voxelCompound.getCenter().x;
      } else if (metrics[i].equals(Metric.CENTER_AVG_Y)) {
        values[i] = centerPositions.stream().mapToDouble((p) -> p.y).average().getAsDouble();
      }
    }
    return new double[0];
  }

  private Point2[] boundingBox(VoxelCompound voxelCompound) {
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    Compound compound = voxelCompound.getSnapshot();
    for (Component component : compound.getComponents()) {
      for (Point2 p : component.getPoly().getVertexes()) {
        if (p.x < minX) {
          minX = p.x;
        }
        if (p.y < minY) {
          minY = p.y;
        }
        if (p.x > maxX) {
          maxX = p.x;
        }
        if (p.y > maxY) {
          maxY = p.y;
        }
      }
    }
    return new Point2[]{new Point2(minX, minY), new Point2(maxX, maxY)};
  }

}
