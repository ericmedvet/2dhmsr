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

import it.units.erallab.hmsrobots.objects.immutable.Component;
import it.units.erallab.hmsrobots.objects.immutable.Poly;
import it.units.erallab.hmsrobots.objects.immutable.Compound;
import it.units.erallab.hmsrobots.objects.immutable.Point2;
import it.units.erallab.hmsrobots.objects.immutable.VoxelComponent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.World;
import org.dyn4j.dynamics.joint.DistanceJoint;
import org.dyn4j.dynamics.joint.Joint;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Transform;
import org.dyn4j.geometry.Vector2;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Voxel implements WorldObject {

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

  }

  public static class Builder {

    private final static double SIDE_LENGTH = 3d;
    private final static double MASS_SIDE_LENGTH_RATIO = .35d;
    private final static double SPRING_F = 25d;
    private final static double SPRING_D = 1d;
    private final static double MAX_FORCE = 1000d; //not used in forceMethod=DISTANCE
    private final static double AREA_RATIO_OFFSET = 0.25d; //not used in forceMethod=FORCE
    private final static double FRICTION = 0.5d;
    private final static double RESTITUTION = 0.1d;
    private final static double MASS = 1d;
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
    private double maxForce = MAX_FORCE;
    private double areaRatioOffset = AREA_RATIO_OFFSET;
    private double friction = FRICTION;
    private double restitution = RESTITUTION;
    private double mass = MASS;
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

    public ForceMethod getForceMethod() {
      return forceMethod;
    }

    public EnumSet<SpringScaffolding> getSpringScaffoldings() {
      return springScaffoldings;
    }

    public Voxel build(double x, double y) {
      return new Voxel(this, x, y);
    }

  }

  private final double sideLength;
  private final double massSideLengthRatio;
  private final double springF;
  private final double springD;
  private final double maxForce;
  private final double areaRatioOffset;
  private final double friction;
  private final double restitution;
  private final double mass;
  private final ForceMethod forceMethod;
  private final EnumSet<SpringScaffolding> springScaffoldings;

  private final Body[] vertexBodies;
  private final DistanceJoint[] joints;

  private double lastAppliedForce = 0d;
  
  private Voxel() {
    this(Builder.create(), 0d, 0d);
  }
  
  private Voxel(Builder builder, double x, double y) {
    //set fields
    sideLength = builder.getSideLength();
    massSideLengthRatio = builder.getMassSideLengthRatio();
    springF = builder.getSpringF();
    springD = builder.getSpringD();
    maxForce = builder.getMaxForce();
    areaRatioOffset = builder.getAreaRatioOffset();
    friction = builder.getFriction();
    restitution = builder.getRestitution();
    mass = builder.getMass();
    forceMethod = builder.getForceMethod();
    springScaffoldings = builder.getSpringScaffoldings();
    //compute densities
    double massSideLength = sideLength*massSideLengthRatio;
    double density = mass * massSideLength / massSideLength / 4;
    //build bodies
    vertexBodies = new Body[4];
    vertexBodies[0] = new Body(1);
    vertexBodies[1] = new Body(1);
    vertexBodies[2] = new Body(1);
    vertexBodies[3] = new Body(1);
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
      body.translate(x, y);
    }
    //build joints
    List<DistanceJoint> allJoints = new ArrayList<>();
    double minSideLenght = Math.sqrt(sideLength * sideLength * (1d - areaRatioOffset));
    double maxSideLenght = Math.sqrt(sideLength * sideLength * (1d + areaRatioOffset));
    SpringRange sideParallelRange = new SpringRange(minSideLenght - 2d * massSideLength, sideLength - 2d * massSideLength, maxSideLenght - 2d * massSideLength);
    SpringRange sideCrossRange = new SpringRange(Math.sqrt(massSideLength * massSideLength + sideParallelRange.min * sideParallelRange.min), Math.sqrt(massSideLength * massSideLength + sideParallelRange.rest * sideParallelRange.rest), Math.sqrt(massSideLength * massSideLength + sideParallelRange.max * sideParallelRange.max));
    SpringRange centralCrossRange = new SpringRange((minSideLenght - massSideLength) * Math.sqrt(2d), (sideLength - massSideLength) * Math.sqrt(2d), (maxSideLenght - massSideLength) * Math.sqrt(2d));
    if (springScaffoldings.contains(SpringScaffolding.SIDE_INTERNAL)) {
      List<DistanceJoint> localJoints = new ArrayList<>();
      localJoints.add(new DistanceJoint(vertexBodies[0], vertexBodies[1],
              vertexBodies[0].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d),
              vertexBodies[1].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d)
      ));
      localJoints.add(new DistanceJoint(vertexBodies[1], vertexBodies[2],
              vertexBodies[1].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d),
              vertexBodies[2].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d)
      ));
      localJoints.add(new DistanceJoint(vertexBodies[2], vertexBodies[3],
              vertexBodies[2].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d),
              vertexBodies[3].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d)
      ));
      localJoints.add(new DistanceJoint(vertexBodies[3], vertexBodies[0],
              vertexBodies[3].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d),
              vertexBodies[0].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d)
      ));
      for (DistanceJoint joint : localJoints) {
        joint.setUserData(sideParallelRange);
      }
      allJoints.addAll(localJoints);
    }
    if (springScaffoldings.contains(SpringScaffolding.SIDE_EXTERNAL)) {
      List<DistanceJoint> localJoints = new ArrayList<>();
      localJoints.add(new DistanceJoint(vertexBodies[0], vertexBodies[1],
              vertexBodies[0].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d),
              vertexBodies[1].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d)
      ));
      localJoints.add(new DistanceJoint(vertexBodies[1], vertexBodies[2],
              vertexBodies[1].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d),
              vertexBodies[2].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d)
      ));
      localJoints.add(new DistanceJoint(vertexBodies[2], vertexBodies[3],
              vertexBodies[2].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d),
              vertexBodies[3].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d)
      ));
      localJoints.add(new DistanceJoint(vertexBodies[3], vertexBodies[0],
              vertexBodies[3].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d),
              vertexBodies[0].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d)
      ));
      for (DistanceJoint joint : localJoints) {
        joint.setUserData(sideParallelRange);
      }
      allJoints.addAll(localJoints);
    }
    if (springScaffoldings.contains(SpringScaffolding.SIDE_CROSS)) {
      List<DistanceJoint> localJoints = new ArrayList<>();
      localJoints.add(new DistanceJoint(vertexBodies[0], vertexBodies[1],
              vertexBodies[0].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d),
              vertexBodies[1].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d)
      ));
      localJoints.add(new DistanceJoint(vertexBodies[0], vertexBodies[1],
              vertexBodies[0].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d),
              vertexBodies[1].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d)
      ));
      localJoints.add(new DistanceJoint(vertexBodies[1], vertexBodies[2],
              vertexBodies[1].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d),
              vertexBodies[2].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d)
      ));
      localJoints.add(new DistanceJoint(vertexBodies[1], vertexBodies[2],
              vertexBodies[1].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d),
              vertexBodies[2].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d)
      ));
      localJoints.add(new DistanceJoint(vertexBodies[2], vertexBodies[3],
              vertexBodies[2].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d),
              vertexBodies[3].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d)
      ));
      localJoints.add(new DistanceJoint(vertexBodies[2], vertexBodies[3],
              vertexBodies[2].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d),
              vertexBodies[3].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d)
      ));
      localJoints.add(new DistanceJoint(vertexBodies[3], vertexBodies[0],
              vertexBodies[3].getWorldCenter().copy().add(-massSideLength / 2d, +massSideLength / 2d),
              vertexBodies[0].getWorldCenter().copy().add(+massSideLength / 2d, -massSideLength / 2d)
      ));
      localJoints.add(new DistanceJoint(vertexBodies[3], vertexBodies[0],
              vertexBodies[3].getWorldCenter().copy().add(+massSideLength / 2d, +massSideLength / 2d),
              vertexBodies[0].getWorldCenter().copy().add(-massSideLength / 2d, -massSideLength / 2d)
      ));
      for (DistanceJoint joint : localJoints) {
        joint.setUserData(sideCrossRange);
      }
      allJoints.addAll(localJoints);
    }
    if (springScaffoldings.contains(SpringScaffolding.CENTRAL_CROSS)) {
      List<DistanceJoint> localJoints = new ArrayList<>();
      localJoints.add(new DistanceJoint(vertexBodies[0], vertexBodies[2],
              vertexBodies[0].getWorldCenter(),
              vertexBodies[2].getWorldCenter()
      ));
      localJoints.add(new DistanceJoint(vertexBodies[1], vertexBodies[3],
              vertexBodies[1].getWorldCenter(),
              vertexBodies[3].getWorldCenter()
      ));
      for (DistanceJoint joint : localJoints) {
        joint.setUserData(centralCrossRange);
      }
      allJoints.addAll(localJoints);
    }
    //setup joints
    for (DistanceJoint joint : allJoints) {
      joint.setDistance(((SpringRange) joint.getUserData()).rest);
      joint.setFrequency(springF);
      joint.setDampingRatio(springD);
    }
    joints = (DistanceJoint[]) allJoints.toArray(new DistanceJoint[0]);
  }

  @Override
  public Compound getSnapshot() {
    List<Component> components = new ArrayList<>(1 + 1 + vertexBodies.length + joints.length);
    //add enclosing
    Poly poly = new Poly(
            new Point2(getIndexedVertex(0, 3)),
            new Point2(getIndexedVertex(1, 2)),
            new Point2(getIndexedVertex(2, 1)),
            new Point2(getIndexedVertex(3, 0))
    );
    components.add(new VoxelComponent(lastAppliedForce, sideLength * sideLength, poly.area(), poly));
    //add parts
    for (Body body : vertexBodies) {
      components.add(new Component(Component.Type.RIGID, rectangleToPoly(body)));
    }
    //add joints
    for (DistanceJoint joint : joints) {
      components.add(new Component(Component.Type.CONNECTION, new Poly(new Point2(joint.getAnchor1()), new Point2(joint.getAnchor2()))));
    }
    return new Compound(this.getClass(), components);
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
    for (Joint joint : joints) {
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
      for (DistanceJoint joint : joints) {
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

}
