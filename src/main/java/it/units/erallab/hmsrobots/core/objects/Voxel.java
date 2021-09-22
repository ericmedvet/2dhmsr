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
import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.geometry.Poly;
import it.units.erallab.hmsrobots.core.geometry.Vector;
import it.units.erallab.hmsrobots.core.sensors.Touch;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import it.units.erallab.hmsrobots.core.snapshots.VoxelPoly;
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class Voxel implements Actionable, Serializable, Snapshottable, WorldObject {

  public enum SpringScaffolding {
    SIDE_EXTERNAL, SIDE_INTERNAL, SIDE_CROSS, CENTRAL_CROSS
  }

  protected static class SpringRange {

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

  public static class RobotFilter implements Filter {

    @Override
    public boolean isAllowed(Filter filter) {
      return true;
    }

  }

  public static final double SIDE_LENGTH = 3d;
  public static final double MASS_SIDE_LENGTH_RATIO = .40d;
  public static final double SPRING_F = 8d;
  public static final double SPRING_D = 0.3d;
  public static final double MASS_LINEAR_DAMPING = 0.1d;
  public static final double MASS_ANGULAR_DAMPING = 0.1d;
  public static final double FRICTION = 10d;
  public static final double RESTITUTION = 0.1d;
  public static final double MASS = 1d;
  public static final boolean LIMIT_CONTRACTION_FLAG = true;
  public static final boolean MASS_COLLISION_FLAG = false;
  public static final double AREA_RATIO_MAX_DELTA = 0.225d;
  public static final EnumSet<SpringScaffolding> SPRING_SCAFFOLDINGS = EnumSet.allOf(SpringScaffolding.class);

  @JsonProperty
  private final double sideLength;
  @JsonProperty
  private final double massSideLengthRatio;
  @JsonProperty
  protected final double springF;
  @JsonProperty
  private final double springD;
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
  private final boolean limitContractionFlag;
  @JsonProperty
  private final boolean massCollisionFlag;
  @JsonProperty
  private final double areaRatioMaxDelta;
  @JsonProperty
  private final EnumSet<SpringScaffolding> springScaffoldings;

  protected transient Body[] vertexBodies;
  protected transient DistanceJoint[] springJoints;
  protected transient RopeJoint[] ropeJoints;
  private transient World world;

  private transient double areaRatioEnergy;

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
      @JsonProperty("limitContractionFlag") boolean limitContractionFlag,
      @JsonProperty("massCollisionFlag") boolean massCollisionFlag,
      @JsonProperty("areaRatioMaxDelta") double areaRatioMaxDelta,
      @JsonProperty("springScaffoldings") EnumSet<SpringScaffolding> springScaffoldings
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
    this.limitContractionFlag = limitContractionFlag;
    this.massCollisionFlag = massCollisionFlag;
    this.areaRatioMaxDelta = areaRatioMaxDelta;
    this.springScaffoldings = springScaffoldings;
    assemble();
  }

  public Voxel() {
    this(SIDE_LENGTH, MASS_SIDE_LENGTH_RATIO, SPRING_F, SPRING_D, MASS_LINEAR_DAMPING, MASS_ANGULAR_DAMPING, FRICTION, RESTITUTION, MASS, LIMIT_CONTRACTION_FLAG, MASS_COLLISION_FLAG, AREA_RATIO_MAX_DELTA, SPRING_SCAFFOLDINGS);
  }

  private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
    ois.defaultReadObject();
    assemble();
  }

  private void assemble() {
    //compute densities
    double massSideLength = sideLength * massSideLengthRatio;
    double density = mass * massSideLength / massSideLength / 4;
    //build bodies
    vertexBodies = new Body[4];
    vertexBodies[0] = new Body(1); //NW
    vertexBodies[1] = new Body(1); //NE
    vertexBodies[2] = new Body(1); //SE
    vertexBodies[3] = new Body(1); //SW
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
    //build rope joints
    List<RopeJoint> localRopeJoints = new ArrayList<>();
    if (limitContractionFlag) {
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
    double minSideLength = Math.sqrt(sideLength * sideLength * (1d - areaRatioMaxDelta));
    double maxSideLength = Math.sqrt(sideLength * sideLength * (1d + areaRatioMaxDelta));
    SpringRange sideParallelRange = new SpringRange(minSideLength - 2d * massSideLength, sideLength - 2d * massSideLength, maxSideLength - 2d * massSideLength);
    SpringRange sideCrossRange = new SpringRange(Math.sqrt(massSideLength * massSideLength + sideParallelRange.min * sideParallelRange.min), Math.sqrt(massSideLength * massSideLength + sideParallelRange.rest * sideParallelRange.rest), Math.sqrt(massSideLength * massSideLength + sideParallelRange.max * sideParallelRange.max));
    SpringRange centralCrossRange = new SpringRange((minSideLength - massSideLength) * Math.sqrt(2d), (sideLength - massSideLength) * Math.sqrt(2d), (maxSideLength - massSideLength) * Math.sqrt(2d));
    if (springScaffoldings.contains(SpringScaffolding.SIDE_INTERNAL)) {
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
    if (springScaffoldings.contains(SpringScaffolding.SIDE_EXTERNAL)) {
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
    if (springScaffoldings.contains(SpringScaffolding.SIDE_CROSS)) {
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
    if (springScaffoldings.contains(SpringScaffolding.CENTRAL_CROSS)) {
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
      joint.setFrequency(springF);
      joint.setDampingRatio(springD);
    }
    springJoints = allSpringJoints.toArray(new DistanceJoint[0]);
  }

  public void setOwner(Robot<?> robot) {
    Filter filter;
    if (massCollisionFlag) {
      filter = new ParentFilter(robot);
    } else {
      filter = new RobotFilter();
    }
    for (Body vertexBody : vertexBodies) {
      vertexBody.setUserData(robot);
      vertexBody.getFixture(0).setFilter(filter);
    }
  }

  @Override
  public Snapshot getSnapshot() {
    Snapshot snapshot = new Snapshot(getVoxelPoly(), getClass());
    fillSnapshot(snapshot);
    return snapshot;
  }

  public VoxelPoly getVoxelPoly() {
    return new VoxelPoly(
        getVertices(),
        getAngle(),
        getLinearVelocity(),
        Touch.isTouchingGround(this),
        getAreaRatio(),
        getAreaRatioEnergy()
    );
  }

  protected List<Point2> getVertices() {
    return List.of(
        Point2.of(getIndexedVertex(0, 3)),
        Point2.of(getIndexedVertex(1, 2)),
        Point2.of(getIndexedVertex(2, 1)),
        Point2.of(getIndexedVertex(3, 0))
    );
  }

  protected void fillSnapshot(Snapshot snapshot) {
    //add parts
    for (Body body : vertexBodies) {
      snapshot.getChildren().add(new Snapshot(
          rectangleToPoly(body),
          getClass()
      ));
    }
    //add joints
    for (DistanceJoint joint : springJoints) {
      snapshot.getChildren().add(new Snapshot(
          Vector.of(
              Point2.of(joint.getAnchor1()),
              Point2.of(joint.getAnchor2())
          ),
          getClass()
      ));
    }
  }

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
    return BoundingBox.of(
        Point2.of(minX, minY),
        Point2.of(maxX, maxY)
    );
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
      vertices[i] = Point2.of(tV);
    }
    return Poly.of(vertices);
  }

  @Override
  public void addTo(World world) {
    if (this.world != null) {
      for (Body body : vertexBodies) {
        this.world.removeBody(body);
      }
      for (Joint joint : springJoints) {
        this.world.removeJoint(joint);
      }
      for (Joint joint : ropeJoints) {
        this.world.removeJoint(joint);
      }
    }
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

  public Body[] getVertexBodies() {
    return vertexBodies;
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

  public double getAreaRatio() {
    Poly poly = Poly.of(
        Point2.of(getIndexedVertex(0, 3)),
        Point2.of(getIndexedVertex(1, 2)),
        Point2.of(getIndexedVertex(2, 1)),
        Point2.of(getIndexedVertex(3, 0))
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

  public World getWorld() {
    return world;
  }

  @Override
  public void reset() {
    assemble();
    areaRatioEnergy = 0d;
  }

  @Override
  public void act(double t) {
    double areaRatio = getAreaRatio();
    areaRatioEnergy = areaRatioEnergy + areaRatio * areaRatio;
  }

  public double getAreaRatioEnergy() {
    return areaRatioEnergy;
  }

  @Override
  public String toString() {
    return "Voxel{" +
        "sideLength=" + sideLength +
        ", massSideLengthRatio=" + massSideLengthRatio +
        ", springF=" + springF +
        ", springD=" + springD +
        ", massLinearDamping=" + massLinearDamping +
        ", massAngularDamping=" + massAngularDamping +
        ", friction=" + friction +
        ", restitution=" + restitution +
        ", mass=" + mass +
        ", limitContractionFlag=" + limitContractionFlag +
        ", massCollisionFlag=" + massCollisionFlag +
        ", areaRatioMaxDelta=" + areaRatioMaxDelta +
        ", springScaffoldings=" + springScaffoldings +
        ", areaRatioEnergy=" + areaRatioEnergy +
        '}';
  }
}
