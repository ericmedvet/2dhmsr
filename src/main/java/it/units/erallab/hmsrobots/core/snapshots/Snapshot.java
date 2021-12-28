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

package it.units.erallab.hmsrobots.core.snapshots;

import java.util.ArrayList;
import java.util.List;

/**
 * @author "Eric Medvet" on 2021/08/12 for 2dhmsr
 */
public class Snapshot {

  private final Object content;
  private final Class<? extends Snapshottable> snapshottableClass;
  private final List<Snapshot> children;

  public Snapshot(Object content, Class<? extends Snapshottable> snapshottableClass) {
    this.content = content;
    this.snapshottableClass = snapshottableClass;
    this.children = new ArrayList<>();
  }

  public static Snapshot world(List<Snapshot> snapshots) {
    Snapshottable snapshottable = () -> world(List.of());
    Snapshot snapshot = new Snapshot(new Object(), snapshottable.getClass());
    snapshot.getChildren().addAll(snapshots);
    return snapshot;
  }

  public List<Snapshot> getChildren() {
    return children;
  }

  public Object getContent() {
    return content;
  }

  public Class<? extends Snapshottable> getSnapshottableClass() {
    return snapshottableClass;
  }
}
