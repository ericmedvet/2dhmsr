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
package it.units.erallab.hmsrobots.core.objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.geometry.Poly;
import it.units.erallab.hmsrobots.core.sensors.Sensor;
import it.units.erallab.hmsrobots.core.sensors.Touch;
import it.units.erallab.hmsrobots.core.snapshots.VoxelPoly;
import it.units.erallab.hmsrobots.util.DoubleRange;
import org.apache.commons.lang3.ArrayUtils;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.joint.DistanceJoint;

import java.util.*;
import java.util.random.RandomGenerator;

public class BreakableVoxel extends Voxel {

  @JsonProperty
  private final Map<ComponentType, Set<MalfunctionType>> malfunctions;
  @JsonProperty
  private final Map<MalfunctionTrigger, Double> triggerThresholds;
  @JsonProperty
  private final double restoreTime;
  @JsonProperty
  private final long randomSeed;
  private final EnumMap<MalfunctionTrigger, Double> triggerCounters;
  private final EnumMap<ComponentType, MalfunctionType> state;
  private transient double lastT;
  private transient double lastBreakT;
  private transient double lastControlEnergy;
  private transient double lastAreaRatioEnergy;
  private transient double[] sensorReadings;
  private transient RandomGenerator random;

  @JsonCreator
  public BreakableVoxel(
      @JsonProperty("sideLength") double sideLength,
      @JsonProperty("massSideLengthRatio") double massSideLengthRatio,
      @JsonProperty("springF") double springF,
      @JsonProperty("springD") double springD,
      @JsonProperty("massLinearDamping") double massLinearDamping,
      @JsonProperty("massAngularDamping") double massAngularDamping,
      @JsonProperty("friction") double friction,
      @JsonProperty("restitution") double restitution,
      @JsonProperty("mass") double mass,
      @JsonProperty("areaRatioPassiveRange") DoubleRange areaRatioPassiveRange,
      @JsonProperty("areaRatioActiveRange") DoubleRange areaRatioActiveRange,
      @JsonProperty("springScaffoldings") EnumSet<SpringScaffolding> springScaffoldings,
      @JsonProperty("sensors") List<Sensor> sensors,
      @JsonProperty("randomSeed") long randomSeed,
      @JsonProperty("malfunctions") Map<ComponentType, Set<MalfunctionType>> malfunctions,
      @JsonProperty("triggerThresholds") Map<MalfunctionTrigger, Double> triggerThresholds,
      @JsonProperty("restoreTime") double restoreTime
  ) {
    super(
        sideLength,
        massSideLengthRatio,
        springF,
        springD,
        massLinearDamping,
        massAngularDamping,
        friction,
        restitution,
        mass,
        areaRatioPassiveRange,
        areaRatioActiveRange,
        springScaffoldings,
        sensors
    );
    this.randomSeed = randomSeed;
    this.malfunctions = malfunctions;
    this.triggerThresholds = triggerThresholds;
    this.restoreTime = restoreTime;
    triggerCounters = new EnumMap<>(MalfunctionTrigger.class);
    state = new EnumMap<>(ComponentType.class);
    reset();
  }

  public BreakableVoxel(
      List<Sensor> sensors,
      long randomSeed,
      Map<ComponentType, Set<MalfunctionType>> malfunctions,
      Map<MalfunctionTrigger, Double> triggerThresholds,
      double restoreTime
  ) {
    super(sensors);
    this.randomSeed = randomSeed;
    this.malfunctions = malfunctions;
    this.triggerThresholds = triggerThresholds;
    this.restoreTime = restoreTime;
    triggerCounters = new EnumMap<>(MalfunctionTrigger.class);
    state = new EnumMap<>(ComponentType.class);
    Arrays.stream(ComponentType.values()).sequential().forEach(component -> state.put(component, MalfunctionType.NONE));
    reset();
  }

  public enum ComponentType {
    ACTUATOR, SENSORS, STRUCTURE
  }

  public enum MalfunctionTrigger {
    CONTROL, AREA, TIME
  }

  public enum MalfunctionType {
    NONE, ZERO, FROZEN, RANDOM
  }

  @Override
  public void act(double t) {
    super.act(t);
    if (state.get(ComponentType.SENSORS).equals(MalfunctionType.NONE) || sensorReadings == null) {
      sensorReadings = super.getSensorReadings();
    }
    //update counters
    triggerCounters.put(MalfunctionTrigger.TIME, triggerCounters.get(MalfunctionTrigger.TIME) + t - lastT);
    triggerCounters.put(
        MalfunctionTrigger.CONTROL,
        triggerCounters.get(MalfunctionTrigger.CONTROL) + (getControlEnergy() - lastControlEnergy)
    );
    triggerCounters.put(
        MalfunctionTrigger.AREA,
        triggerCounters.get(MalfunctionTrigger.AREA) + (getAreaRatioEnergy() - lastAreaRatioEnergy)
    );
    lastT = t;
    lastControlEnergy = getControlEnergy();
    lastAreaRatioEnergy = getAreaRatioEnergy();
    boolean breaking = false;
    //check if malfunction is applicable
    for (Map.Entry<MalfunctionTrigger, Double> triggerThreshold : triggerThresholds.entrySet()) {
      if (random.nextDouble() < 1d - Math.tanh(triggerThreshold.getValue() / triggerCounters.get(triggerThreshold.getKey()))) {
        //reset counters
        Arrays.stream(MalfunctionTrigger.values()).sequential().forEach(trigger -> triggerCounters.put(trigger, 0d));
        //choose component and malfunction
        ComponentType[] componentTypes = malfunctions.keySet().toArray(ComponentType[]::new);
        if (componentTypes.length > 0) {
          breaking = true;
          ComponentType componentType = componentTypes[random.nextInt(componentTypes.length)];
          MalfunctionType[] malfunctionTypes = malfunctions.get(componentType).toArray(MalfunctionType[]::new);
          MalfunctionType malfunctionType = malfunctionTypes[random.nextInt(malfunctionTypes.length)];
          state.put(componentType, malfunctionType);
          updateStructureMalfunctionType();
        }
      }
    }
    if (breaking) {
      lastBreakT = t;
    }
    //possibly restore
    if (t - lastBreakT > restoreTime) {
      state.put(ComponentType.ACTUATOR, MalfunctionType.NONE);
      state.put(ComponentType.SENSORS, MalfunctionType.NONE);
      state.put(ComponentType.STRUCTURE, MalfunctionType.NONE);
    }
  }

  @Override
  public void applyForce(double f) {
    double innerF = f;
    if (state.get(ComponentType.ACTUATOR).equals(MalfunctionType.ZERO)) {
      f = 0;
    } else if (state.get(ComponentType.ACTUATOR).equals(MalfunctionType.FROZEN)) {
      f = getLastAppliedForce();
    } else if (state.get(ComponentType.ACTUATOR).equals(MalfunctionType.RANDOM)) {
      f = random.nextDouble() * 2d - 1d;
    }
    super.applyForce(f);
  }

  @Override
  public double[] getSensorReadings() {
    return switch (state.get(ComponentType.SENSORS)) {
      case NONE, FROZEN -> sensorReadings;
      case ZERO -> new double[sensorReadings.length];
      case RANDOM -> getSensors().stream()
          .map(s -> random(s.getDomains()))
          .reduce(ArrayUtils::addAll)
          .orElse(new double[sensorReadings.length]);
    };
  }

  @Override
  public VoxelPoly getVoxelPoly() {
    return new VoxelPoly(
        Poly.of(getVertices().toArray(Point2[]::new)),
        getAngle(),
        getLinearVelocity(),
        Touch.isTouchingGround(this),
        getAreaRatio(),
        getAreaRatioEnergy(),
        getLastAppliedForce(),
        getControlEnergy(),
        new EnumMap<>(state)
    );
  }

  @Override
  public void reset() {
    super.reset();
    lastT = 0d;
    lastBreakT = 0d;
    lastControlEnergy = 0d;
    lastAreaRatioEnergy = 0d;
    random = new Random(randomSeed);
    sensorReadings = null;
    Arrays.stream(MalfunctionTrigger.values()).sequential().forEach(trigger -> triggerCounters.put(trigger, 0d));
    Arrays.stream(ComponentType.values()).sequential().forEach(component -> state.put(component, MalfunctionType.NONE));
    updateStructureMalfunctionType();
  }

  @Override
  public String toString() {
    return "BreakableVoxel{" + "malfunctions=" + malfunctions + ", triggerThresholds=" + triggerThresholds + ", restoreTime=" + restoreTime + '}';
  }

  public boolean isBroken() {
    return !state.get(ComponentType.ACTUATOR).equals(MalfunctionType.NONE) || !state.get(ComponentType.SENSORS)
        .equals(MalfunctionType.NONE) || !state.get(ComponentType.STRUCTURE).equals(MalfunctionType.NONE);
  }

  private double[] random(DoubleRange[] domains) {
    double[] values = new double[domains.length];
    for (int i = 0; i < domains.length; i++) {
      values[i] = random.nextDouble() * domains[i].extent() + domains[i].min();
    }
    return values;
  }

  private void updateStructureMalfunctionType() {
    if (state.get(ComponentType.STRUCTURE).equals(MalfunctionType.NONE)) {
      for (DistanceJoint<Body> springJoint : springJoints) {
        springJoint.setFrequency(springF);
        springJoint.setDampingRatio(springD);
      }
    } else if (state.get(ComponentType.STRUCTURE).equals(MalfunctionType.FROZEN)) {
      for (DistanceJoint<Body> springJoint : springJoints) {
        springJoint.setFrequency(0d);
        springJoint.setDampingRatio(0d);
      }
    } else {
      throw new IllegalArgumentException("Unsupported structure malfunction type.");
    }
  }
}
