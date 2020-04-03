/*
 * Copyright (C) 2019 Eric Medvet <eric.medvet@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.objects;

import it.units.erallab.hmsrobots.objects.immutable.Poly;
import it.units.erallab.hmsrobots.objects.immutable.ImmutableObject;
import it.units.erallab.hmsrobots.objects.immutable.ImmutablePoly;
import it.units.erallab.hmsrobots.objects.immutable.ImmutableVector;
import it.units.erallab.hmsrobots.objects.immutable.Point2;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
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

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Voxel implements WorldObject {

  public static enum Sensor {
    AREA_RATIO,
    VELOCITY_MAGNITUDE,
    X_VELOCITY, Y_VELOCITY,
    X_ROT_VELOCITY, Y_ROT_VELOCITY,
    ANGLE,
    BROKEN_RATIO,
    LAST_APPLIED_FORCE,
    TOUCHING;
  }

  public static enum ForceMethod {
    DISTANCE, FORCE
  };

  public static enum SpringScaffolding {
    SIDE_EXTERNAL, SIDE_INTERNAL, SIDE_CROSS, CENTRAL_CROSS
  };

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
      if (Double.doubleToLongBits(this.max) != Double.doubleToLongBits(other.max)) {
        return false;
      }
      return true;
    }

  }

  public static class Builder implements Serializable {

    private final static double SIDE_LENGTH = 3d;
    private final static double MASS_SIDE_LENGTH_RATIO = .30d;
    private final static double SPRING_F = 8d;
    private final static double SPRING_D = 0.3d;
    private final static double MASS_LINEAR_DUMPING = 1d;
    private final static double MASS_ANGULAR_DUMPING = 1d;
    private final static double MAX_FORCE = 1000d; //not used in forceMethod=DISTANCE
    private final static double AREA_RATIO_OFFSET = 0.2d; //not used in forceMethod=FORCE
    private final static double FRICTION = 100d;
    private final static double RESTITUTION = 0.1d;
    private final static double MASS = 1d;
    private final static boolean LIMIT_CONTRACTION_FLAG = true;
    private final static boolean MASS_COLLISION_FLAG = false;
    private final static ForceMethod FORCE_METHOD = ForceMethod.DISTANCE;
    private final static EnumSet<SpringScaffolding> SPRING_SCAFFOLDINGS = EnumSet.of(
            SpringScaffolding.SIDE_EXTERNAL,
            SpringScaffolding.SIDE_INTERNAL,
            SpringScaffolding.SIDE_CROSS,
            SpringScaffolding.CENTRAL_CROSS
    );

    private double sideLength = SIDE_LENGTH;
    private double massSideLengthRatio = MASS_SIDE_LENGTH_RATIO;
    private double springF = SPRING_F;
    private double springD = SPRING_D;
    private double massLinearDamping = MASS_LINEAR_DUMPING;
    private double massAngularDamping = MASS_ANGULAR_DUMPING;
    private double maxForce = MAX_FORCE;
    private double areaRatioOffset = AREA_RATIO_OFFSET;
    private double friction = FRICTION;
    private double restitution = RESTITUTION;
    private double mass = MASS;
    private boolean limitContractionFlag = LIMIT_CONTRACTION_FLAG;
    private boolean massCollisionFlag = MASS_COLLISION_FLAG;
    private ForceMethod forceMethod = FORCE_METHOD;
    private EnumSet<SpringScaffolding> springScaffoldings = SPRING_SCAFFOLDINGS;

    public static Builder create() {
      return new Builder();
    }

    public Builder sideLength(double sideLength) {
      this.sideLength = sideLength;
      return this;
    }

    public Builder massSideLengthRatio(double massSideLengthRatio) {
      this.massSideLengthRatio = massSideLengthRatio;
      return this;
    }

    public Builder springF(double springF) {
      this.springF = springF;
      return this;
    }

    public Builder springD(double springD) {
      this.springD = springD;
      return this;
    }

    public Builder massLinearDamping(double massLinearDamping) {
      this.massLinearDamping = massLinearDamping;
      return this;
    }

    public Builder massAngularDamping(double massAngularDamping) {
      this.massAngularDamping = massAngularDamping;
      return this;
    }

    public Builder maxForce(double maxForce) {
      this.maxForce = maxForce;
      return this;
    }

    public Builder areaRatioOffset(double areaRatioOffset) {
      this.areaRatioOffset = areaRatioOffset;
      return this;
    }

    public Builder friction(double friction) {
      this.friction = friction;
      return this;
    }

    public Builder restitution(double restitution) {
      this.restitution = restitution;
      return this;
    }

    public Builder mass(double mass) {
      this.mass = mass;
      return this;
    }

    public Builder ropeJointsFlag(boolean limitContractionFlag) {
      this.limitContractionFlag = limitContractionFlag;
      return this;
    }

    public Builder massCollisionFlag(boolean massCollisionFlag) {
      this.massCollisionFlag = massCollisionFlag;
      return this;
    }

    public Builder forceMethod(ForceMethod forceMethod) {
      this.forceMethod = forceMethod;
      return this;
    }

    public Builder springScaffoldings(EnumSet<SpringScaffolding> springScaffoldings) {
      this.springScaffoldings = springScaffoldings;
      return this;
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

    public void setSideLength(double sideLength) {
      this.sideLength = sideLength;
    }

    public void setMassSideLengthRatio(double massSideLengthRatio) {
      this.massSideLengthRatio = massSideLengthRatio;
    }

    public void setSpringF(double springF) {
      this.springF = springF;
    }

    public void setSpringD(double springD) {
      this.springD = springD;
    }

    public void setMassLinearDamping(double massLinearDamping) {
      this.massLinearDamping = massLinearDamping;
    }

    public void setMassAngularDamping(double massAngularDamping) {
      this.massAngularDamping = massAngularDamping;
    }

    public void setMaxForce(double maxForce) {
      this.maxForce = maxForce;
    }

    public void setAreaRatioOffset(double areaRatioOffset) {
      this.areaRatioOffset = areaRatioOffset;
    }

    public void setFriction(double friction) {
      this.friction = friction;
    }

    public void setRestitution(double restitution) {
      this.restitution = restitution;
    }

    public void setMass(double mass) {
      this.mass = mass;
    }

    public void setLimitContractionFlag(boolean limitContractionFlag) {
      this.limitContractionFlag = limitContractionFlag;
    }

    public void setMassCollisionFlag(boolean massCollisionFlag) {
      this.massCollisionFlag = massCollisionFlag;
    }

    public void setForceMethod(ForceMethod forceMethod) {
      this.forceMethod = forceMethod;
    }

    public void setSpringScaffoldings(EnumSet<SpringScaffolding> springScaffoldings) {
      this.springScaffoldings = springScaffoldings;
    }

    public Voxel build(double x, double y, Object parent) {
      return new Voxel(this, x, y, parent);
    }

    @Override
    public String toString() {
      return "Builder{" + "sideLength=" + sideLength + ", massSideLengthRatio=" + massSideLengthRatio + ", springF=" + springF + ", springD=" + springD + ", massLinearDamping=" + massLinearDamping + ", massAngularDamping=" + massAngularDamping + ", maxForce=" + maxForce + ", areaRatioOffset=" + areaRatioOffset + ", friction=" + friction + ", restitution=" + restitution + ", mass=" + mass + ", limitContractionFlag=" + limitContractionFlag + ", massCollisionFlag=" + massCollisionFlag + ", forceMethod=" + forceMethod + ", springScaffoldings=" + springScaffoldings + '}';
    }

    @Override
    public int hashCode() {
      int hash = 7;
      hash = 97 * hash + (int) (Double.doubleToLongBits(this.sideLength) ^ (Double.doubleToLongBits(this.sideLength) >>> 32));
      hash = 97 * hash + (int) (Double.doubleToLongBits(this.massSideLengthRatio) ^ (Double.doubleToLongBits(this.massSideLengthRatio) >>> 32));
      hash = 97 * hash + (int) (Double.doubleToLongBits(this.springF) ^ (Double.doubleToLongBits(this.springF) >>> 32));
      hash = 97 * hash + (int) (Double.doubleToLongBits(this.springD) ^ (Double.doubleToLongBits(this.springD) >>> 32));
      hash = 97 * hash + (int) (Double.doubleToLongBits(this.massLinearDamping) ^ (Double.doubleToLongBits(this.massLinearDamping) >>> 32));
      hash = 97 * hash + (int) (Double.doubleToLongBits(this.massAngularDamping) ^ (Double.doubleToLongBits(this.massAngularDamping) >>> 32));
      hash = 97 * hash + (int) (Double.doubleToLongBits(this.maxForce) ^ (Double.doubleToLongBits(this.maxForce) >>> 32));
      hash = 97 * hash + (int) (Double.doubleToLongBits(this.areaRatioOffset) ^ (Double.doubleToLongBits(this.areaRatioOffset) >>> 32));
      hash = 97 * hash + (int) (Double.doubleToLongBits(this.friction) ^ (Double.doubleToLongBits(this.friction) >>> 32));
      hash = 97 * hash + (int) (Double.doubleToLongBits(this.restitution) ^ (Double.doubleToLongBits(this.restitution) >>> 32));
      hash = 97 * hash + (int) (Double.doubleToLongBits(this.mass) ^ (Double.doubleToLongBits(this.mass) >>> 32));
      hash = 97 * hash + (this.limitContractionFlag ? 1 : 0);
      hash = 97 * hash + (this.massCollisionFlag ? 1 : 0);
      hash = 97 * hash + Objects.hashCode(this.forceMethod);
      hash = 97 * hash + Objects.hashCode(this.springScaffoldings);
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
      final Builder other = (Builder) obj;
      if (Double.doubleToLongBits(this.sideLength) != Double.doubleToLongBits(other.sideLength)) {
        return false;
      }
      if (Double.doubleToLongBits(this.massSideLengthRatio) != Double.doubleToLongBits(other.massSideLengthRatio)) {
        return false;
      }
      if (Double.doubleToLongBits(this.springF) != Double.doubleToLongBits(other.springF)) {
        return false;
      }
      if (Double.doubleToLongBits(this.springD) != Double.doubleToLongBits(other.springD)) {
        return false;
      }
      if (Double.doubleToLongBits(this.massLinearDamping) != Double.doubleToLongBits(other.massLinearDamping)) {
        return false;
      }
      if (Double.doubleToLongBits(this.massAngularDamping) != Double.doubleToLongBits(other.massAngularDamping)) {
        return false;
      }
      if (Double.doubleToLongBits(this.maxForce) != Double.doubleToLongBits(other.maxForce)) {
        return false;
      }
      if (Double.doubleToLongBits(this.areaRatioOffset) != Double.doubleToLongBits(other.areaRatioOffset)) {
        return false;
      }
      if (Double.doubleToLongBits(this.friction) != Double.doubleToLongBits(other.friction)) {
        return false;
      }
      if (Double.doubleToLongBits(this.restitution) != Double.doubleToLongBits(other.restitution)) {
        return false;
      }
      if (Double.doubleToLongBits(this.mass) != Double.doubleToLongBits(other.mass)) {
        return false;
      }
      if (this.limitContractionFlag != other.limitContractionFlag) {
        return false;
      }
      if (this.massCollisionFlag != other.massCollisionFlag) {
        return false;
      }
      if (this.forceMethod != other.forceMethod) {
        return false;
      }
      if (!Objects.equals(this.springScaffoldings, other.springScaffoldings)) {
        return false;
      }
      return true;
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

  private final double sideLength;
  private final double massSideLengthRatio;
  private final double springF;
  private final double springD;
  private final double massLinearDamping;
  private final double massAngularDamping;
  private final double maxForce;
  private final double areaRatioOffset;
  private final double friction;
  private final double restitution;
  private final double mass;
  private final boolean limitContractionFlag;
  private final boolean massCollisionFlag;
  private final ForceMethod forceMethod;
  private final EnumSet<SpringScaffolding> springScaffoldings;

  private final Body[] vertexBodies;
  private final DistanceJoint[] springJoints;
  private final RopeJoint[] ropeJoints;

  private double lastAppliedForce = 0d;

  private Voxel() {
    this(Builder.create(), 0d, 0d, null);
  }

  private Voxel(Builder builder, double x, double y, Object parent) {
    //set fields
    sideLength = builder.getSideLength();
    massSideLengthRatio = builder.getMassSideLengthRatio();
    springF = builder.getSpringF();
    springD = builder.getSpringD();
    massLinearDamping = builder.getMassLinearDamping();
    massAngularDamping = builder.getMassAngularDamping();
    maxForce = builder.getMaxForce();
    areaRatioOffset = builder.getAreaRatioOffset();
    friction = builder.getFriction();
    restitution = builder.getRestitution();
    mass = builder.getMass();
    limitContractionFlag = builder.isLimitContractionFlag();
    massCollisionFlag = builder.isMassCollisionFlag();
    forceMethod = builder.getForceMethod();
    springScaffoldings = builder.getSpringScaffoldings();
    //compute densities
    double massSideLength = sideLength * massSideLengthRatio;
    double density = mass * massSideLength / massSideLength / 4;
    //build bodies
    vertexBodies = new Body[4];
    vertexBodies[0] = new Body(1); //NW
    vertexBodies[1] = new Body(1); //NE
    vertexBodies[2] = new Body(1); //SE
    vertexBodies[3] = new Body(1); //SW
    for (Body vertexBody : vertexBodies) {
      vertexBody.setUserData(parent);
    }
    vertexBodies[0].addFixture(new Rectangle(massSideLength, massSideLength), density, friction, restitution);
    vertexBodies[1].addFixture(new Rectangle(massSideLength, massSideLength), density, friction, restitution);
    vertexBodies[2].addFixture(new Rectangle(massSideLength, massSideLength), density, friction, restitution);
    vertexBodies[3].addFixture(new Rectangle(massSideLength, massSideLength), density, friction, restitution);
    vertexBodies[0].translate(-(sideLength / 2d - massSideLength / 2d), +(sideLength / 2d - massSideLength / 2d));
    vertexBodies[1].translate(+(sideLength / 2d - massSideLength / 2d), +(sideLength / 2d - massSideLength / 2d));
    vertexBodies[2].translate(+(sideLength / 2d - massSideLength / 2d), -(sideLength / 2d - massSideLength / 2d));
    vertexBodies[3].translate(-(sideLength / 2d - massSideLength / 2d), -(sideLength / 2d - massSideLength / 2d));
    ParentFilter filter = new ParentFilter(parent);
    for (Body body : vertexBodies) {
      body.setMass(MassType.NORMAL);
      body.setLinearDamping(massLinearDamping);
      body.setAngularDamping(massAngularDamping);
      body.translate(x, y);
      if (massCollisionFlag) {
        body.getFixture(0).setFilter(filter);
      }
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
    ropeJoints = (RopeJoint[]) localRopeJoints.toArray(new RopeJoint[0]);
    //build distance joints
    List<DistanceJoint> allSpringJoints = new ArrayList<>();
    double minSideLenght = Math.sqrt(sideLength * sideLength * (1d - areaRatioOffset));
    double maxSideLenght = Math.sqrt(sideLength * sideLength * (1d + areaRatioOffset));
    SpringRange sideParallelRange = new SpringRange(minSideLenght - 2d * massSideLength, sideLength - 2d * massSideLength, maxSideLenght - 2d * massSideLength);
    SpringRange sideCrossRange = new SpringRange(Math.sqrt(massSideLength * massSideLength + sideParallelRange.min * sideParallelRange.min), Math.sqrt(massSideLength * massSideLength + sideParallelRange.rest * sideParallelRange.rest), Math.sqrt(massSideLength * massSideLength + sideParallelRange.max * sideParallelRange.max));
    SpringRange centralCrossRange = new SpringRange((minSideLenght - massSideLength) * Math.sqrt(2d), (sideLength - massSideLength) * Math.sqrt(2d), (maxSideLenght - massSideLength) * Math.sqrt(2d));
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
    springJoints = (DistanceJoint[]) allSpringJoints.toArray(new DistanceJoint[0]);
  }

  @Override
  public ImmutableObject immutable() {
    //add enclosing
    Poly poly = new Poly(
            new Point2(getIndexedVertex(0, 3)),
            new Point2(getIndexedVertex(1, 2)),
            new Point2(getIndexedVertex(2, 1)),
            new Point2(getIndexedVertex(3, 0))
    );
    ImmutablePoly immutablePoly = new ImmutablePoly(poly, getClass());
    // TODO add components for sensors
    /*
    EnumMap<Voxel.Sensor, Double> sensorReadings = new EnumMap<>(Voxel.Sensor.class);
    for (Voxel.Sensor sensor : Voxel.Sensor.values()) {
      sensorReadings.put(sensor, getSensorReading(sensor));
    }
    */
    //add parts
    for (Body body : vertexBodies) {
      immutablePoly.getChildren().add(new ImmutablePoly(rectangleToPoly(body), Body.class));      
    }
    //add joints
    for (DistanceJoint joint : springJoints) {
      immutablePoly.getChildren().add(new ImmutableVector(new Point2(joint.getAnchor1()), new Point2(joint.getAnchor2()), Body.class));      
    }
    return immutablePoly;
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
      vertices[i] = new Point2(tV);
    }
    return new Poly(vertices);
  }

  @Override
  public void addTo(World world) {
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
    Poly poly = new Poly(
            new Point2(getIndexedVertex(0, 3)),
            new Point2(getIndexedVertex(1, 2)),
            new Point2(getIndexedVertex(2, 1)),
            new Point2(getIndexedVertex(3, 0))
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
    Vector2 downSide = vertexBodies[3].getWorldCenter().copy().subtract(vertexBodies[2].getWorldCenter());
    return (upSide.getDirection() + downSide.getDirection()) / 2d;
  }

  public void translate(Vector2 v) {
    for (Body body : vertexBodies) {
      body.translate(v);
    }
  }

  private double ratioOfOverlappingVertexBodies() {
    double c = 0d;
    for (int i = 0; i < vertexBodies.length; i++) {
      for (int j = i + 1; j < vertexBodies.length; j++) {
        double d = vertexBodies[i].getWorldCenter().distance(vertexBodies[j].getWorldCenter());
        if (d < sideLength * massSideLengthRatio) {
          c = c + 1d;
        }
      }
    }
    return 2d * c / (double) (vertexBodies.length * (vertexBodies.length - 1));
  }

  public double getSensorReading(Sensor sensor) {
    switch (sensor) {
      case AREA_RATIO:
        return getAreaRatio();
      case VELOCITY_MAGNITUDE:
        return getLinearVelocity().getMagnitude();
      case ANGLE:
        return getAngle();
      case X_VELOCITY:
        return getLinearVelocity().x;
      case Y_VELOCITY:
        return getLinearVelocity().y;
      case X_ROT_VELOCITY:
        return getLinearVelocity().copy().dot(new Vector2(getAngle()));
      case Y_ROT_VELOCITY:
        return getLinearVelocity().copy().dot(new Vector2(getAngle() + Math.PI / 2d));
      case BROKEN_RATIO:
        return ratioOfOverlappingVertexBodies();
      case LAST_APPLIED_FORCE:
        return getLastAppliedForce();
      case TOUCHING:
        return isInTouch() ? 1d : 0d;
      default:
        return 0d;
    }
  }

  private boolean isInTouch() {
    for (Body vertexBody : vertexBodies) {
      List<Body> inContactBodies = vertexBody.getInContactBodies(false);
      for (Body inContactBody : inContactBodies) {
        Object userData = inContactBody.getUserData();
        if (userData == null) {
          return true;
        } else if (userData != vertexBody.getUserData()) {
          return true;
        }
      }
    }
    return false;
  }

}
