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

import java.awt.*;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * @author "Eric Medvet" on 2021/09/20 for 2dhmsr
 */
public abstract class MemoryDrawer<K> extends SubtreeDrawer {

  protected final double windowT;
  private final Function<Snapshot, K> function;
  private final SortedMap<Double, K> memory;

  public MemoryDrawer(Extractor extractor, Function<Snapshot, K> function, double windowT) {
    super(extractor);
    this.function = function;
    this.windowT = windowT;
    memory = new TreeMap<>();
  }

  protected abstract void innerDraw(double t, Snapshot snapshot, SortedMap<Double, K> memory, Graphics2D g);

  @Override
  protected void innerDraw(double t, Snapshot snapshot, Graphics2D g) {
    K currentReading = function.apply(snapshot);
    memory.put(t, currentReading);
    while (memory.firstKey() < (t - windowT)) {
      memory.remove(memory.firstKey());
    }
    innerDraw(t, snapshot, memory, g);
  }
}
