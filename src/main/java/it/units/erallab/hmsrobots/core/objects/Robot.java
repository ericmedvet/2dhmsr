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
package it.units.erallab.hmsrobots.core.objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.Actionable;
import it.units.erallab.hmsrobots.core.controllers.Controller;
import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.geometry.Shape;
import it.units.erallab.hmsrobots.core.snapshots.RobotShape;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import it.units.erallab.hmsrobots.core.snapshots.VoxelPoly;
import it.units.erallab.hmsrobots.util.Grid;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.joint.Joint;
import org.dyn4j.dynamics.joint.WeldJoint;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Robot implements Actionable, Serializable, WorldObject, Snapshottable, Shape {

  @JsonProperty
  private final Controller controller;
  @JsonProperty
  private final Grid<Voxel> voxels;

  private transient List<Joint<Body>> joints;

  @JsonCreator
  public Robot(
      @JsonProperty("controller") Controller controller, @JsonProperty("voxels") Grid<Voxel> voxels
  ) {
    this.controller = controller;
    this.voxels = voxels;
    reset();
  }

  private static Joint<Body> join(Body body1, Body body2) {
    return new WeldJoint<>(body1, body2, new Vector2(
        (body1.getWorldCenter().x + body1.getWorldCenter().x) / 2d,
        (body1.getWorldCenter().y + body1.getWorldCenter().y) / 2d
    ));
  }

  @Override
  public void act(final double t) {
    voxels.values().stream().filter(Objects::nonNull).forEach(v -> v.act(t));
    controller.control(t, voxels);
  }

  @Override
  public void addTo(World<Body> world) {
    for (Voxel voxel : voxels.values()) {
      if (voxel != null) {
        voxel.addTo(world);
      }
    }
    for (Joint<Body> joint : joints) {
      world.addJoint(joint);
    }
  }

  private void assemble() {
    joints = new ArrayList<>();
    //translate voxels
    for (int gx = 0; gx < voxels.getW(); gx++) {
      for (int gy = 0; gy < voxels.getH(); gy++) {
        Voxel voxel = voxels.get(gx, gy);
        if (voxel != null) {
          voxel.setOwner(this);
          voxel.translate(new Vector2((double) gx * voxel.getSideLength(), (double) gy * voxel.getSideLength()));
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

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Override
  public BoundingBox boundingBox() {
    return voxels.values().stream().filter(Objects::nonNull).map(Voxel::boundingBox).reduce(BoundingBox::largest).get();
  }

  @Override
  public Point2 center() {
    return Point2.average(voxels.values().stream().filter(Objects::nonNull).map(Voxel::center).toArray(Point2[]::new));
  }

  @Override
  public double area() {
    return voxels.values().stream().filter(Objects::nonNull).mapToDouble(Voxel::area).sum();
  }

  public Controller getController() {
    return controller;
  }

  @Override
  public Snapshot getSnapshot() {
    Grid<Snapshot> voxelSnapshots = Grid.create(voxels, v -> v == null ? null : v.getSnapshot());
    Snapshot snapshot = new Snapshot(new RobotShape(Grid.create(
        voxelSnapshots,
        s -> s == null ? null : ((VoxelPoly) s.getContent())
    ), boundingBox()), getClass());
    if (controller instanceof Snapshottable) {
      snapshot.getChildren().add(((Snapshottable) controller).getSnapshot());
    }
    snapshot.getChildren().addAll(voxelSnapshots.values().stream().filter(Objects::nonNull).toList());
    return snapshot;
  }

  public Grid<Voxel> getVoxels() {
    return voxels;
  }

  @Serial
  private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
    ois.defaultReadObject();
    reset();
  }

  @Override
  public void reset() {
    voxels.values().stream().filter(Objects::nonNull).forEach(Voxel::reset);
    assemble();
    controller.reset();
  }

  @Override
  public String toString() {
    return "Robot{" + "controller=" + controller + ", voxels=" + voxels + '}';
  }

  public void translate(Vector2 v) {
    for (Voxel voxel : voxels.values()) {
      if (voxel != null) {
        voxel.translate(v);
      }
    }
  }
}
