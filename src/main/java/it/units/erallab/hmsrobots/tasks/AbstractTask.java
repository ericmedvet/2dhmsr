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

import it.units.erallab.hmsrobots.core.Actionable;
import it.units.erallab.hmsrobots.core.objects.WorldObject;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.SnapshotListener;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.world.World;

import java.util.List;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public abstract class AbstractTask<T, R> implements Task<T, R> {

  protected final Settings settings;

  public AbstractTask(Settings settings) {
    this.settings = settings;
  }

  protected static double updateWorld(
      final double t,
      final double dT,
      final World<Body> world,
      final List<WorldObject> objects,
      final SnapshotListener listener
  ) {
    double newT = t + dT;
    world.step(1);
    objects.stream().filter(o -> o instanceof Actionable).forEach(o -> ((Actionable) o).act(newT));
    //possibly output snapshot
    if (listener != null) {
      listener.listen(
          newT,
          Snapshot.world(
              objects.stream()
                  .filter(o -> o instanceof Snapshottable)
                  .map(o -> ((Snapshottable) o).getSnapshot())
                  .toList()
          )
      );
    }
    return newT;
  }

  public Settings getSettings() {
    return settings;
  }

}
