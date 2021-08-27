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

package it.units.erallab.hmsrobots.viewers.drawers;

import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;

import java.awt.*;
import java.util.List;

/**
 * @author "Eric Medvet" on 2021/08/13 for 2dhmsr
 */
public interface Drawer {
  void draw(List<Snapshot> lineage, Graphics2D g);

  static boolean match(Snapshot snapshot, Class<?> contentClass, Class<? extends Snapshottable> creatorClass) {
    return contentClass.isAssignableFrom(snapshot.getContent().getClass()) && creatorClass.isAssignableFrom(snapshot.getSnapshottableClass());
  }

  static Snapshot lastMatching(List<Snapshot> lineage, Class<?> contentClass, Class<? extends Snapshottable> creatorClass) {
    for (int i = lineage.size() - 1; i >= 0; i--) {
      if (match(lineage.get(i), contentClass, creatorClass)) {
        return lineage.get(i);
      }
    }
    return null;
  }
}
