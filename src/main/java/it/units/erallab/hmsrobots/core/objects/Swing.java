package it.units.erallab.hmsrobots.core.objects;

import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.geometry.Poly;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.joint.Joint;
import org.dyn4j.dynamics.joint.RevoluteJoint;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

import java.util.Arrays;
import java.util.List;

/**
 * @author Federico Pigozzi <pigozzife@gmail.com>
 */
public class Swing extends RigidBody {

  public static final double PLATFORM_HEIGHT = 2.0D;
  private final Joint<Body> joint;
  private final Polygon polygon;

  public Swing(double halfPlatformWidth, double platformHeight, double impulse) {
    Circle pivotPolygon = new Circle(0.000000001);
    polygon = new Polygon(
        new Vector2(-halfPlatformWidth, platformHeight + PLATFORM_HEIGHT),
        new Vector2(-halfPlatformWidth, platformHeight),
        new Vector2(halfPlatformWidth, platformHeight),
        new Vector2(halfPlatformWidth, platformHeight + PLATFORM_HEIGHT)
    );
    Body pivot = new Body();
    pivot.addFixture(pivotPolygon);
    pivot.setMass(MassType.INFINITE);
    pivot.setUserData(Swing.class);
    Body platform = new Body();
    platform.setAngularDamping(1);
    platform.addFixture(polygon);
    platform.setMass(MassType.NORMAL);
    platform.setUserData(Swing.class);
    joint = new RevoluteJoint<>(pivot, platform, new Vector2(0, platformHeight));
    bodies = List.of(platform, pivot);
    platform.applyImpulse(impulse);
  }

  @Override
  public void addTo(World<Body> world) {
    super.addTo(world);
    world.addJoint(joint);
  }

  public double getAngle() {
    return bodies.get(0).getTransform().getRotation().toRadians() * 180.0 / Math.PI;
  }

  private Polygon getRotatedPlatform() {
    Vector2 translation = bodies.get(0).getTransform().getTranslation();
    double rotation = bodies.get(0).getTransform().getRotationTransform().getRotation().toRadians();
    Vector2[] vertices = new Vector2[4];
    for (int i = 0; i < vertices.length; i++) {
      vertices[i] = new Vector2(polygon.getVertices()[i].x, polygon.getVertices()[i].y);
    }
    Polygon copyPolygon = new Polygon(vertices);
    copyPolygon.translate(translation);
    copyPolygon.rotate(rotation);
    return copyPolygon;
  }

  @Override
  public Snapshot getSnapshot() {
    Polygon copyPolygon = getRotatedPlatform();
    return new Snapshot(
        Poly.of(Arrays.stream(copyPolygon.getVertices()).map(Point2::of).toArray(Point2[]::new)),
        getClass()
    );
  }

}
