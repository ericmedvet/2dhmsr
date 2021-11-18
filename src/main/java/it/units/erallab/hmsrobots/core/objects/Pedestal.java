package it.units.erallab.hmsrobots.core.objects;

import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.geometry.Poly;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.World;
import org.dyn4j.dynamics.joint.Joint;
import org.dyn4j.dynamics.joint.RevoluteJoint;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Vector2;

import java.util.Arrays;
import java.util.List;


public class Pedestal implements WorldObject, Snapshottable {

  private final Body platform;
  private final Body cuspid;
  private final Joint joint;
  private final List<Vector2> platformVertices;

  public Pedestal(double halfPlatformWidth, double platformHeight) {
    Circle cuspidPolygon = new Circle(1);
    Polygon platformPolygon = new Polygon(new Vector2(- halfPlatformWidth, platformHeight),
            new Vector2(- halfPlatformWidth, 0),
            new Vector2(halfPlatformWidth, 0),
            new Vector2(halfPlatformWidth, platformHeight));
    platformVertices = Arrays.asList(platformPolygon.getVertices());
    cuspid = new Body(1);
    cuspid.addFixture(cuspidPolygon);
    cuspid.setMass(MassType.INFINITE);
    cuspid.setUserData(Pedestal.class);
    platform = new Body(1);
    platform.addFixture(platformPolygon);
    platform.setMass(MassType.NORMAL);
    platform.setUserData(Pedestal.class);
    joint = new RevoluteJoint(cuspid, platform, new Vector2(0, 0));
  }

  @Override
  public Snapshot getSnapshot() {
    Point2[] vertices = new Point2[platformVertices.size()];
    for (int i = 0; i < platformVertices.size(); ++i) {
      vertices[i] = Point2.of(platformVertices.get(i));
    }
    return new Snapshot(Poly.of(vertices), getClass());
  }

  @Override
  public void addTo(World world) {
    world.addBody(cuspid);
    world.addBody(platform);
    world.addJoint(joint);
  }

}
