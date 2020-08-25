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

import java.util.*;
import java.util.stream.Collectors;

public class BreakableVoxel extends SensingVoxel {

  public enum ComponentType {
    ACTUATOR, SENSORS, STRUCTURE
  }

  public enum MalfunctionType {
    NONE, ZERO, FROZEN, RANDOM
  }

  public enum MalfunctionTrigger {
    CONTROL, AREA, TIME
  }

  private final Random random;
  private final Map<ComponentType, Set<MalfunctionType>> malfunctions;
  private final Map<MalfunctionTrigger, Double> triggerThresholds;

  private List<Pair<Sensor, double[]>> lastSensorReadings;
  private final EnumMap<MalfunctionTrigger, Double> triggerCounters;
  private final EnumMap<ComponentType, MalfunctionType> state;
  private double lastControlT = 0d;

  public BreakableVoxel(double sideLength, double massSideLengthRatio, double springF, double springD, double massLinearDamping, double massAngularDamping, double friction, double restitution, double mass, boolean limitContractionFlag, boolean massCollisionFlag, double areaRatioMaxDelta, EnumSet<SpringScaffolding> springScaffoldings, double maxForce, ForceMethod forceMethod, List<Sensor> sensors, Random random, Map<ComponentType, Set<MalfunctionType>> malfunctions, Map<MalfunctionTrigger, Double> triggerThresholds) {
    super(sideLength, massSideLengthRatio, springF, springD, massLinearDamping, massAngularDamping, friction, restitution, mass, limitContractionFlag, massCollisionFlag, areaRatioMaxDelta, springScaffoldings, maxForce, forceMethod, sensors);
    this.random = random;
    this.malfunctions = malfunctions;
    this.triggerThresholds = triggerThresholds;
    triggerCounters = new EnumMap<>(MalfunctionTrigger.class);
    Arrays.stream(MalfunctionTrigger.values()).sequential().forEach(trigger -> triggerCounters.put(trigger, 0d));
    state = new EnumMap<>(ComponentType.class);
    Arrays.stream(ComponentType.values()).sequential().forEach(component -> state.put(component, MalfunctionType.NONE));
  }

  public BreakableVoxel(double maxForce, ForceMethod forceMethod, List<Sensor> sensors, Random random, Map<ComponentType, Set<MalfunctionType>> malfunctions, Map<MalfunctionTrigger, Double> triggerThresholds) {
    super(maxForce, forceMethod, sensors);
    this.random = random;
    this.malfunctions = malfunctions;
    this.triggerThresholds = triggerThresholds;
    triggerCounters = new EnumMap<>(MalfunctionTrigger.class);
    Arrays.stream(MalfunctionTrigger.values()).sequential().forEach(trigger -> triggerCounters.put(trigger, 0d));
    state = new EnumMap<>(ComponentType.class);
    Arrays.stream(ComponentType.values()).sequential().forEach(component -> state.put(component, MalfunctionType.NONE));
  }

  public BreakableVoxel(List<Sensor> sensors, Random random, Map<ComponentType, Set<MalfunctionType>> malfunctions, Map<MalfunctionTrigger, Double> triggerThresholds) {
    super(sensors);
    this.random = random;
    this.malfunctions = malfunctions;
    this.triggerThresholds = triggerThresholds;
    triggerCounters = new EnumMap<>(MalfunctionTrigger.class);
    Arrays.stream(MalfunctionTrigger.values()).sequential().forEach(trigger -> triggerCounters.put(trigger, 0d));
    state = new EnumMap<>(ComponentType.class);
    Arrays.stream(ComponentType.values()).sequential().forEach(component -> state.put(component, MalfunctionType.NONE));
  }

  @Override
  public List<Pair<Sensor, double[]>> sense(double t) {
    //update counters
    triggerCounters.put(MalfunctionTrigger.TIME, triggerCounters.get(MalfunctionTrigger.TIME) + t - lastControlT);
    triggerCounters.put(MalfunctionTrigger.CONTROL, triggerCounters.get(MalfunctionTrigger.CONTROL) + getControlEnergyDelta());
    triggerCounters.put(MalfunctionTrigger.AREA, triggerCounters.get(MalfunctionTrigger.AREA) + getAreaEnergyDelta());
    lastControlT = t;
    //check if malfunction is applicable
    for (Map.Entry<MalfunctionTrigger, Double> triggerThreshold : triggerThresholds.entrySet()) {
      if (random.nextDouble() < 1d - Math.tanh(triggerThreshold.getValue() / triggerCounters.get(triggerThreshold.getKey()))) {
        //reset counters
        Arrays.stream(MalfunctionTrigger.values()).sequential().forEach(trigger -> triggerCounters.put(trigger, 0d));
        //choose component and malfunction
        ComponentType[] componentTypes = malfunctions.keySet().toArray(ComponentType[]::new);
        if (componentTypes.length > 0) {
          ComponentType componentType = componentTypes[random.nextInt(componentTypes.length)];
          MalfunctionType[] malfunctionTypes = malfunctions.get(componentType).toArray(MalfunctionType[]::new);
          MalfunctionType malfunctionType = malfunctionTypes[random.nextInt(malfunctionTypes.length)];
          state.put(componentType, malfunctionType);
          if (componentType.equals(ComponentType.STRUCTURE)) {
            setStructureMalfunctionType(malfunctionType);
          }
        }
      }
    }
    //sense
    List<Pair<Sensor, double[]>> sensorReadings = getSensors().stream()
        .map(s -> Pair.of(s, Arrays.stream(s.domains())
            .mapToDouble(d -> (d.getMin() + d.getMax()) / 2d)
            .toArray()
        )).collect(Collectors.toList());
    if (state.get(ComponentType.SENSORS).equals(MalfunctionType.NONE)) {
      sensorReadings = super.sense(t);
    } else if (state.get(ComponentType.SENSORS).equals(MalfunctionType.FROZEN)) {
      sensorReadings = lastSensorReadings;
    } else if (state.get(ComponentType.SENSORS).equals(MalfunctionType.RANDOM)) {
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
    if (state.get(ComponentType.ACTUATOR).equals(MalfunctionType.ZERO)) {
      f = 0;
    } else if (state.get(ComponentType.ACTUATOR).equals(MalfunctionType.FROZEN)) {
      f = getAppliedForce();
    } else if (state.get(ComponentType.ACTUATOR).equals(MalfunctionType.RANDOM)) {
      f = random.nextDouble() * 2d - 1d;
    }
    super.applyForce(f);
  }

  private void setStructureMalfunctionType(MalfunctionType structureMalfunctionType) {
    state.put(ComponentType.STRUCTURE, structureMalfunctionType);
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


  public boolean isBroken() {
    return !state.get(ComponentType.ACTUATOR).equals(MalfunctionType.NONE)
        || !state.get(ComponentType.SENSORS).equals(MalfunctionType.NONE)
        || !state.get(ComponentType.STRUCTURE).equals(MalfunctionType.NONE);
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
        state.get(ComponentType.ACTUATOR),
        state.get(ComponentType.SENSORS),
        state.get(ComponentType.STRUCTURE)
    );
    immutable.getChildren().addAll(superImmutable.getChildren());
    return immutable;
  }
}
