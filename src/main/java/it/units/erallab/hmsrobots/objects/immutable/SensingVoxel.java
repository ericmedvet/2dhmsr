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
import it.units.erallab.hmsrobots.util.Configurable;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class SensingVoxel extends ControllableVoxel {

  private final List<Sensor> sensors;

  public SensingVoxel(double sideLength, double massSideLengthRatio, double springF, double springD, double massLinearDamping, double massAngularDamping, double friction, double restitution, double mass, boolean limitContractionFlag, boolean massCollisionFlag, double areaRatioMaxDelta, EnumSet<SpringScaffolding> springScaffoldings, double maxForce, double areaRatioControlMaxDelta, ForceMethod forceMethod, List<Sensor> sensors) {
    super(sideLength, massSideLengthRatio, springF, springD, massLinearDamping, massAngularDamping, friction, restitution, mass, limitContractionFlag, massCollisionFlag, areaRatioMaxDelta, springScaffoldings, maxForce, areaRatioControlMaxDelta, forceMethod);
    this.sensors = sensors;
  }

  public SensingVoxel(double maxForce, double areaRatioControlMaxDelta, ForceMethod forceMethod, List<Sensor> sensors) {
    super(maxForce, areaRatioControlMaxDelta, forceMethod);
    this.sensors = sensors;
  }

  public SensingVoxel(List<Sensor> sensors) {
    this.sensors = sensors;
  }

  private List<Pair<Sensor, double[]>> lastSensorReadings = Collections.EMPTY_LIST;

  public List<Pair<Sensor, double[]>> sense(double t) {
    List<Pair<Sensor, double[]>> pairs = sensors.stream()
        .map(s -> Pair.of(s, s.sense(this, t)))
        .collect(Collectors.toList());
    lastSensorReadings = pairs;
    return pairs;
  }

  public List<Sensor> getSensors() {
    return sensors;
  }

  @Override
  public ImmutableObject immutable() {
    ImmutableVoxel superImmutable = (ImmutableVoxel) super.immutable();
    //add sensor readings
    int nOfSensors = lastSensorReadings.size();
    List<ImmutableObject> children = new ArrayList<>(superImmutable.getChildren());
    for (int i = 0; i < nOfSensors; i++) {
      Pair<Sensor, double[]> pair = lastSensorReadings.get(i);
      Sensor sensor = pair.getKey();
      children.add(new ImmutableReading(
          sensor,
          superImmutable.getShape(),
          pair.getValue(),
          sensor.domains(),
          (sensor instanceof Configurable) ? ((Configurable) sensor).toConfiguration() : null,
          i,
          nOfSensors
      ));
    }
    return new ImmutableVoxel(
        this,
        superImmutable.getShape(),
        children,
        superImmutable.getRestArea()
    );
  }

}
