/*
 * Copyright (c) "Eric Medvet" 2021.
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

/**
 * @author "Eric Medvet" on 2021/08/29 for 2dhmsr
 */
public abstract class RecursiveDrawer implements Drawer {

  public enum TestOutcome {STOP, SKIP, DRAW}

  public interface Filter {
    TestOutcome test(Snapshot snapshot, int index);

    static Filter matches(Class<?> contentClass, Class<? extends Snapshottable> snapshottableClass, Integer index) {
      return (snapshot, i) -> {
        boolean matches =
            (contentClass == null || contentClass.isAssignableFrom(snapshot.getContent().getClass())) &&
                (snapshottableClass == null || snapshottableClass.isAssignableFrom(snapshot.getSnapshottableClass())) &&
                (index == null || index == i);
        return matches ? TestOutcome.DRAW : TestOutcome.SKIP;
      };
    }
  }

  private final Filter filter;

  public RecursiveDrawer(Filter filter) {
    this.filter = filter;
  }

  @Override
  public void draw(double t, Snapshot snapshot, Graphics2D g) {
    recursiveDraw(t, snapshot, 0, g);
  }

  private void recursiveDraw(double t, Snapshot snapshot, int index, Graphics2D g) {
    TestOutcome outcome = filter.test(snapshot, index);
    if (outcome.equals(TestOutcome.STOP)) {
      return;
    }
    if (outcome.equals(TestOutcome.SKIP)) {
      for (int i = 0; i < snapshot.getChildren().size(); i++) {
        recursiveDraw(t, snapshot.getChildren().get(i), i, g);
      }
    }
    if (outcome.equals(TestOutcome.DRAW)) {
      if (innerDraw(t, snapshot, g)) {
        for (int i = 0; i < snapshot.getChildren().size(); i++) {
          recursiveDraw(t, snapshot.getChildren().get(i), i, g);
        }
      }
    }
  }

  protected abstract boolean innerDraw(double t, Snapshot snapshot, Graphics2D g);
}
