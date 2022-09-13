package it.units.erallab.hmsrobots.core.objects;

import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.geometry.Poly;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;


public class Wall extends RigidBody {

  private final Polygon polygon;

  public Wall(double startX, double startY, double width, double height) {
    bodies = new ArrayList<>();
    Polygon bodyPolygon = new Polygon(
        new Vector2(startX, startY + height),
        new Vector2(startX, startY),
        new Vector2(startX + width, startY),
        new Vector2(startX + width, startY + height));
    polygon = bodyPolygon;
    Body body = new Body();
    body.addFixture(bodyPolygon);
    body.setMass(MassType.INFINITE);
    body.setUserData(Wall.class);
    bodies.add(body);
  }

  @Override
  public Snapshot getSnapshot() {
    Point2[] vertices = new Point2[polygon.getVertices().length];
    for (int i = 0; i < vertices.length; i++) {
      vertices[i] = Point2.of(polygon.getVertices()[i]);
    }
    return new Snapshot(Poly.of(vertices), getClass());
  }

}
