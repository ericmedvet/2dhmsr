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

package it.units.erallab.hmsrobots.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author "Eric Medvet" on 2021/08/12 for 2dhmsr
 */
public class Snapshot {

  private final Object content;
  private final Class<Snapshottable> snapshottableClass;
  private final List<Snapshot> children;

  public Snapshot(Object content, Class<Snapshottable> snapshottableClass) {
    this.content = content;
    this.snapshottableClass = snapshottableClass;
    this.children = new ArrayList<>();
  }

  public List<Snapshot> getChildren() {
    return children;
  }

  public Object getContent() {
    return content;
  }

  public Class<Snapshottable> getSnapshottableClass() {
    return snapshottableClass;
  }
}
