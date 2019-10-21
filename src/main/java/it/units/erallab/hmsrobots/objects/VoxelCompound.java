/*
 * Copyright (C) 2019 Eric Medvet <eric.medvet@gmail.com>
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
package it.units.erallab.hmsrobots.objects;

import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.controllers.Controller;
import it.units.erallab.hmsrobots.objects.immutable.Component;
import it.units.erallab.hmsrobots.objects.immutable.Compound;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.World;
import org.dyn4j.dynamics.joint.Joint;
import org.dyn4j.dynamics.joint.WeldJoint;
import org.dyn4j.geometry.Vector2;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class VoxelCompound implements WorldObject {

  private final List<Joint> joints;
  private final Controller controller;
  private final Grid<Voxel> voxels;
  private final Description description;

  public static class Description implements Serializable {

    private final Grid<Boolean> structure;
    private final Controller controller;
    private final Grid<Voxel.Builder> builderGrid;

    public Description(Grid<Boolean> structure, Controller controller, Grid<Voxel.Builder> builderGrid) {
      this.structure = structure;
      this.controller = controller;
      this.builderGrid = builderGrid;
    }

    public Grid<Boolean> getStructure() {
      return structure;
    }

    public Controller getController() {
      return controller;
    }

    public Grid<Voxel.Builder> getBuilderGrid() {
      return builderGrid;
    }

  }

  public VoxelCompound(double x, double y, Description description) {
    this.description = description;
    this.controller = description.getController();
    joints = new ArrayList<>();
    //construct voxels
    voxels = Grid.create(description.getStructure());
    for (int gx = 0; gx < description.getStructure().getW(); gx++) {
      for (int gy = 0; gy < description.getStructure().getH(); gy++) {
        if (description.getStructure().get(gx, gy)) {
          Voxel voxel = description.getBuilderGrid().get(gx, gy).build(
                  x + (double) gx * description.getBuilderGrid().get(gx, gy).getSideLength(),
                  y + gy * description.getBuilderGrid().get(gx, gy).getSideLength(),
                  this
          );
          voxels.set(gx, gy, voxel);
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
  public Compound getSnapshot() {
    List<Component> components = new ArrayList<>();
    for (Voxel voxel : voxels.values()) {
      if (voxel != null) {
        components.addAll(voxel.getSnapshot().getComponents());
      }
    }
    return new Compound(VoxelCompound.class, components);
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

  public Grid<Double> control(double t, double dt) {
    Grid<Double> forceGrid = null;
    if (controller != null) {
      forceGrid = controller.control(t, dt, voxels);
      for (Grid.Entry<Double> entry : forceGrid) {
        if (entry.getValue() != null) {
          if (voxels.get(entry.getX(), entry.getY()) != null) {
            voxels.get(entry.getX(), entry.getY()).applyForce(entry.getValue());
          }
        }
      }
    }
    return forceGrid;
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

  public Grid<Voxel> getVoxels() {
    return voxels;
  }

  public Description getDescription() {
    return description;
  }

}
