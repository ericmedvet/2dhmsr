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
package it.units.erallab.hmsrobots.core.objects;

import it.units.erallab.hmsrobots.core.objects.immutable.ControllableVoxel;
import it.units.erallab.hmsrobots.core.objects.immutable.Immutable;
import it.units.erallab.hmsrobots.core.sensors.Sensor;
import org.apache.commons.lang3.tuple.Pair;
import org.dyn4j.dynamics.joint.DistanceJoint;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class BreakableVoxel extends SensingVoxel {

  public enum ComponentType {
    ACTUATOR, SENSORS, STRUCTURE
  }

  public enum MalfunctionType {
    NONE, ZERO, FROZEN, RANDOM
  }

  private final Random random;

  private MalfunctionType actuatorMalfunctionType = MalfunctionType.NONE;
  private MalfunctionType sensorsMalfunctionType = MalfunctionType.NONE;
  private MalfunctionType structureMalfunctionType = MalfunctionType.NONE;
  private List<Pair<Sensor, double[]>> lastSensorReadings;

  public BreakableVoxel(double sideLength, double massSideLengthRatio, double springF, double springD, double massLinearDamping, double massAngularDamping, double friction, double restitution, double mass, boolean limitContractionFlag, boolean massCollisionFlag, double areaRatioMaxDelta, EnumSet<SpringScaffolding> springScaffoldings, double maxForce, ForceMethod forceMethod, List<Sensor> sensors, Random random) {
    super(sideLength, massSideLengthRatio, springF, springD, massLinearDamping, massAngularDamping, friction, restitution, mass, limitContractionFlag, massCollisionFlag, areaRatioMaxDelta, springScaffoldings, maxForce, forceMethod, sensors);
    this.random = random;
  }

  public BreakableVoxel(double maxForce, ForceMethod forceMethod, List<Sensor> sensors, Random random) {
    super(maxForce, forceMethod, sensors);
    this.random = random;
  }

  public BreakableVoxel(List<Sensor> sensors, Random random) {
    super(sensors);
    this.random = random;
  }

  @Override
  public List<Pair<Sensor, double[]>> sense(double t) {
    List<Pair<Sensor, double[]>> sensorReadings = getSensors().stream()
        .map(s -> Pair.of(s, Arrays.stream(s.domains())
            .mapToDouble(d -> (d.getMin() + d.getMax()) / 2d)
            .toArray()
        )).collect(Collectors.toList());
    if (sensorsMalfunctionType.equals(MalfunctionType.NONE)) {
      sensorReadings = super.sense(t);
    } else if (sensorsMalfunctionType.equals(MalfunctionType.FROZEN)) {
      sensorReadings = lastSensorReadings;
    } else if (sensorsMalfunctionType.equals(MalfunctionType.RANDOM)) {
      sensorReadings = getSensors().stream()
          .map(s -> Pair.of(s, random(s.domains())))
          .collect(Collectors.toList());
    }
    lastSensorReadings = sensorReadings;
    return sensorReadings;
  }

  @Override
  public void applyForce(double f) {
    double innerF = f;
    if (actuatorMalfunctionType.equals(MalfunctionType.ZERO)) {
      f = 0;
    } else if (actuatorMalfunctionType.equals(MalfunctionType.FROZEN)) {
      f = getAppliedForce();
    } else if (actuatorMalfunctionType.equals(MalfunctionType.RANDOM)) {
      f = random.nextDouble() * 2d - 1d;
    }
    super.applyForce(f);
  }

  public MalfunctionType getActuatorMalfunctionType() {
    return actuatorMalfunctionType;
  }

  public void setActuatorMalfunctionType(MalfunctionType actuatorMalfunctionType) {
    this.actuatorMalfunctionType = actuatorMalfunctionType;
  }

  public MalfunctionType getSensorsMalfunctionType() {
    return sensorsMalfunctionType;
  }

  public void setSensorsMalfunctionType(MalfunctionType sensorsMalfunctionType) {
    this.sensorsMalfunctionType = sensorsMalfunctionType;
  }

  public MalfunctionType getStructureMalfunctionType() {
    return structureMalfunctionType;
  }

  public void setStructureMalfunctionType(MalfunctionType structureMalfunctionType) {
    this.structureMalfunctionType = structureMalfunctionType;
    if (structureMalfunctionType.equals(MalfunctionType.NONE)) {
      for (DistanceJoint springJoint : springJoints) {
        springJoint.setFrequency(springF);
      }
    } else if (structureMalfunctionType.equals(MalfunctionType.FROZEN)) {
      for (DistanceJoint springJoint : springJoints) {
        springJoint.setFrequency(0d);
        springJoint.setDampingRatio(0d);
      }
    } else {
      throw new IllegalArgumentException("Unsupported structure malfunction type.");
    }
  }

  public MalfunctionType getMalfunctionType(ComponentType componentType) {
    if (componentType.equals(ComponentType.ACTUATOR)) {
      return getActuatorMalfunctionType();
    }
    if (componentType.equals(ComponentType.SENSORS)) {
      return getSensorsMalfunctionType();
    }
    if (componentType.equals(ComponentType.STRUCTURE)) {
      return getStructureMalfunctionType();
    }
    return null;
  }

  public void setMalfunctionType(ComponentType componentType, MalfunctionType malfunctionType) {
    if (componentType.equals(ComponentType.ACTUATOR)) {
      setActuatorMalfunctionType(malfunctionType);
    }
    if (componentType.equals(ComponentType.SENSORS)) {
      setSensorsMalfunctionType(malfunctionType);
    }
    if (componentType.equals(ComponentType.STRUCTURE)) {
      setStructureMalfunctionType(malfunctionType);
    }
  }

  public boolean isBroken() {
    return !actuatorMalfunctionType.equals(MalfunctionType.NONE)
        || !sensorsMalfunctionType.equals(MalfunctionType.NONE)
        || !structureMalfunctionType.equals(MalfunctionType.NONE);
  }

  private double[] random(Sensor.Domain[] domains) {
    double[] values = new double[domains.length];
    for (int i = 0; i < domains.length; i++) {
      values[i] = random.nextDouble() * (domains[i].getMax() - domains[i].getMin()) + domains[i].getMin();
    }
    return values;
  }

  @Override
  public Immutable immutable() {
    ControllableVoxel superImmutable = (ControllableVoxel) super.immutable();
    it.units.erallab.hmsrobots.core.objects.immutable.BreakableVoxel immutable = new it.units.erallab.hmsrobots.core.objects.immutable.BreakableVoxel(
        superImmutable.getShape(),
        superImmutable.getAreaRatio(),
        superImmutable.getAppliedForce(),
        superImmutable.getControlEnergy(),
        superImmutable.getControlEnergyDelta(),
        actuatorMalfunctionType,
        sensorsMalfunctionType,
        structureMalfunctionType
    );
    immutable.getChildren().addAll(superImmutable.getChildren());
    return immutable;
  }
}
