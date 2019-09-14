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

import it.units.erallab.hmsrobots.util.TimeAccumulator;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.controllers.PhaseSin;
import it.units.erallab.hmsrobots.objects.Ground;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.objects.VoxelCompound;
import it.units.erallab.hmsrobots.objects.WorldObject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.dyn4j.collision.AxisAlignedBounds;
import org.dyn4j.dynamics.World;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Experimenter {

  private final static Logger L = Logger.getLogger(Experimenter.class.getName());

  public static void main(String[] args) throws FileNotFoundException {

    World world = new World();
    List<WorldObject> worldObjects = new ArrayList<>();
    Ground ground = new Ground(new double[]{0, 1, 999, 1000}, new double[]{25, 0, 0, 25});
    ground.addTo(world);
    worldObjects.add(ground);
    int wormW = 5;
    int wormH = 5;
    Grid<Double> wormController = Grid.create(wormW, wormH, 0d);
    for (int x = 0; x < wormW; x++) {
      for (int y = 0; y < wormH; y++) {
        wormController.set(x, y, (double) x / (double) wormH * 1 * Math.PI);
      }
    }
    Grid<Boolean> wormShape = Grid.create(wormW, wormH, true);
    VoxelCompound vc2 = new VoxelCompound(
            50, 10,
            wormShape,
            new PhaseSin(1d, 1d, wormController),
            Voxel.Builder.create()
    );
    vc2.addTo(world);
    worldObjects.add(vc2);

    List<Snapshot> events = new ArrayList<>();
    File file = new File("/home/eric/experiments/2dhmsr/prova30s-dense.serial");
    double dt = 0.01d;
    TimeAccumulator t = new TimeAccumulator();
    while (t.getT()<30) {
      t.add(dt);
      vc2.control(t.getT(), dt);
      world.update(dt);
      events.add(new Snapshot(t.getT(), worldObjects.stream().map(WorldObject::getSnapshot).collect(Collectors.toList())));
    }
    writeAll(events, file);
    System.out.println("done");
  }

  private static void writeAll(List<Snapshot> events, File file) {
    try (final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file, false))) {
      L.fine(String.format("Writing %d events on %s", events.size(), file));
      oos.writeObject(events);
      events.clear();
    } catch (FileNotFoundException ex) {
      L.log(Level.SEVERE, String.format("Cannot serialize on file %s", file), ex);
    } catch (IOException ex) {
      L.log(Level.SEVERE, String.format("Cannot open object stream on file %s", file), ex);
    }
  }


}
