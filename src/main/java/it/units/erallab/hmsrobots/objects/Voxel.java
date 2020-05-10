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
package it.units.erallab.hmsrobots.objects;

import it.units.erallab.hmsrobots.objects.immutable.*;
import it.units.erallab.hmsrobots.sensors.Sensor;
import it.units.erallab.hmsrobots.util.Configurable;
import it.units.erallab.hmsrobots.util.ConfigurableField;
import org.apache.commons.lang3.tuple.Pair;
import org.dyn4j.collision.Filter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.World;
import org.dyn4j.dynamics.joint.DistanceJoint;
import org.dyn4j.dynamics.joint.Joint;
import org.dyn4j.dynamics.joint.RopeJoint;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Transform;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Voxel implements WorldObject {

  public enum ForceMethod {
    DISTANCE, FORCE
  }

  public enum SpringScaffolding {
    SIDE_EXTERNAL, SIDE_INTERNAL, SIDE_CROSS, CENTRAL_CROSS
  }

  private static class SpringRange {

    public final double min;
    public final double rest;
    public final double max;

    public SpringRange(double min, double rest, double max) {
      if ((min > rest) || (max < rest) || (min < 0)) {
        throw new IllegalArgumentException(String.format("Wrong spring range [%f, %f, %f]", min, rest, max));
      }
      this.min = min;
      this.rest = rest;
      this.max = max;
    }

    @Override
    public int hashCode() {
      int hash = 5;
      hash = 53 * hash + (int) (Double.doubleToLongBits(this.min) ^ (Double.doubleToLongBits(this.min) >>> 32));
      hash = 53 * hash + (int) (Double.doubleToLongBits(this.rest) ^ (Double.doubleToLongBits(this.rest) >>> 32));
      hash = 53 * hash + (int) (Double.doubleToLongBits(this.max) ^ (Double.doubleToLongBits(this.max) >>> 32));
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final SpringRange other = (SpringRange) obj;
      if (Double.doubleToLongBits(this.min) != Double.doubleToLongBits(other.min)) {
        return false;
      }
      if (Double.doubleToLongBits(this.rest) != Double.doubleToLongBits(other.rest)) {
        return false;
      }
      return Double.doubleToLongBits(this.max) == Double.doubleToLongBits(other.max);
    }

  }

  public static class ParentFilter implements Filter {

    private final Object parent;

    public ParentFilter(Object parent) {
      this.parent = parent;
    }

    @Override
    public boolean isAllowed(Filter f) {
      if (!(f instanceof ParentFilter)) {
        return true;
      }
      return parent != ((ParentFilter) f).parent;
    }

  }

  public static class Description implements Configurable<Description> {
    @ConfigurableField
    private final double sideLength = 3d;
    @ConfigurableField
    private final double massSideLengthRatio = .30d;
    @ConfigurableField
    private final double springF = 8d;
    @ConfigurableField
    private final double springD = 0.3d;
    @ConfigurableField
    private final double massLinearDamping = 1d;
    @ConfigurableField
    private final double massAngularDamping = 1d;
    @ConfigurableField
    private final double maxForce = 1000d; //not used in forceMethod=DISTANCE
    @ConfigurableField
    private final double areaRatioOffset = 0.2d; //not used in forceMethod=FORCE
    @ConfigurableField
    private final double friction = 100d;
    @ConfigurableField
    private final double restitution = 0.1d;
    @ConfigurableField
    private final double mass = 1d;
    @ConfigurableField
    private final boolean limitContractionFlag = true;
    @ConfigurableField
    private final boolean massCollisionFlag = false;
    @ConfigurableField
    private final ForceMethod forceMethod = ForceMethod.DISTANCE;
    @ConfigurableField
    private final EnumSet<SpringScaffolding> springScaffoldings = EnumSet.of(
        SpringScaffolding.SIDE_EXTERNAL,
        SpringScaffolding.SIDE_INTERNAL,
        SpringScaffolding.SIDE_CROSS,
        SpringScaffolding.CENTRAL_CROSS
    );
    @ConfigurableField
    private final List<Sensor> sensors = new ArrayList<>();

    private Description() {
    }

    public double getSideLength() {
      return sideLength;
    }

    public double getMassSideLengthRatio() {
      return massSideLengthRatio;
    }

    public double getSpringF() {
      return springF;
    }

    public double getSpringD() {
      return springD;
    }

    public double getMassLinearDamping() {
      return massLinearDamping;
    }

    public double getMassAngularDamping() {
      return massAngularDamping;
    }

    public double getMaxForce() {
      return maxForce;
    }

    public double getAreaRatioOffset() {
      return areaRatioOffset;
    }

    public double getFriction() {
      return friction;
    }

    public double getRestitution() {
      return restitution;
    }

    public double getMass() {
      return mass;
    }

    public boolean isLimitContractionFlag() {
      return limitContractionFlag;
    }

    public boolean isMassCollisionFlag() {
      return massCollisionFlag;
    }

    public ForceMethod getForceMethod() {
      return forceMethod;
    }

    public EnumSet<SpringScaffolding> getSpringScaffoldings() {
      return springScaffoldings;
    }

    public List<Sensor> getSensors() {
      return sensors;
    }

    public static Description build() {
      return new Description();
    }
  }

  private final Body[] vertexBodies;
  private final DistanceJoint[] springJoints;
  private final RopeJoint[] ropeJoints;
  private final double sideLength;
  private final ForceMethod forceMethod;
  private final double maxForce;
  private final double massSideLengthRatio;
  private final Robot robot;
  private final List<Sensor> sensors;

  private double lastAppliedForce = 0d;
  private List<Pair<Sensor, double[]>> lastSensorReadings = Collections.EMPTY_LIST;
  private World world;

  public static Voxel build(Robot robot, Description description) {
    return new Voxel(robot, description);
  }

  public static Voxel build(Robot robot) {
    return new Voxel(robot, Description.build());
  }

  private Voxel(Robot robot, Description description) {
    this.robot = robot;
    this.sideLength = description.sideLength;
    this.forceMethod = description.forceMethod;
    this.maxForce = description.maxForce;
    this.massSideLengthRatio = description.massSideLengthRatio;
    this.sensors = new ArrayList<>(description.sensors);
    //compute densities
    double massSideLength = description.sideLength * description.massSideLengthRatio;
    double density = description.mass * massSideLength / massSideLength / 4;
    //build bodies
    vertexBodies = new Body[4];
    vertexBodies[0] = new Body(1); //NW
    vertexBodies[1] = new Body(1); //NE
    vertexBodies[2] = new Body(1); //SE
    vertexBodies[3] = new Body(1); //SW
    for (Body vertexBody : vertexBodies) {
      vertexBody.setUserData(robot);
    }
    vertexBodies[0].addFixture(new Rectangle(massSideLength, massSideLength), density, description.friction, description.restitution);
    vertexBodies[1].addFixture(new Rectangle(massSideLength, massSideLength), density, description.friction, description.restitution);
    vertexBodies[2].addFixture(new Rectangle(massSideLength, massSideLength), density, description.friction, description.restitution);
    vertexBodies[3].addFixture(new Rectangle(massSideLength, massSideLength), density, description.friction, description.restitution);
    vertexBodies[0].translate(-(sideLength / 2d - massSideLength / 2d), +(sideLength / 2d - massSideLength / 2d));
    vertexBodies[1].translate(+(sideLength / 2d - massSideLength / 2d), +(sideLength / 2d - massSideLength / 2d));
    vertexBodies[2].translate(+(sideLength / 2d - massSideLength / 2d), -(sideLength / 2d - massSideLength / 2d));
    vertexBodies[3].translate(-(sideLength / 2d - massSideLength / 2d), -(sideLength / 2d - massSideLength / 2d));
    ParentFilter filter = new ParentFilter(robot);
    for (Body body : vertexBodies) {
      body.setMass(MassType.NORMAL);
      body.setLinearDamping(description.massLinearDamping);
      body.setAngularDamping(description.massAngularDamping);
      if (description.massCollisionFlag) {
        body.getFixture(0).setFilter(filter);
      }
    }
    //build rope joints
    List<RopeJoint> localRopeJoints = new ArrayList<>();
    if (description.limitContractionFlag) {
      localRopeJoints.add(new RopeJoint(vertexBodies[0], vertexBodies[1], vertexBodies[0].getWorldCenter(), vertexBodies[1].getWorldCenter()));
      localRopeJoints.add(new RopeJoint(vertexBodies[1], vertexBodies[2], vertexBodies[1].getWorldCenter(), vertexBodies[2].getWorldCenter()));
      localRopeJoints.add(new RopeJoint(vertexBodies[2], vertexBodies[3], vertexBodies[2].getWorldCenter(), vertexBodies[3].getWorldCenter()));
      localRopeJoints.add(new RopeJoint(vertexBodies[3], vertexBodies[0], vertexBodies[3].getWorldCenter(), vertexBodies[0].getWorldCenter()));
      for (RopeJoint ropeJoint : localRopeJoints) {
        ropeJoint.setLowerLimit(massSideLength);
        ropeJoint.setLowerLimitEnabled(true);
        ropeJoint.setUpperLimitEnabled(false);
      }
    }
    ropeJoints = localRopeJoints.toArray(new RopeJoint[0]);
    //build distance joints
    List<DistanceJoint> allSpringJoints = new ArrayList<>();
    double minSideLength = Math.sqrt(sideLength * sideLength * (1d - description.areaRatioOffset));
    double maxSideLength = Math.sqrt(sideLength * sideLength * (1d + description.areaRatioOffset));
    SpringRange sideParallelRange = new SpringRange(minSideLength - 2d * massSideLength, sideLength - 2d * massSideLength, maxSideLength - 2d * massSideLength);
    SpringRange sideCrossRange = new SpringRange(Math.sqrt(massSideLength * massSideLength + sideParallelRange.min * sideParallelRange.min), Math.sqrt(massSideLength * massSideLength + sideParallelRange.rest * sideParallelRange.rest), Math.sqrt(massSideLength * massSideLength + sideParallelRange.max * sideParallelRange.max));
    SpringRange centralCrossRange = new SpringRange((minSideLength - massSideLength) * Math.sqrt(2d), (sideLength - massSideLength) * Math.sqrt(2d), (maxSideLength - massSideLength) * Math.sqrt(2d));
    if (description.springScaffoldings.contains(SpringScaffolding.SIDE_INTERNAL)) {
      List<DistanceJoint> localSpringJoints = new ArrayList<>();
      localSpringJoints.add(new DistanceJoint(vertexBodies[0], vertexBodies[1],
          vertexBodies[0].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d),
          vertexBodies[1].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint(vertexBodies[1], vertexBodies[2],
          vertexBodies[1].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d),
          vertexBodies[2].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint(vertexBodies[2], vertexBodies[3],
          vertexBodies[2].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d),
          vertexBodies[3].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint(vertexBodies[3], vertexBodies[0],
          vertexBodies[3].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d),
          vertexBodies[0].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d)
      ));
      for (DistanceJoint joint : localSpringJoints) {
        joint.setUserData(sideParallelRange);
      }
      allSpringJoints.addAll(localSpringJoints);
    }
    if (description.springScaffoldings.contains(SpringScaffolding.SIDE_EXTERNAL)) {
      List<DistanceJoint> localSpringJoints = new ArrayList<>();
      localSpringJoints.add(new DistanceJoint(vertexBodies[0], vertexBodies[1],
          vertexBodies[0].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d),
          vertexBodies[1].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint(vertexBodies[1], vertexBodies[2],
          vertexBodies[1].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d),
          vertexBodies[2].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint(vertexBodies[2], vertexBodies[3],
          vertexBodies[2].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d),
          vertexBodies[3].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint(vertexBodies[3], vertexBodies[0],
          vertexBodies[3].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d),
          vertexBodies[0].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d)
      ));
      for (DistanceJoint joint : localSpringJoints) {
        joint.setUserData(sideParallelRange);
      }
      allSpringJoints.addAll(localSpringJoints);
    }
    if (description.springScaffoldings.contains(SpringScaffolding.SIDE_CROSS)) {
      List<DistanceJoint> localSpringJoints = new ArrayList<>();
      localSpringJoints.add(new DistanceJoint(vertexBodies[0], vertexBodies[1],
          vertexBodies[0].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d),
          vertexBodies[1].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint(vertexBodies[0], vertexBodies[1],
          vertexBodies[0].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d),
          vertexBodies[1].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint(vertexBodies[1], vertexBodies[2],
          vertexBodies[1].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d),
          vertexBodies[2].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint(vertexBodies[1], vertexBodies[2],
          vertexBodies[1].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d),
          vertexBodies[2].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint(vertexBodies[2], vertexBodies[3],
          vertexBodies[2].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d),
          vertexBodies[3].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint(vertexBodies[2], vertexBodies[3],
          vertexBodies[2].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d),
          vertexBodies[3].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint(vertexBodies[3], vertexBodies[0],
          vertexBodies[3].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d),
          vertexBodies[0].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d)
      ));
      localSpringJoints.add(new DistanceJoint(vertexBodies[3], vertexBodies[0],
          vertexBodies[3].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d),
          vertexBodies[0].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d)
      ));
      for (DistanceJoint joint : localSpringJoints) {
        joint.setUserData(sideCrossRange);
      }
      allSpringJoints.addAll(localSpringJoints);
    }
    if (description.springScaffoldings.contains(SpringScaffolding.CENTRAL_CROSS)) {
      List<DistanceJoint> localSpringJoints = new ArrayList<>();
      localSpringJoints.add(new DistanceJoint(vertexBodies[0], vertexBodies[2],
          vertexBodies[0].getWorldCenter(),
          vertexBodies[2].getWorldCenter()
      ));
      localSpringJoints.add(new DistanceJoint(vertexBodies[1], vertexBodies[3],
          vertexBodies[1].getWorldCenter(),
          vertexBodies[3].getWorldCenter()
      ));
      for (DistanceJoint joint : localSpringJoints) {
        joint.setUserData(centralCrossRange);
      }
      allSpringJoints.addAll(localSpringJoints);
    }
    //setup spring joints
    for (DistanceJoint joint : allSpringJoints) {
      joint.setDistance(((SpringRange) joint.getUserData()).rest);
      joint.setFrequency(description.springF);
      joint.setDampingRatio(description.springD);
    }
    springJoints = allSpringJoints.toArray(new DistanceJoint[0]);
  }

  @Override
  public ImmutableObject immutable() {
    //voxel shape
    Shape voxelShape = Poly.build(
        Point2.build(getIndexedVertex(0, 3)),
        Point2.build(getIndexedVertex(1, 2)),
        Point2.build(getIndexedVertex(2, 1)),
        Point2.build(getIndexedVertex(3, 0))
    );
    //add parts
    List<ImmutableObject> children = new ArrayList<>(vertexBodies.length + springJoints.length);
    for (Body body : vertexBodies) {
      children.add(new ImmutableObject(body, rectangleToPoly(body)));
    }
    //add joints
    for (DistanceJoint joint : springJoints) {
      children.add(new ImmutableObject(joint, Vector.build(
          Point2.build(joint.getAnchor1()),
          Point2.build(joint.getAnchor2())
      )));
    }
    //add sensor readings
    int nOfSensors = lastSensorReadings.size();
    for (int i = 0; i < nOfSensors; i++) {
      Pair<Sensor, double[]> pair = lastSensorReadings.get(i);
      Sensor sensor = pair.getKey();
      children.add(new ImmutableReading(
          sensor,
          voxelShape,
          pair.getValue(),
          sensor.domains(),
          (sensor instanceof Configurable) ? ((Configurable) sensor).toConfiguration() : null,
          i,
          nOfSensors
      ));
    }
    //add enclosing
    ImmutableVoxel immutable = new ImmutableVoxel(
        this,
        voxelShape,
        children,
        sideLength * sideLength
    );
    return immutable;
  }

  private Vector2 getIndexedVertex(int i, int j) {
    Transform t = vertexBodies[i].getTransform();
    Rectangle rectangle = (Rectangle) vertexBodies[i].getFixture(0).getShape();
    Vector2 tV = rectangle.getVertices()[j].copy();
    t.transform(tV);
    return tV;
  }

  private Poly rectangleToPoly(Body body) {
    Point2[] vertices = new Point2[4];
    Transform t = body.getTransform();
    Rectangle rectangle = (Rectangle) body.getFixture(0).getShape();
    for (int i = 0; i < 4; i++) {
      Vector2 tV = rectangle.getVertices()[i].copy();
      t.transform(tV);
      vertices[i] = Point2.build(tV);
    }
    return Poly.build(vertices);
  }

  @Override
  public void addTo(World world) {
    this.world = world;
    for (Body body : vertexBodies) {
      world.addBody(body);
    }
    for (Joint joint : springJoints) {
      world.addJoint(joint);
    }
    for (Joint joint : ropeJoints) {
      world.addJoint(joint);
    }
  }

  public List<Pair<Sensor, double[]>> sense(double t) {
    List<Pair<Sensor, double[]>> pairs = sensors.stream()
        .map(s -> Pair.of(s, s.sense(this, t)))
        .collect(Collectors.toList());
    lastSensorReadings = pairs;
    return pairs;
  }

  public Body[] getVertexBodies() {
    return vertexBodies;
  }

  public void applyForce(double f) {
    if (Math.abs(f) > 1d) {
      f = Math.signum(f);
    }
    lastAppliedForce = f;
    if (forceMethod.equals(ForceMethod.FORCE)) {
      double xc = 0d;
      double yc = 0d;
      for (Body body : vertexBodies) {
        xc = xc + body.getWorldCenter().x;
        yc = yc + body.getWorldCenter().y;
      }
      xc = xc / (double) vertexBodies.length;
      yc = yc / (double) vertexBodies.length;
      for (Body body : vertexBodies) {
        Vector2 force = (new Vector2(xc, yc)).subtract(body.getWorldCenter()).getNormalized().multiply(f * maxForce);
        body.applyForce(force);
      }
    } else if (forceMethod.equals(ForceMethod.DISTANCE)) {
      for (DistanceJoint joint : springJoints) {
        SpringRange range = (SpringRange) joint.getUserData();
        if (f > 0) {
          joint.setDistance(range.rest - (range.rest - range.min) * f);
        } else if (f < 0) {
          joint.setDistance(range.rest + (range.max - range.rest) * -f);
        }
      }
    }
  }

  public double getLastAppliedForce() {
    return lastAppliedForce;
  }

  public Vector2 getLinearVelocity() {
    double x = 0d;
    double y = 0d;
    for (Body vertex : vertexBodies) {
      x = x + vertex.getLinearVelocity().x;
      y = y + vertex.getLinearVelocity().y;
    }
    return new Vector2(x / (double) vertexBodies.length, y / (double) vertexBodies.length);
  }

  public double getAreaRatio() {
    Poly poly = Poly.build(
        Point2.build(getIndexedVertex(0, 3)),
        Point2.build(getIndexedVertex(1, 2)),
        Point2.build(getIndexedVertex(2, 1)),
        Point2.build(getIndexedVertex(3, 0))
    );
    return poly.area() / sideLength / sideLength;
  }

  public Vector2 getCenter() {
    double xc = 0d;
    double yc = 0d;
    for (Body vertex : vertexBodies) {
      xc = xc + vertex.getWorldCenter().x;
      yc = yc + vertex.getWorldCenter().y;
    }
    return new Vector2(xc / (double) vertexBodies.length, yc / (double) vertexBodies.length);
  }

  public double getAngle() {
    Vector2 upSide = vertexBodies[1].getWorldCenter().copy().subtract(vertexBodies[0].getWorldCenter());
    Vector2 downSide = vertexBodies[2].getWorldCenter().copy().subtract(vertexBodies[3].getWorldCenter());
    return (upSide.getDirection() + downSide.getDirection()) / 2d;
  }

  public void translate(Vector2 v) {
    for (Body body : vertexBodies) {
      body.translate(v);
    }
  }

  public double getSideLength() {
    return sideLength;
  }

  public Robot getRobot() {
    return robot;
  }

  public World getWorld() {
    return world;
  }

  public List<Sensor> getSensors() {
    return sensors;
  }
}
