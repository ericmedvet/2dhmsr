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
import java.util.ArrayList;
import java.util.List;

/**
 * @author "Eric Medvet" on 2021/08/29 for 2dhmsr
 */
public abstract class SubtreeDrawer implements Drawer {

  private final Extractor extractor;

  public SubtreeDrawer(Extractor extractor) {
    this.extractor = extractor;
  }

  @FunctionalInterface
  public interface Extractor {
    List<Snapshot> extract(Snapshot snapshot);

    private static void extract(
        List<Snapshot> snapshots,
        Snapshot s,
        Class<?> contentClass,
        Class<? extends Snapshottable> snapshottableClass,
        Integer index
    ) {
      int c = 0;
      for (int i = 0; i < s.getChildren().size(); i++) {
        if (matches(s.getChildren().get(i), 0, contentClass, snapshottableClass, null)) {
          c = c + 1;
        }
        if (matches(s.getChildren().get(i), c - 1, contentClass, snapshottableClass, index)) {
          snapshots.add(s.getChildren().get(i));
        }
        extract(snapshots, s.getChildren().get(i), contentClass, snapshottableClass, index);
      }
    }

    static Extractor matches(Class<?> contentClass, Class<? extends Snapshottable> snapshottableClass, Integer index) {
      return snapshot -> {
        List<Snapshot> snapshots = new ArrayList<>();
        if (matches(snapshot, 0, contentClass, snapshottableClass, index)) {
          snapshots.add(snapshot);
        }
        extract(snapshots, snapshot, contentClass, snapshottableClass, index);
        return snapshots;
      };
    }

    private static boolean matches(
        Snapshot snapshot,
        int i,
        Class<?> contentClass,
        Class<? extends Snapshottable> snapshottableClass,
        Integer index
    ) {
      return (contentClass == null || contentClass.isAssignableFrom(snapshot.getContent().
          getClass())) &&
          (snapshottableClass == null || snapshottableClass.isAssignableFrom(snapshot.getSnapshottableClass())) &&
          (index == null || index == i);
    }

  }

  protected abstract void innerDraw(double t, Snapshot snapshot, Graphics2D g);

  @Override
  public void draw(double t, Snapshot snapshot, Graphics2D g) {
    extractor.extract(snapshot).forEach(s -> innerDraw(t, s, g));
  }

}
