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
import it.units.erallab.hmsrobots.core.sensors.Touch;
import it.units.erallab.hmsrobots.core.snapshots.VoxelPoly;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.joint.DistanceJoint;
import org.dyn4j.geometry.Vector2;

import java.util.EnumSet;

public class ControllableVoxel extends Voxel {

  public enum ForceMethod {
    DISTANCE, FORCE
  }

  public static final double MAX_FORCE = 100d;
  public static final ForceMethod FORCE_METHOD = ForceMethod.DISTANCE;

  @JsonProperty
  private final double maxForce; //not used in distance forceMethod
  @JsonProperty
  private final ForceMethod forceMethod;

  private transient double controlEnergy;
  private transient double lastAppliedForce;

  @JsonCreator
  public ControllableVoxel(
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
      @JsonProperty("forceMethod") ForceMethod forceMethod
  ) {
    super(sideLength, massSideLengthRatio, springF, springD, massLinearDamping, massAngularDamping, friction, restitution, mass, limitContractionFlag, massCollisionFlag, areaRatioMaxDelta, springScaffoldings);
    this.maxForce = maxForce;
    this.forceMethod = forceMethod;
  }

  public ControllableVoxel(double maxForce, ForceMethod forceMethod) {
    this.maxForce = maxForce;
    this.forceMethod = forceMethod;
  }

  public ControllableVoxel() {
    this(MAX_FORCE, FORCE_METHOD);
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
        Voxel.SpringRange range = (SpringRange) joint.getUserData();
        if (f >= 0) { // shrink
          joint.setDistance(range.rest - (range.rest - range.min) * f);
        } else if (f < 0) { // expand
          joint.setDistance(range.rest + (range.max - range.rest) * -f);
        }
      }
    }
  }

  public double getLastAppliedForce() {
    return lastAppliedForce;
  }

  public double getControlEnergy() {
    return controlEnergy;
  }

  @Override
  public VoxelPoly getVoxelPoly() {
    return new VoxelPoly(
        getVertices(),
        getAngle(),
        getLinearVelocity(),
        Touch.isTouchingGround(this),
        getAreaRatio(),
        getAreaRatioEnergy(),
        getLastAppliedForce(),
        getControlEnergy()
    );
  }

  @Override
  public void reset() {
    super.reset();
    applyForce(0d);
    controlEnergy = 0d;
    lastAppliedForce = 0d;
  }

  @Override
  public void act(double t) {
    super.act(t);
    //compute energy
    double areaRatio = getAreaRatio();
    if (((areaRatio > 1d) && (lastAppliedForce < 0)) || ((areaRatio < 1d) && (lastAppliedForce > 0))) { //expanded and expand or shrunk and shrink
      controlEnergy = controlEnergy + lastAppliedForce * lastAppliedForce;
    }
  }

  @Override
  public String toString() {
    return "ControllableVoxel{" +
        "maxForce=" + maxForce +
        ", forceMethod=" + forceMethod +
        ", controlEnergy=" + controlEnergy +
        '}';
  }
}
