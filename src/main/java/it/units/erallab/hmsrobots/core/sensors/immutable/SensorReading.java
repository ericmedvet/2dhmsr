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
package it.units.erallab.hmsrobots.core.sensors.immutable;

import it.units.erallab.hmsrobots.core.objects.immutable.Immutable;
import it.units.erallab.hmsrobots.core.sensors.Sensor;

public class SensorReading extends Immutable {

  private final double[] values;
  private final Sensor.Domain[] domains;
  private final int sensorIndex;
  private final int nOfSensors;

  public SensorReading(double[] values, Sensor.Domain[] domains, int sensorIndex, int nOfSensors) {
    this.values = values;
    this.domains = domains;
    this.sensorIndex = sensorIndex;
    this.nOfSensors = nOfSensors;
  }

  public double[] getValues() {
    return values;
  }

  public Sensor.Domain[] getDomains() {
    return domains;
  }

  public int getSensorIndex() {
    return sensorIndex;
  }

  public int getnOfSensors() {
    return nOfSensors;
  }
}
