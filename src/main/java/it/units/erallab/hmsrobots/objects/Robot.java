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
package it.units.erallab.hmsrobots.objects;

import it.units.erallab.hmsrobots.controllers.Controller;
import it.units.erallab.hmsrobots.objects.immutable.BoundingBox;
import it.units.erallab.hmsrobots.objects.immutable.ImmutableObject;
import it.units.erallab.hmsrobots.sensors.Sensor;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.lang3.tuple.Pair;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.World;
import org.dyn4j.dynamics.joint.Joint;
import org.dyn4j.dynamics.joint.WeldJoint;
import org.dyn4j.geometry.Vector2;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Robot<V extends Voxel> implements WorldObject, Serializable {

  private final Controller controller;
  private final Grid<V> voxels;

  private transient List<Joint> joints;

  public Robot(Controller controller, Grid<V> voxels) {
    this.controller = controller;
    this.voxels = voxels;
    assemble();
  }

  private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
    ois.defaultReadObject();
    assemble();
  }

  private void assemble() {
    joints = new ArrayList<>();
    //translate voxels
    for (int gx = 0; gx < voxels.getW(); gx++) {
      for (int gy = 0; gy < voxels.getH(); gy++) {
        Voxel voxel = voxels.get(gx, gy);
        if (voxel != null) {
          voxel.setOwner(this);
          voxel.translate(new Vector2(
              (double) gx * voxel.getSideLength(),
              (double) gy * voxel.getSideLength()
          ));
          //check for adjacent voxels
          if ((gx > 0) && (voxels.get(gx - 1, gy) != null)) {
            Voxel adjacent = voxels.get(gx - 1, gy);
            joints.add(join(voxel.getVertexBodies()[0], adjacent.getVertexBodies()[1]));
            joints.add(join(voxel.getVertexBodies()[3], adjacent.getVertexBodies()[2]));
          }
          if ((gy > 0) && (voxels.get(gx, gy - 1) != null)) {
            Voxel adjacent = voxels.get(gx, gy - 1);
            joints.add(join(voxel.getVertexBodies()[3], adjacent.getVertexBodies()[0]));
            joints.add(join(voxel.getVertexBodies()[2], adjacent.getVertexBodies()[1]));
          }
        }
      }
    }
  }

  private static Joint join(Body body1, Body body2) {
    WeldJoint joint = new WeldJoint(body1, body2, new Vector2(
        (body1.getWorldCenter().x + body1.getWorldCenter().x) / 2d,
        (body1.getWorldCenter().y + body1.getWorldCenter().y) / 2d
    ));
    return joint;
  }

  @Override
  public ImmutableObject immutable() {
    List<ImmutableObject> children = new ArrayList<>();
    for (Voxel voxel : voxels.values()) {
      if (voxel != null) {
        children.add(voxel.immutable());
      }
    }
    return new ImmutableObject(
        this,
        null,
        children
    );
  }

  @Override
  public void addTo(World world) {
    for (Voxel voxel : voxels.values()) {
      if (voxel != null) {
        voxel.addTo(world);
      }
    }
    for (Joint joint : joints) {
      world.addJoint(joint);
    }
  }

  public Grid<Double> act(final double t) {
    //sense
    Grid<List<Pair<Sensor, double[]>>> sensorsValues = Grid.create(voxels, v -> v == null ? null : v.sense(t));
    //control
    Grid<Double> controlValues = controller.control(t, sensorsValues);
    //apply
    for (Grid.Entry<V> voxelEntry : voxels) {
      if (voxelEntry.getValue() != null) {
        voxelEntry.getValue().applyForce(controlValues.get(voxelEntry.getX(), voxelEntry.getY()));
      }
    }
    return controlValues;
  }

  public Vector2 getCenter() {
    double xc = 0d;
    double yc = 0d;
    double n = 0;
    for (Voxel voxel : voxels.values()) {
      if (voxel != null) {
        final Vector2 center = voxel.getCenter();
        xc = xc + center.x;
        yc = yc + center.y;
        n = n + 1;
      }
    }
    return new Vector2(xc / n, yc / n);
  }

  public void translate(Vector2 v) {
    for (Voxel voxel : voxels.values()) {
      if (voxel != null) {
        voxel.translate(v);
      }
    }
  }

  public Grid<V> getVoxels() {
    return voxels;
  }

  public BoundingBox boundingBox() {
    return immutable().getChildren().stream()
        .map(o -> o.getShape().boundingBox())
        .reduce((b1, b2) -> BoundingBox.largest(b1, b2)).get();
  }

  public Controller getController() {
    return controller;
  }

}
