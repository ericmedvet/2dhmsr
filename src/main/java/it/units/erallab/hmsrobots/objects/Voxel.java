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
import it.units.erallab.hmsrobots.objects.immutable.VoxelComponent;
import java.util.ArrayList;
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

  public final static double SIDE_LENGHT = 3d;
  
  private final static double V_L = SIDE_LENGHT / 3d * 1d;
  private final static double SPRING_F = 25d;
  private final static double SPRING_D = 1d;
  private final static double MAX_ABS_FORCE = 400d;

  private final Body[] vertexBodies;
  private final DistanceJoint[] joints;

  private final double externalJointDistance;
  private final double diagonalJointDistance;
  
  private double lastAppliedForce = 0d;

  public Voxel(double x, double y, double mass) {
    vertexBodies = new Body[4];
    joints = new DistanceJoint[6];
    //compute densities
    double density = mass * V_L / V_L / 4;
    //build bodies
    vertexBodies[0] = new Body(1);
    vertexBodies[1] = new Body(1);
    vertexBodies[2] = new Body(1);
    vertexBodies[3] = new Body(1);
    vertexBodies[0].addFixture(new Rectangle(V_L, V_L), density);
    vertexBodies[1].addFixture(new Rectangle(V_L, V_L), density);
    vertexBodies[2].addFixture(new Rectangle(V_L, V_L), density);
    vertexBodies[3].addFixture(new Rectangle(V_L, V_L), density);
    vertexBodies[0].translate(-(SIDE_LENGHT / 2 - V_L / 2), +(SIDE_LENGHT / 2 - V_L / 2));
    vertexBodies[1].translate(+(SIDE_LENGHT / 2 - V_L / 2), +(SIDE_LENGHT / 2 - V_L / 2));
    vertexBodies[2].translate(+(SIDE_LENGHT / 2 - V_L / 2), -(SIDE_LENGHT / 2 - V_L / 2));
    vertexBodies[3].translate(-(SIDE_LENGHT / 2 - V_L / 2), -(SIDE_LENGHT / 2 - V_L / 2));
    for (Body body : vertexBodies) {
      body.setMass(MassType.NORMAL);
      body.translate(x, y);
    }
    //build external joints    
    joints[0] = new DistanceJoint(vertexBodies[0], vertexBodies[1], vertexBodies[0].getWorldCenter().copy().add(+V_L / 2, +V_L / 2), vertexBodies[1].getWorldCenter().copy().add(-V_L / 2, V_L / 2));
    joints[1] = new DistanceJoint(vertexBodies[1], vertexBodies[2], vertexBodies[1].getWorldCenter().copy().add(+V_L / 2, -V_L / 2), vertexBodies[2].getWorldCenter().copy().add(V_L / 2, V_L / 2));
    joints[2] = new DistanceJoint(vertexBodies[2], vertexBodies[3], vertexBodies[2].getWorldCenter().copy().add(-V_L / 2, -V_L / 2), vertexBodies[3].getWorldCenter().copy().add(+V_L / 2, -V_L / 2));
    joints[3] = new DistanceJoint(vertexBodies[3], vertexBodies[0], vertexBodies[3].getWorldCenter().copy().add(-V_L / 2, +V_L / 2), vertexBodies[0].getWorldCenter().copy().add(-V_L / 2, -V_L / 2));
    //build diagonal joints
    joints[4] = new DistanceJoint(vertexBodies[0], vertexBodies[2], vertexBodies[0].getWorldCenter(), vertexBodies[2].getWorldCenter());
    joints[5] = new DistanceJoint(vertexBodies[1], vertexBodies[3], vertexBodies[1].getWorldCenter(), vertexBodies[3].getWorldCenter());
    //setup joints
    for (DistanceJoint joint : joints) {
      joint.setDistance(new Vector2(joint.getAnchor1(), joint.getAnchor2()).getMagnitude());
      joint.setFrequency(SPRING_F);
      joint.setDampingRatio(SPRING_D);
    }
    externalJointDistance = joints[0].getDistance();
    diagonalJointDistance = joints[4].getDistance();
  }

  @Override
  public Compound getSnapshot() {
    List<Component> components = new ArrayList<>(1 + 1 + vertexBodies.length + joints.length);
    //add enclosing
    Poly poly = new Poly(
            getIndexedVertex(0, 3),
            getIndexedVertex(1, 2),
            getIndexedVertex(2, 1),
            getIndexedVertex(3, 0)
    );
    components.add(new VoxelComponent(lastAppliedForce, SIDE_LENGHT*SIDE_LENGHT, poly.area(), poly));
    //add parts
    for (Body body : vertexBodies) {
      components.add(new Component(Component.Type.RIGID, rectangleToPoly(body)));
    }
    //add joints
    for (DistanceJoint joint : joints) {
      components.add(new Component(Component.Type.CONNECTION, new Poly(joint.getAnchor1(), joint.getAnchor2())));
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
    Vector2[] vertices = new Vector2[4];
    Transform t = body.getTransform();
    Rectangle rectangle = (Rectangle) body.getFixture(0).getShape();
    for (int i = 0; i < 4; i++) {
      Vector2 tV = rectangle.getVertices()[i].copy();
      t.transform(tV);
      vertices[i] = tV;
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
    if (Math.abs(f)>1d) {
      f = Math.signum(f);
    }
    double xc = 0d;
    double yc = 0d;
    for (Body body : vertexBodies) {
      xc = xc + body.getWorldCenter().x;
      yc = yc + body.getWorldCenter().y;
    }
    xc = xc / (double) vertexBodies.length;
    yc = yc / (double) vertexBodies.length;
    for (Body body : vertexBodies) {
      Vector2 force = (new Vector2(xc, yc)).subtract(body.getWorldCenter()).getNormalized().multiply(f*MAX_ABS_FORCE);
      body.applyForce(force);
    }
    lastAppliedForce = f;
  }

  public double getLastAppliedForce() {
    return lastAppliedForce;
  }

  public Vector2 getLinearVelocity() {
    double x = 0d;
    double y = 0d;
    for (Body vertex : vertexBodies) {
      x = x+vertex.getLinearVelocity().x;
      y = y+vertex.getLinearVelocity().y;
    }
    return new Vector2(x/(double)vertexBodies.length, y/(double)vertexBodies.length);
  }
  
  public double getAreaRatio() {
    Poly poly = new Poly(
            getIndexedVertex(0, 3),
            getIndexedVertex(1, 2),
            getIndexedVertex(2, 1),
            getIndexedVertex(3, 0)
    );
    return poly.area()/SIDE_LENGHT/SIDE_LENGHT;
  }
    
}
