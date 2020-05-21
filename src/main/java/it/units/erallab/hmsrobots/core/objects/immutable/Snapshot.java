/*
 * Copyright (C) 2020 Eric Medvet <eric.medvet@gmail.com> (as eric)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.core.objects.immutable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Snapshot implements Serializable, Comparable<Snapshot> {
  
  private final double time;
  private final Collection<Immutable> objects;

  public Snapshot(double time, Collection<Immutable> objects) {
    this.time = time;
    this.objects = Collections.unmodifiableCollection(objects);
  }

  public double getTime() {
    return time;
  }

  public Collection<Immutable> getObjects() {
    return objects;
  }

  @Override
  public int compareTo(Snapshot other) {
    return Double.compare(time, other.time);
  }
  
}
