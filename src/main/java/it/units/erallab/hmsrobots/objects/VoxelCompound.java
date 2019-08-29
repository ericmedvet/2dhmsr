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
import java.util.ArrayList;
import java.util.List;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.World;
import org.dyn4j.dynamics.joint.Joint;
import org.dyn4j.dynamics.joint.WeldJoint;
import org.dyn4j.geometry.Vector2;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class VoxelCompound implements WorldObject {

  private final List<Joint> joints;
  private final Controller controller;
  private final Grid<Voxel> voxels;

  public VoxelCompound(double x, double y, Grid<Boolean> grid, double mass, Controller controller) {
    this.controller = controller;
    joints = new ArrayList<>();
    //construct voxels
    voxels = Grid.create(grid);
    for (int gx = 0; gx < grid.getW(); gx++) {
      for (int gy = 0; gy < grid.getH(); gy++) {
        if (grid.get(gx, gy)) {
          Voxel voxel = new Voxel(x + (double) gx * Voxel.SIDE_LENGHT, y + gy * Voxel.SIDE_LENGHT, mass);
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

  public VoxelCompound(double x, double y, String grid, double mass, Controller controller) {
    this(x, y, fromString(grid), mass, controller);
  }

  private static Joint join(Body body1, Body body2) {
    WeldJoint joint = new WeldJoint(body1, body2, new Vector2(
            (body1.getWorldCenter().x + body1.getWorldCenter().x) / 2d,
            (body1.getWorldCenter().y + body1.getWorldCenter().y) / 2d
    ));
    return joint;
  }

  private static Grid<Boolean> fromString(String s) {
    String[] rows = s.split(",");
    Grid<Boolean> grid = Grid.create(rows[0].length(), rows.length, false);
    for (int y = 0; y < grid.getH(); y++) {
      for (int x = 0; x < grid.getW(); x++) {
        if (x >= rows[y].length()) {
          grid.set(x, grid.getH() - y - 1, false);
        } else {
          grid.set(x, grid.getH() - y - 1, rows[y].charAt(x) != ' ');
        }
      }
    }
    return grid;
  }

  @Override
  public Compound getSnapshot() {
    List<Component> components = new ArrayList<>();
    for (int x = 0; x < voxels.getW(); x++) {
      for (int y = 0; y < voxels.getH(); y++) {
        Voxel voxel = voxels.get(x, y);
        if (voxel != null) {
          components.addAll(voxel.getSnapshot().getComponents());
        }
      }
    }
    return new Compound(Voxel.class, components);
  }

  @Override
  public void addTo(World world) {
    for (int x = 0; x < voxels.getW(); x++) {
      for (int y = 0; y < voxels.getH(); y++) {
        Voxel voxel = voxels.get(x, y);
        if (voxel != null) {
          voxel.addTo(world);
        }
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
      for (int x = 0; x < voxels.getW(); x++) {
        for (int y = 0; y < voxels.getH(); y++) {
          Voxel voxel = voxels.get(x, y);
          if (voxel != null) {
            voxels.get(x, y).applyForce(forceGrid.get(x, y));
          }
        }
      }
    }
    return forceGrid;
  }

}
