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
package it.units.erallab.hmsrobots.objects.immutable;

import it.units.erallab.hmsrobots.sensors.Sensor;
import it.units.erallab.hmsrobots.util.Configuration;

public class ImmutableReading extends ImmutableObject {
  private final double[] values;
  private final Sensor.Domain[] domains;
  private final Configuration<? extends Sensor> configuration;
  private final int index;
  private final int nOfSensors;

  public ImmutableReading(Object object, Shape shape, double[] values, Sensor.Domain[] domains, Configuration<? extends Sensor> configuration, int index, int nOfSensors) {
    super(object, shape);
    this.values = values;
    this.domains = domains;
    this.configuration = configuration;
    this.index = index;
    this.nOfSensors = nOfSensors;
  }

  public double[] getValues() {
    return values;
  }

  public Sensor.Domain[] getDomains() {
    return domains;
  }

  public Configuration<? extends Sensor> getConfiguration() {
    return configuration;
  }

  public int getNOfSensors() {
    return nOfSensors;
  }

  public int getIndex() {
    return index;
  }
}
