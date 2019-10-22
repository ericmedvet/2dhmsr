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
package it.units.erallab.hmsrobots.validation;

import it.units.erallab.hmsrobots.Snapshot;
import it.units.erallab.hmsrobots.objects.Ground;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.objects.VoxelCompound;
import it.units.erallab.hmsrobots.objects.WorldObject;
import it.units.erallab.hmsrobots.objects.immutable.Point2;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.viewers.OnlineViewer;
import it.units.erallab.hmsrobots.viewers.SnapshotListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.dyn4j.dynamics.World;
import org.dyn4j.dynamics.joint.WeldJoint;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class CantileverBending {

  private final static double WALL_MARGIN = 10d;

  private final Grid<Voxel.Builder> builderGrid;
  private final double force;
  private final SnapshotListener listener;

  public CantileverBending(Grid<Voxel.Builder> builderGrid, double force, SnapshotListener listener) {
    this.builderGrid = builderGrid;
    this.force = force;
    this.listener = listener;
  }

  public VoxelCompound call() {
    List<WorldObject> worldObjects = new ArrayList<>();
    //build voxel compound
    VoxelCompound vc = new VoxelCompound(0, 0, new VoxelCompound.Description(
            Grid.create(builderGrid.getW(), builderGrid.getH(), true), null, builderGrid
    ));
    Point2[] boundingBox = vc.boundingBox();
    worldObjects.add(vc);
    //build ground
    Ground ground = new Ground(new double[]{0, 1}, new double[]{0, boundingBox[1].y - boundingBox[0].y + 2d * WALL_MARGIN});
    worldObjects.add(ground);
    //build world w/o gravity
    World world = new World();
    world.setGravity(new Vector2(0d, 0d));
    for (WorldObject worldObject : worldObjects) {
      worldObject.addTo(world);
    }
    //attach vc to ground
    vc.translate(new Vector2(-boundingBox[0].x + 1d, (boundingBox[1].y - boundingBox[0].y + 2d * WALL_MARGIN) / 2d - 1d));
    for (int y = 0; y < vc.getVoxels().getH(); y++) {
      for (int i : new int[]{0, 3}) {
        WeldJoint joint = new WeldJoint(
                ground.getBodies().get(0),
                vc.getVoxels().get(0, y).getVertexBodies()[i],
                vc.getVoxels().get(0, y).getVertexBodies()[i].getWorldCenter()
        );
        world.addJoint(joint);
      }
    }
    //simulate
    double dt = 0.01d;
    double t = 0d;
    while (t < 90d) {
      //add force
      for (int y = 0; y < vc.getVoxels().getH(); y++) {
        for (int i : new int[]{1, 2}) {
          vc.getVoxels().get(vc.getVoxels().getW() - 1, y).getVertexBodies()[i].applyForce(new Vector2(0d, -force / 2d));
        }
      }
      //do step
      t = t + dt;
      world.update(dt);
      if (listener != null) {
        Snapshot snapshot = new Snapshot(t, worldObjects.stream().map(WorldObject::getSnapshot).collect(Collectors.toList()));;
        listener.listen(snapshot);
      }
    }
    return vc;
  }

  public static void main(String[] args) {
    OnlineViewer viewer = new OnlineViewer(Executors.newScheduledThreadPool(2));
    viewer.start();
    CantileverBending cb = new CantileverBending(
            Grid.create(10, 2, Voxel.Builder.create()
                    .springD(0.00001d)
                    .springF(20d)
                    .mass(5d)
                    //.springScaffoldings(EnumSet.of(Voxel.SpringScaffolding.CENTRAL_CROSS, Voxel.SpringScaffolding.SIDE_EXTERNAL))
                    .ropeJointsFlag(false)
            ),
            50d, viewer
    );
    cb.call();
  }

}
