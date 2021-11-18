package it.units.erallab.hmsrobots.core.objects;

import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.Polygon;

import java.util.List;

/**
 * @author Federico Pigozzi <pigozzife@gmail.com>
 */
public abstract class RigidBody implements WorldObject, Snapshottable {

  protected List<Body> bodies;
  protected Polygon polygon;

  @Override
  public void addTo(World world) {
    for (Body body : bodies) {
      world.addBody(body);
    }
  }

}
