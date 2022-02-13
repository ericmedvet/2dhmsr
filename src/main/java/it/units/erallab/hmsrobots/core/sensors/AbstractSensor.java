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

package it.units.erallab.hmsrobots.core.sensors;

import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.core.snapshots.ScopedReadings;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.util.DoubleRange;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author "Eric Medvet" on 2021/08/13 for 2dhmsr
 */
public abstract class AbstractSensor implements Sensor, Serializable {
  protected final DoubleRange[] domains;
  protected Voxel voxel;
  protected double[] readings;

  public AbstractSensor(DoubleRange[] domains) {
    this.domains = domains;
  }

  protected abstract double[] sense(double t);

  @Override
  public void act(double t) {
    readings = sense(t);
  }

  @Override
  public DoubleRange[] getDomains() {
    return domains;
  }

  @Override
  public double[] getReadings() {
    return readings;
  }

  @Override
  public Snapshot getSnapshot() {
    return new Snapshot(new ScopedReadings(
        Arrays.copyOf(readings, readings.length),
        Arrays.copyOf(domains, domains.length)
    ), getClass());
  }

  public Voxel getVoxel() {
    return voxel;
  }

  @Override
  public void setVoxel(Voxel voxel) {
    this.voxel = voxel;
  }

  @Override
  public void reset() {
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
