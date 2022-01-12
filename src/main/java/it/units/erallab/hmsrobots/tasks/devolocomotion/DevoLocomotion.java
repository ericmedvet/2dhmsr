/*
 * Copyright (C) 2021 Giorgia Nadizar <giorgia.nadizar@gmail.com> (as Giorgia Nadizar)
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

package it.units.erallab.hmsrobots.tasks.devolocomotion;

import it.units.erallab.hmsrobots.core.objects.Ground;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.snapshots.SnapshotListener;
import it.units.erallab.hmsrobots.tasks.AbstractTask;
import it.units.erallab.hmsrobots.tasks.locomotion.Locomotion;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * @author "Giorgia Nadizar" on 2021/12/04 for VSREvolution
 */
public abstract class DevoLocomotion extends AbstractTask<UnaryOperator<Robot>, DevoOutcome> {

  protected final double maxT;
  protected final double[][] groundProfile;
  protected final double initialPlacement;

  public DevoLocomotion(double maxT, double[][] groundProfile, double initialPlacement, Settings settings) {
    super(settings);
    this.maxT = maxT;
    this.groundProfile = groundProfile;
    this.initialPlacement = initialPlacement;
  }

  @Override
  public abstract DevoOutcome apply(UnaryOperator<Robot> solution, SnapshotListener listener);

  protected void rebuildWorld(Ground ground, Robot robot, World<Body> world, double newMinX) {
    ground.addTo(world);
    robot.addTo(world);
    //position robot: translate on x
    robot.translate(new Vector2(newMinX - robot.boundingBox().min().x(), 0));
    //translate on y
    double minYGap = robot.getVoxels().values().stream()
        .filter(Objects::nonNull)
        .mapToDouble(v -> v.boundingBox().min().y() - ground.yAt(v.center().x()))
        .min().orElse(0d);
    robot.translate(new Vector2(0, Locomotion.INITIAL_PLACEMENT_Y_GAP - minYGap));
  }

}
