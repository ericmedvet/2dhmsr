/*
 * Copyright (C) 2020 Eric Medvet <eric.medvet@gmail.com> (as Eric Medvet <eric.medvet@gmail.com>)
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
package it.units.erallab.hmsrobots.core.objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.sensors.Sensor;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import org.apache.commons.lang3.ArrayUtils;

import java.util.EnumSet;
import java.util.List;

public class SensingVoxel extends ControllableVoxel {

  @JsonProperty
  private final List<Sensor> sensors;

  @JsonCreator
  public SensingVoxel(
      @JsonProperty("sideLength") double sideLength,
      @JsonProperty("massSideLengthRatio") double massSideLengthRatio,
      @JsonProperty("springF") double springF,
      @JsonProperty("springD") double springD,
      @JsonProperty("massLinearDamping") double massLinearDamping,
      @JsonProperty("massAngularDamping") double massAngularDamping,
      @JsonProperty("friction") double friction,
      @JsonProperty("restitution") double restitution,
      @JsonProperty("mass") double mass,
      @JsonProperty("limitContractionFlag") boolean limitContractionFlag,
      @JsonProperty("massCollisionFlag") boolean massCollisionFlag,
      @JsonProperty("areaRatioMaxDelta") double areaRatioMaxDelta,
      @JsonProperty("springScaffoldings") EnumSet<SpringScaffolding> springScaffoldings,
      @JsonProperty("maxForce") double maxForce,
      @JsonProperty("forceMethod") ForceMethod forceMethod,
      @JsonProperty("sensors") List<Sensor> sensors
  ) {
    super(sideLength, massSideLengthRatio, springF, springD, massLinearDamping, massAngularDamping, friction, restitution, mass, limitContractionFlag, massCollisionFlag, areaRatioMaxDelta, springScaffoldings, maxForce, forceMethod);
    this.sensors = sensors;
  }

  public SensingVoxel(double maxForce, ForceMethod forceMethod, List<Sensor> sensors) {
    super(maxForce, forceMethod);
    this.sensors = sensors;
  }

  public SensingVoxel(List<Sensor> sensors) {
    this.sensors = sensors;
  }

  @Override
  public void act(double t) {
    super.act(t);
    sensors.forEach(s -> s.act(t));
  }

  @Override
  public void reset() {
    super.reset();
    sensors.forEach(s -> {
      s.setVoxel(this);
      s.reset();
    });
  }

  public double[] getSensorReadings() {
    return sensors.stream()
        .map(Sensor::getReadings)
        .reduce(ArrayUtils::addAll)
        .orElse(new double[sensors.stream().mapToInt(s -> s.getDomains().length).sum()]);
  }

  public List<Sensor> getSensors() {
    return sensors;
  }

  @Override
  protected void fillSnapshot(Snapshot snapshot) {
    super.fillSnapshot(snapshot);
    //add sensors
    for (Sensor sensor : sensors) {
      snapshot.getChildren().add(sensor.getSnapshot());
    }
  }

  @Override
  public String toString() {
    return "SensingVoxel{" +
        "sensors=" + sensors +
        '}';
  }
}
