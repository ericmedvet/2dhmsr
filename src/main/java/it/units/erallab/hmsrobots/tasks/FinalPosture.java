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

package it.units.erallab.hmsrobots.tasks;

import it.units.erallab.hmsrobots.behavior.BehaviorUtils;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.core.objects.WorldObject;
import it.units.erallab.hmsrobots.core.snapshots.SnapshotListener;
import it.units.erallab.hmsrobots.util.Grid;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FinalPosture extends AbstractTask<Robot, Grid<Boolean>> {

  private final int gridSize;
  private final double finalT;

  public FinalPosture(int gridSize, double finalT) {
    super(new Settings());
    this.gridSize = gridSize;
    this.finalT = finalT;
  }

  @Override
  public Grid<Boolean> apply(Robot robot, SnapshotListener listener) {
    //init world
    World<Body> world = new World<>();
    world.setSettings(settings);
    world.setGravity(Vector2.create(0d, Math.PI));
    List<WorldObject> worldObjects = new ArrayList<>();
    robot.reset();
    //add robot to world
    robot.addTo(world);
    worldObjects.add(robot);
    //run
    double t = 0d;
    while (t < finalT) {
      t = AbstractTask.updateWorld(t, settings.getStepFrequency(), world, worldObjects, listener);
    }
    //get final posture
    return BehaviorUtils.computePosture(
        robot.getVoxels().values().stream()
            .filter(Objects::nonNull)
            .map(Voxel::getVoxelPoly)
            .toList(),
        gridSize
    );
  }

}
