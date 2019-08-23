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
package it.units.erallab.hmsrobots;

import it.units.erallab.hmsrobots.controllers.PhaseController;
import it.units.erallab.hmsrobots.objects.Box;
import it.units.erallab.hmsrobots.objects.Ground;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.objects.VoxelCompound;
import it.units.erallab.hmsrobots.objects.WorldObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.dyn4j.collision.AxisAlignedBounds;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.Vector2;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Starter {

  public static void main(String[] args) {

    World world = new World(new AxisAlignedBounds(250, 250));
    //world.setGravity(new Vector2(0, 0));
    List<WorldObject> worldObjects = new ArrayList<>();

    //Ground ground = new Ground(new double[]{0, 2, 4, 40, 50, 63, 100}, new double[]{10, 13, 12, 3, 8, 24, 10});
    Ground ground = new Ground(new double[]{0, 1, 99, 100}, new double[]{25, 0, 0, 25});
    ground.addTo(world);
    worldObjects.add(ground);
    Random r = new Random();
    for (int i = 0; i < 0; i++) {
      Box box = new Box(r.nextDouble() * 100, 30 + 30 * r.nextDouble(), 2 + 5 * r.nextDouble(), 2 + 5 * r.nextDouble(), r.nextDouble() * Math.PI, 1);
      box.addTo(world);
      worldObjects.add(box);
    }

    Voxel v = new Voxel(10, 50, 1);
    v.addTo(world);
    worldObjects.add(v);

    VoxelCompound vc = new VoxelCompound(
            30, 20,
            " **, * , * , * , * , * ,***",
            1,
            new PhaseController(0.5d, 100d, Grid.create(3, 7, 0d))
    );
    vc.addTo(world);
    worldObjects.add(vc);

    ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    Viewer viewer = new Viewer(executor);
    viewer.start();

    double dt = 0.025d;
    TimeAccumulator t = new TimeAccumulator();
    Runnable runnable = () -> {
      try {
        t.add(dt);
        v.applyForce(Math.sin(2d * Math.PI * 0.5d * t.getT()) * 500d);
        vc.control(t.getT(), dt);
        world.update(dt);
        viewer.listen(new WorldEvent(t.getT(), worldObjects.stream().map(WorldObject::getSnapshot).collect(Collectors.toList())));
      } catch (Throwable ex) {
        ex.printStackTrace();
        System.exit(0);
      }
    };
    executor.scheduleAtFixedRate(runnable, 0, Math.round(dt * 1000d / 2d), TimeUnit.MILLISECONDS);
  }

}
