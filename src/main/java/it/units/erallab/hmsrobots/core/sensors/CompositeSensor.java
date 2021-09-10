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
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.util.Domain;

/**
 * @author "Eric Medvet" on 2021/08/13 for 2dhmsr
 */
public abstract class CompositeSensor extends AbstractSensor {
  @JsonProperty
  protected final Sensor sensor;

  public CompositeSensor(
      Domain[] domains,
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
  public void reset() {
    super.reset();
    sensor.reset();
  }

  @Override
  public void setVoxel(SensingVoxel voxel) {
    super.setVoxel(voxel);
    sensor.setVoxel(voxel);
  }

  @Override
  public Snapshot getSnapshot() {
    Snapshot snapshot = super.getSnapshot();
    snapshot.getChildren().add(sensor.getSnapshot());
    return snapshot;
  }

  public Sensor getSensor() {
    return sensor;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "sensor=" + sensor +
        '}';
  }

}
