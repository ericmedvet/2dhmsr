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

import it.units.erallab.hmsrobots.objects.snapshot.Component;
import it.units.erallab.hmsrobots.objects.snapshot.Compound;
import it.units.erallab.hmsrobots.objects.snapshot.Poly;
import java.util.ArrayList;
import java.util.List;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.World;
import org.dyn4j.dynamics.joint.DistanceJoint;
import org.dyn4j.dynamics.joint.Joint;
import org.dyn4j.dynamics.joint.WeldJoint;
import org.dyn4j.geometry.Vector2;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class VoxelCompound implements WorldObject {

  private final List<Voxel> voxels;
  private final List<Joint> joints;

  public VoxelCompound(double x, double y, boolean[][] grid, double mass) {
    voxels = new ArrayList<>();
    joints = new ArrayList<>();
    //construct voxels
    int w = grid[0].length;
    int h = grid.length;
    Voxel[][] gridVoxels = new Voxel[h][w];
    for (int gx = 0; gx < w; gx++) {
      for (int gy = 0; gy < h; gy++) {
        if (grid[gy][gx]) {
          Voxel voxel = new Voxel(x + (double) gx * Voxel.SIDE_LENGHT, y + gy * Voxel.SIDE_LENGHT, mass);
          gridVoxels[gy][gx] = voxel;
          voxels.add(voxel);
          //check for adjacent voxels
          if ((gx > 0) && (gridVoxels[gy][gx - 1] != null)) {
            Voxel adjacent = gridVoxels[gy][gx - 1];
            joints.add(join(voxel.getVertexBodies()[0], adjacent.getVertexBodies()[1]));
            joints.add(join(voxel.getVertexBodies()[3], adjacent.getVertexBodies()[2]));
          }
          if ((gy > 0) && (gridVoxels[gy - 1][gx] != null)) {
            Voxel adjacent = gridVoxels[gy - 1][gx];
            joints.add(join(voxel.getVertexBodies()[3], adjacent.getVertexBodies()[0]));
            joints.add(join(voxel.getVertexBodies()[2], adjacent.getVertexBodies()[1]));
          }
        }
      }
    }
  }    

  public VoxelCompound(double x, double y, String grid, double mass) {
    this(x, y, fromString(grid), mass);
  }
  
  private static Joint join(Body body1, Body body2) {
    WeldJoint joint = new WeldJoint(body1, body2, new Vector2(
            (body1.getWorldCenter().x+body1.getWorldCenter().x)/2d,
            (body1.getWorldCenter().y+body1.getWorldCenter().y)/2d
    ));
    return joint;
  }

  private static boolean[][] fromString(String s) {
    String[] rows = s.split(",");
    boolean[][] grid = new boolean[rows.length][rows[0].length()];
    for (int x = 0; x < rows.length; x++) {
      for (int y = 0; y < Math.min(rows[0].length(), rows[x].length()); y++) {
        grid[x][y] = rows[x].charAt(y) != ' ';
      }
    }
    return grid;
  }

  @Override
  public Compound getSnapshot() {
    List<Component> components = new ArrayList<>();
    for (Voxel voxel : voxels) {
      components.addAll(voxel.getSnapshot().getComponents());
    }
    return new Compound(Voxel.class, components);
  }

  @Override
  public void addTo(World world) {
    for (Voxel voxel : voxels) {
      voxel.addTo(world);
    }
    for (Joint joint : joints) {
      world.addJoint(joint);
    }
  }

}
