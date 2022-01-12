package it.units.erallab.hmsrobots.core.objects;

import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import org.dyn4j.dynamics.Body;
import org.dyn4j.world.World;

import java.util.List;

/**
 * @author Federico Pigozzi <pigozzife@gmail.com>
 */
public abstract class RigidBody implements WorldObject, Snapshottable {

  protected List<Body> bodies;

  @Override
  public void addTo(World<Body> world) {
    for (Body body : bodies) {
      world.addBody(body);
    }
  }

}
