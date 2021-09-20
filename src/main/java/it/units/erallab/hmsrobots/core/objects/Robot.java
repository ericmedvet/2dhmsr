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
import it.units.erallab.hmsrobots.core.snapshots.RobotShape;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import it.units.erallab.hmsrobots.core.snapshots.VoxelPoly;
import it.units.erallab.hmsrobots.util.Grid;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.World;
import org.dyn4j.dynamics.joint.Joint;
import org.dyn4j.dynamics.joint.WeldJoint;
import org.dyn4j.geometry.Vector2;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Robot<V extends ControllableVoxel> implements Actionable, Serializable, WorldObject, Snapshottable {

  @JsonProperty
  private final Controller<V> controller;
  @JsonProperty
  private final Grid<? extends V> voxels;

  private transient List<Joint> joints;

  @JsonCreator
  public Robot(
      @JsonProperty("controller") Controller<V> controller,
      @JsonProperty("voxels") Grid<? extends V> voxels
  ) {
    this.controller = controller;
    this.voxels = voxels;
    reset();
  }

  @Serial
  private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
    ois.defaultReadObject();
    reset();
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
    return new WeldJoint(body1, body2, new Vector2(
        (body1.getWorldCenter().x + body1.getWorldCenter().x) / 2d,
        (body1.getWorldCenter().y + body1.getWorldCenter().y) / 2d
    ));
  }

  @Override
  public Snapshot getSnapshot() {
    Grid<Snapshot> voxelSnapshots = Grid.create(voxels, v -> v == null ? null : v.getSnapshot());
    Snapshot snapshot = new Snapshot(
        new RobotShape(
            Grid.create(voxelSnapshots, s -> s == null ? null : ((VoxelPoly) s.getContent())),
            boundingBox()
        ),
        getClass()
    );
    if (controller instanceof Snapshottable) {
      snapshot.getChildren().add(((Snapshottable) controller).getSnapshot());
    }
    snapshot.getChildren().addAll(voxelSnapshots.values().stream().filter(Objects::nonNull).collect(Collectors.toList()));
    return snapshot;
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

  @Override
  public void act(final double t) {
    voxels.values().stream().filter(Objects::nonNull).forEach(v -> v.act(t));
    controller.control(t, voxels);
  }

  @Override
  public void reset() {
    voxels.values().stream().filter(Objects::nonNull).forEach(ControllableVoxel::reset);
    assemble();
    controller.reset();
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

  public BoundingBox boundingBox() {
    return voxels.values().stream()
        .filter(Objects::nonNull)
        .map(Voxel::boundingBox)
        .reduce(BoundingBox::largest)
        .get();
  }

  public Controller<V> getController() {
    return controller;
  }

  public Grid<? extends V> getVoxels() {
    return voxels;
  }

  @Override
  public String toString() {
    return "Robot{" +
        "controller=" + controller +
        ", voxels=" + voxels +
        '}';
  }
}
