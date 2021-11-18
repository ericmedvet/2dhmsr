package it.units.erallab.hmsrobots.core.objects;

import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.geometry.Poly;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.World;
import org.dyn4j.dynamics.joint.Joint;
import org.dyn4j.dynamics.joint.RevoluteJoint;
import org.dyn4j.geometry.*;

import java.util.Arrays;
import java.util.List;

/**
 * @author Federico Pigozzi <pigozzife@gmail.com>
 */
public class Pedestal extends RigidBody {

  private final Joint joint;

  public Pedestal(double halfPlatformWidth, double platformHeight) {
    Circle cuspidPolygon = new Circle(1);
    polygon = new Polygon(new Vector2(- halfPlatformWidth, platformHeight),
            new Vector2(- halfPlatformWidth, 0),
            new Vector2(halfPlatformWidth, 0),
            new Vector2(halfPlatformWidth, platformHeight));
    Body cuspid = new Body(1);
    cuspid.addFixture(cuspidPolygon);
    cuspid.setMass(MassType.INFINITE);
    cuspid.setUserData(Pedestal.class);
    Body platform = new Body(1);
    platform.addFixture(polygon);
    platform.setMass(MassType.INFINITE);
    platform.setUserData(Pedestal.class);
    joint = new RevoluteJoint(cuspid, platform, new Vector2(0, 0));
    bodies = List.of(platform, cuspid);
  }

  public void setMovable() {
    bodies.get(0).setMass(MassType.NORMAL);
  }

  @Override
  public Snapshot getSnapshot() {
    Vector2 translation = bodies.get(0).getTransform().getTranslation();
    double rotation = bodies.get(0).getTransform().getRotationTransform().getRotation();
    Polygon copyPolygon = new Polygon(polygon.getVertices());
    copyPolygon.translate(translation);
    copyPolygon.rotate(rotation);
    return new Snapshot(Poly.of(Arrays.stream(copyPolygon.getVertices()).map(Point2::of).toArray(Point2[]::new)), getClass());
  }

  @Override
  public void addTo(World world) {
    super.addTo(world);
    world.addJoint(joint);
  }

}
