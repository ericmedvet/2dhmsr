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

import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.util.DoubleRange;

/**
 * @author "Eric Medvet" on 2021/08/13 for 2dhmsr
 */
public abstract class CompositeSensor extends AbstractSensor {
  @JsonProperty
  protected final Sensor sensor;

  public CompositeSensor(
      DoubleRange[] domains,
      Sensor sensor
  ) {
    super(domains);
    this.sensor = sensor;
  }

  @Override
  public void act(double t) {
    sensor.act(t);
    readings = sense(t);
  }

  @Override
  public Snapshot getSnapshot() {
    Snapshot snapshot = super.getSnapshot();
    snapshot.getChildren().add(sensor.getSnapshot());
    return snapshot;
  }

  @Override
  public void setVoxel(Voxel voxel) {
    super.setVoxel(voxel);
    sensor.setVoxel(voxel);
  }

  @Override
  public void reset() {
    super.reset();
    sensor.reset();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "sensor=" + sensor +
        '}';
  }

  public Sensor getInnermostSensor() {
    return sensor instanceof CompositeSensor cs ? cs.getInnermostSensor() : sensor;
  }

  public Sensor getSensor() {
    return sensor;
  }

}
