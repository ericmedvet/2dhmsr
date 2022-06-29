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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.units.erallab.hmsrobots.core.Actionable;
import it.units.erallab.hmsrobots.core.geometry.*;
import it.units.erallab.hmsrobots.core.sensors.Sensor;
import it.units.erallab.hmsrobots.core.sensors.Touch;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import it.units.erallab.hmsrobots.core.snapshots.VoxelPoly;
import it.units.erallab.hmsrobots.util.DoubleRange;
import org.apache.commons.lang3.ArrayUtils;
import org.dyn4j.collision.Filter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.joint.DistanceJoint;
import org.dyn4j.dynamics.joint.Joint;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Transform;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
@SuppressWarnings("UnaryPlus")
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class Voxel implements Actionable, Serializable, Snapshottable, WorldObject, Shape {

  public static final double SIDE_LENGTH = 3d;
  public static final double MASS_SIDE_LENGTH_RATIO = .35d;
  public static final double SPRING_F = 8d;
  public static final double SPRING_D = 0.3d;
  public static final double MASS_LINEAR_DAMPING = 0.1d;
  public static final double MASS_ANGULAR_DAMPING = 0.1d;
  public static final double FRICTION = 10d;
  public static final double RESTITUTION = 0.1d;
  public static final double MASS = 1d;
  public static final DoubleRange AREA_RATIO_PASSIVE_RANGE = DoubleRange.of(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
  public static final DoubleRange AREA_RATIO_ACTIVE_RANGE = DoubleRange.of(0.8, 1.2);
  public static final EnumSet<SpringScaffolding> SPRING_SCAFFOLDINGS = EnumSet.allOf(SpringScaffolding.class);
  @JsonProperty
  protected final double springF;
  @JsonProperty
  protected final double springD;
  @JsonProperty
  private final double sideLength;
  @JsonProperty
  private final double massSideLengthRatio;
  @JsonProperty
  private final double massLinearDamping;
  @JsonProperty
  private final double massAngularDamping;
  @JsonProperty
  private final double friction;
  @JsonProperty
  private final double restitution;
  @JsonProperty
  private final double mass;
  @JsonProperty
  private final DoubleRange areaRatioPassiveRange;
  @JsonProperty
  private final DoubleRange areaRatioActiveRange;
  @JsonProperty
  private final EnumSet<SpringScaffolding> springScaffoldings;
  @JsonProperty
  private final List<Sensor> sensors;

  protected transient Body[] vertexBodies;
  protected transient List<DistanceJoint<Body>> springJoints;
  private transient World<Body> world;
  private transient double areaRatioEnergy;
  private transient double controlEnergy;
  private transient double lastAppliedForce;

  @JsonCreator
  public Voxel(
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
      @JsonProperty("sensors") List<Sensor> sensors
  ) {
    this.sideLength = sideLength;
    this.massSideLengthRatio = massSideLengthRatio;
    this.springF = springF;
    this.springD = springD;
    this.massLinearDamping = massLinearDamping;
    this.massAngularDamping = massAngularDamping;
    this.friction = friction;
    this.restitution = restitution;
    this.mass = mass;
    this.areaRatioPassiveRange = areaRatioPassiveRange;
    this.areaRatioActiveRange = areaRatioActiveRange;
    this.springScaffoldings = springScaffoldings;
    this.sensors = sensors;
    if (areaRatioPassiveRange.min() > Double.NEGATIVE_INFINITY &&
        areaRatioPassiveRange.min() < ((2 * massSideLengthRatio) * (2 * massSideLengthRatio))) {
      throw new IllegalArgumentException(String.format(
          "Min of areaRatioPassiveRange=%f cannot be lower than the value (%f) imposed by massSideLengthRatio=%f",
          areaRatioPassiveRange.min(),
          (2 * massSideLengthRatio) * (2 * massSideLengthRatio),
          massSideLengthRatio
      ));
    }
    assemble();
  }

  public Voxel(List<Sensor> sensors) {
    this(
        SIDE_LENGTH,
        MASS_SIDE_LENGTH_RATIO,
        SPRING_F,
        SPRING_D,
        MASS_LINEAR_DAMPING,
        MASS_ANGULAR_DAMPING,
        FRICTION,
        RESTITUTION,
        MASS,
        AREA_RATIO_PASSIVE_RANGE,
        AREA_RATIO_ACTIVE_RANGE,
        SPRING_SCAFFOLDINGS,
        sensors
    );
  }

  public enum SpringScaffolding {
    SIDE_EXTERNAL, SIDE_INTERNAL, SIDE_CROSS, CENTRAL_CROSS
  }

  public record ParentFilter(Object parent) implements Filter {

    @Override
    public boolean isAllowed(Filter f) {
      if (!(f instanceof ParentFilter)) {
        return true;
      }
      return parent != ((ParentFilter) f).parent;
    }

  }

  public static class RobotFilter implements Filter {

    @Override
    public boolean isAllowed(Filter filter) {
      return true;
    }

  }

  private record SpringRange(double min, double rest, double max) {

    public SpringRange {
      if ((min > rest) || (max < rest) || (min < 0)) {
        throw new IllegalArgumentException(String.format("Wrong spring range [%f, %f, %f]", min, rest, max));
      }
    }

  }

  @Override
  public void act(double t) {
    double areaRatio = getAreaRatio();
    areaRatioEnergy = areaRatioEnergy + areaRatio * areaRatio;
    controlEnergy = controlEnergy + lastAppliedForce * lastAppliedForce;
    sensors.forEach(s -> s.act(t));
  }

  @Override
  public void addTo(World<Body> world) {
    if (this.world != null) {
      for (Body body : vertexBodies) {
        this.world.removeBody(body);
      }
      for (Joint<Body> joint : springJoints) {
        this.world.removeJoint(joint);
      }
    }
    this.world = world;
    for (Body body : vertexBodies) {
      world.addBody(body);
    }
    for (Joint<Body> joint : springJoints) {
      world.addJoint(joint);
    }
  }

  public void applyForce(double f) {
    if (Math.abs(f) > 1d) {
      f = Math.signum(f);
    }
    lastAppliedForce = f;
    for (DistanceJoint<Body> joint : springJoints) {
      Voxel.SpringRange range = (SpringRange) joint.getUserData();
      if (f >= 0) { // shrink
        joint.setRestDistance(range.rest - (range.rest - range.min) * f);
      } else if (f < 0) { // expand
        joint.setRestDistance(range.rest + (range.max - range.rest) * -f);
      }
    }
  }

  private void assemble() {
    //compute densities
    double massSideLength = sideLength * massSideLengthRatio;
    double density = (mass / 4) / (massSideLength * massSideLength);
    //build bodies
    vertexBodies = new Body[4];
    vertexBodies[0] = new Body(); //NW
    vertexBodies[1] = new Body(); //NE
    vertexBodies[2] = new Body(); //SE
    vertexBodies[3] = new Body(); //SW
    vertexBodies[0].addFixture(new Rectangle(massSideLength, massSideLength), density, friction, restitution);
    vertexBodies[1].addFixture(new Rectangle(massSideLength, massSideLength), density, friction, restitution);
    vertexBodies[2].addFixture(new Rectangle(massSideLength, massSideLength), density, friction, restitution);
    vertexBodies[3].addFixture(new Rectangle(massSideLength, massSideLength), density, friction, restitution);
    vertexBodies[0].translate(-(sideLength / 2d - massSideLength / 2d), +(sideLength / 2d - massSideLength / 2d));
    vertexBodies[1].translate(+(sideLength / 2d - massSideLength / 2d), +(sideLength / 2d - massSideLength / 2d));
    vertexBodies[2].translate(+(sideLength / 2d - massSideLength / 2d), -(sideLength / 2d - massSideLength / 2d));
    vertexBodies[3].translate(-(sideLength / 2d - massSideLength / 2d), -(sideLength / 2d - massSideLength / 2d));
    for (Body body : vertexBodies) {
      body.setMass(MassType.NORMAL);
      body.setLinearDamping(massLinearDamping);
      body.setAngularDamping(massAngularDamping);
    }
    //build distance joints constraints
    List<DistanceJoint<Body>> allSpringJoints = new ArrayList<>();
    DoubleRange passiveSideRange = DoubleRange.of(
        Math.sqrt(sideLength * sideLength * areaRatioPassiveRange.min()),
        Math.sqrt(sideLength * sideLength * areaRatioPassiveRange.max())
    );
    DoubleRange activeSideRange = DoubleRange.of(
        Math.sqrt(sideLength * sideLength * areaRatioActiveRange.min()),
        Math.sqrt(sideLength * sideLength * areaRatioActiveRange.max())
    );
    SpringRange sideParallelPassiveRange = new SpringRange(
        passiveSideRange.min() - 2d * massSideLength,
        sideLength - 2d * massSideLength,
        passiveSideRange.max() - 2d * massSideLength
    );
    SpringRange sideCrossPassiveRange = new SpringRange(
        Math.sqrt(massSideLength * massSideLength + sideParallelPassiveRange.min * sideParallelPassiveRange.min),
        Math.sqrt(massSideLength * massSideLength + sideParallelPassiveRange.rest * sideParallelPassiveRange.rest),
        Math.sqrt(massSideLength * massSideLength + sideParallelPassiveRange.max * sideParallelPassiveRange.max)
    );
    SpringRange centralCrossPassiveRange = new SpringRange(
        (passiveSideRange.min() - massSideLength) * Math.sqrt(2d),
        (sideLength - massSideLength) * Math.sqrt(2d),
        (passiveSideRange.max() - massSideLength) * Math.sqrt(2d)
    );
    SpringRange sideParallelActiveRange = new SpringRange(
        activeSideRange.min() - 2d * massSideLength,
        sideLength - 2d * massSideLength,
        activeSideRange.max() - 2d * massSideLength
    );
    SpringRange sideCrossActiveRange = new SpringRange(
        Math.sqrt(massSideLength * massSideLength + sideParallelActiveRange.min * sideParallelActiveRange.min),
        Math.sqrt(massSideLength * massSideLength + sideParallelActiveRange.rest * sideParallelActiveRange.rest),
        Math.sqrt(massSideLength * massSideLength + sideParallelActiveRange.max * sideParallelActiveRange.max)
    );
    SpringRange centralCrossActiveRange = new SpringRange(
        (activeSideRange.min() - massSideLength) * Math.sqrt(2d),
        (sideLength - massSideLength) * Math.sqrt(2d),
        (activeSideRange.max() - massSideLength) * Math.sqrt(2d)
    );
    //build distance joints
    if (springScaffoldings.contains(SpringScaffolding.SIDE_INTERNAL)) {
      List<DistanceJoint<Body>> localSpringJoints = new ArrayList<>();
      localSpringJoints.add(new DistanceJoint<>(
          vertexBodies[0],
          vertexBodies[1],
          vertexBodies[0].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d),
          vertexBodies[1].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint<>(
          vertexBodies[1],
          vertexBodies[2],
          vertexBodies[1].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d),
          vertexBodies[2].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint<>(
          vertexBodies[2],
          vertexBodies[3],
          vertexBodies[2].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d),
          vertexBodies[3].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint<>(
          vertexBodies[3],
          vertexBodies[0],
          vertexBodies[3].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d),
          vertexBodies[0].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d)
      ));
      for (DistanceJoint<Body> joint : localSpringJoints) {
        joint.setUserData(sideParallelActiveRange);
        if (sideParallelPassiveRange.min > Double.NEGATIVE_INFINITY) {
          joint.setLowerLimit(sideParallelPassiveRange.min);
          joint.setLowerLimitEnabled(true);
        }
        if (sideParallelPassiveRange.max < Double.POSITIVE_INFINITY) {
          joint.setUpperLimit(sideParallelPassiveRange.max);
          joint.setUpperLimitEnabled(true);
        }
      }
      allSpringJoints.addAll(localSpringJoints);
    }
    if (springScaffoldings.contains(SpringScaffolding.SIDE_EXTERNAL)) {
      List<DistanceJoint<Body>> localSpringJoints = new ArrayList<>();
      localSpringJoints.add(new DistanceJoint<>(
          vertexBodies[0],
          vertexBodies[1],
          vertexBodies[0].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d),
          vertexBodies[1].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint<>(
          vertexBodies[1],
          vertexBodies[2],
          vertexBodies[1].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d),
          vertexBodies[2].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint<>(
          vertexBodies[2],
          vertexBodies[3],
          vertexBodies[2].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d),
          vertexBodies[3].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint<>(
          vertexBodies[3],
          vertexBodies[0],
          vertexBodies[3].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d),
          vertexBodies[0].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d)
      ));
      for (DistanceJoint<Body> joint : localSpringJoints) {
        joint.setUserData(sideParallelActiveRange);
        if (sideParallelPassiveRange.min > Double.NEGATIVE_INFINITY) {
          joint.setLowerLimit(sideParallelPassiveRange.min);
          joint.setLowerLimitEnabled(true);
        }
        if (sideParallelPassiveRange.max < Double.POSITIVE_INFINITY) {
          joint.setUpperLimit(sideParallelPassiveRange.max);
          joint.setUpperLimitEnabled(true);
        }
      }
      allSpringJoints.addAll(localSpringJoints);
    }
    if (springScaffoldings.contains(SpringScaffolding.SIDE_CROSS)) {
      List<DistanceJoint<Body>> localSpringJoints = new ArrayList<>();
      localSpringJoints.add(new DistanceJoint<>(
          vertexBodies[0],
          vertexBodies[1],
          vertexBodies[0].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d),
          vertexBodies[1].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint<>(
          vertexBodies[0],
          vertexBodies[1],
          vertexBodies[0].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d),
          vertexBodies[1].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint<>(
          vertexBodies[1],
          vertexBodies[2],
          vertexBodies[1].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d),
          vertexBodies[2].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint<>(
          vertexBodies[1],
          vertexBodies[2],
          vertexBodies[1].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d),
          vertexBodies[2].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint<>(
          vertexBodies[2],
          vertexBodies[3],
          vertexBodies[2].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d),
          vertexBodies[3].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint<>(
          vertexBodies[2],
          vertexBodies[3],
          vertexBodies[2].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d),
          vertexBodies[3].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint<>(
          vertexBodies[3],
          vertexBodies[0],
          vertexBodies[3].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d),
          vertexBodies[0].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint<>(
          vertexBodies[3],
          vertexBodies[0],
          vertexBodies[3].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d),
          vertexBodies[0].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d)
      ));
      for (DistanceJoint<Body> joint : localSpringJoints) {
        joint.setUserData(sideCrossActiveRange);
        if (sideCrossPassiveRange.min > Double.NEGATIVE_INFINITY) {
          joint.setLowerLimit(sideCrossPassiveRange.min);
          joint.setLowerLimitEnabled(true);
        }
        if (sideCrossPassiveRange.max < Double.POSITIVE_INFINITY) {
          joint.setUpperLimit(sideCrossPassiveRange.max);
          joint.setUpperLimitEnabled(true);
        }
      }
      allSpringJoints.addAll(localSpringJoints);
    }
    if (springScaffoldings.contains(SpringScaffolding.CENTRAL_CROSS)) {
      List<DistanceJoint<Body>> localSpringJoints = new ArrayList<>();
      localSpringJoints.add(new DistanceJoint<>(
          vertexBodies[0],
          vertexBodies[2],
          vertexBodies[0].getWorldCenter(),
          vertexBodies[2].getWorldCenter()
      ));
      localSpringJoints.add(new DistanceJoint<>(
          vertexBodies[1],
          vertexBodies[3],
          vertexBodies[1].getWorldCenter(),
          vertexBodies[3].getWorldCenter()
      ));
      for (DistanceJoint<Body> joint : localSpringJoints) {
        joint.setUserData(centralCrossActiveRange);
        if (centralCrossPassiveRange.min > Double.NEGATIVE_INFINITY) {
          joint.setLowerLimit(centralCrossPassiveRange.min);
          joint.setLowerLimitEnabled(true);
        }
        if (centralCrossPassiveRange.max < Double.POSITIVE_INFINITY) {
          joint.setUpperLimit(centralCrossPassiveRange.max);
          joint.setUpperLimitEnabled(true);
        }
      }
      allSpringJoints.addAll(localSpringJoints);
    }
    //setup spring joints
    for (DistanceJoint<Body> joint : allSpringJoints) {
      joint.setRestDistance(((SpringRange) joint.getUserData()).rest);
      joint.setCollisionAllowed(true);
      joint.setFrequency(springF);
      joint.setDampingRatio(springD);
    }
    springJoints = Collections.unmodifiableList(allSpringJoints);
  }

  @Override
  public BoundingBox boundingBox() {
    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < 4; i++) {
      Vector2 point = getIndexedVertex(i, 3 - i);
      minX = Math.min(minX, point.x);
      maxX = Math.max(maxX, point.x);
      minY = Math.min(minY, point.y);
      maxY = Math.max(maxY, point.y);
    }
    return BoundingBox.of(Point2.of(minX, minY), Point2.of(maxX, maxY));
  }

  @Override
  public Point2 center() {
    double xc = 0d;
    double yc = 0d;
    for (Body vertex : vertexBodies) {
      xc = xc + vertex.getWorldCenter().x;
      yc = yc + vertex.getWorldCenter().y;
    }
    return Point2.of(xc / (double) vertexBodies.length, yc / (double) vertexBodies.length);
  }

  @Override
  public double area() {
    return Poly.of(
        Point2.of(getIndexedVertex(0, 3)),
        Point2.of(getIndexedVertex(1, 2)),
        Point2.of(getIndexedVertex(2, 1)),
        Point2.of(getIndexedVertex(3, 0))
    ).area();
  }

  public double getAngle() {
    Vector2 upSide = vertexBodies[1].getWorldCenter().copy().subtract(vertexBodies[0].getWorldCenter());
    Vector2 downSide = vertexBodies[2].getWorldCenter().copy().subtract(vertexBodies[3].getWorldCenter());
    return (upSide.getDirection() + downSide.getDirection()) / 2d;
  }

  public double getAreaRatio() {
    return area() / sideLength / sideLength;
  }

  public double getAreaRatioEnergy() {
    return areaRatioEnergy;
  }

  public double getControlEnergy() {
    return controlEnergy;
  }

  private Vector2 getIndexedVertex(int i, int j) {
    Transform t = vertexBodies[i].getTransform();
    Rectangle rectangle = (Rectangle) vertexBodies[i].getFixture(0).getShape();
    Vector2 tV = rectangle.getVertices()[j].copy();
    t.transform(tV);
    return tV;
  }

  public double getLastAppliedForce() {
    return lastAppliedForce;
  }

  public Point2 getLinearVelocity() {
    double x = 0d;
    double y = 0d;
    for (Body vertex : vertexBodies) {
      x = x + vertex.getLinearVelocity().x;
      y = y + vertex.getLinearVelocity().y;
    }
    return Point2.of(x / (double) vertexBodies.length, y / (double) vertexBodies.length);
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

  public double getSideLength() {
    return sideLength;
  }

  @Override
  public Snapshot getSnapshot() {
    Snapshot snapshot = new Snapshot(getVoxelPoly(), getClass());
    //add parts
    for (Body body : vertexBodies) {
      snapshot.getChildren().add(new Snapshot(rectangleToPoly(body), getClass()));
    }
    //add joints
    for (DistanceJoint<Body> joint : springJoints) {
      snapshot.getChildren()
          .add(new Snapshot(Vector.of(Point2.of(joint.getAnchor1()), Point2.of(joint.getAnchor2())), getClass()));
    }
    //add sensors
    for (Sensor sensor : sensors) {
      snapshot.getChildren().add(sensor.getSnapshot());
    }
    return snapshot;
  }

  public Body[] getVertexBodies() {
    return vertexBodies;
  }

  protected List<Point2> getVertices() {
    return List.of(
        Point2.of(getIndexedVertex(0, 3)),
        Point2.of(getIndexedVertex(1, 2)),
        Point2.of(getIndexedVertex(2, 1)),
        Point2.of(getIndexedVertex(3, 0))
    );
  }

  public VoxelPoly getVoxelPoly() {
    return new VoxelPoly(
        Poly.of(getVertices().toArray(Point2[]::new)),
        getAngle(),
        getLinearVelocity(),
        Touch.isTouchingGround(this),
        getAreaRatio(),
        getAreaRatioEnergy(),
        getLastAppliedForce(),
        getControlEnergy()
    );
  }

  public World<Body> getWorld() {
    return world;
  }

  @Serial
  private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
    ois.defaultReadObject();
    assemble();
  }

  private Poly rectangleToPoly(Body body) {
    Point2[] vertices = new Point2[4];
    Transform t = body.getTransform();
    Rectangle rectangle = (Rectangle) body.getFixture(0).getShape();
    for (int i = 0; i < 4; i++) {
      Vector2 tV = rectangle.getVertices()[i].copy();
      t.transform(tV);
      vertices[i] = Point2.of(tV);
    }
    return Poly.of(vertices);
  }

  @Override
  public void reset() {
    assemble();
    areaRatioEnergy = 0d;
    applyForce(0d);
    controlEnergy = 0d;
    lastAppliedForce = 0d;
    sensors.forEach(s -> {
      s.setVoxel(this);
      s.reset();
    });
  }

  public void setOwner(Robot robot) {
    Filter filter = new RobotFilter();
    for (Body vertexBody : vertexBodies) {
      vertexBody.setUserData(robot);
      vertexBody.getFixture(0).setFilter(filter);
    }
  }

  @Override
  public String toString() {
    return "Voxel{" +
        "springF=" + springF +
        ", sideLength=" + sideLength +
        ", massSideLengthRatio=" + massSideLengthRatio +
        ", springD=" + springD +
        ", massLinearDamping=" + massLinearDamping +
        ", massAngularDamping=" + massAngularDamping +
        ", friction=" + friction +
        ", restitution=" + restitution +
        ", mass=" + mass +
        ", areaRatioPassiveRange=" + areaRatioPassiveRange +
        ", areaRatioActiveRange=" + areaRatioActiveRange +
        ", springScaffoldings=" + springScaffoldings +
        ", sensors=" + sensors +
        '}';
  }

  public void translate(Vector2 v) {
    for (Body body : vertexBodies) {
      body.translate(v);
    }
  }

}
