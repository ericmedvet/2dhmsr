/*
 * Copyright (C) 2020 Eric Medvet <eric.medvet@gmail.com> (as eric)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.core.objects;

import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.geometry.Poly;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Box implements WorldObject, Snapshottable {

  private final static double FRICTION = 1d;
  private final static double RESTITUTION = 0.5d;

  private final Body body;

  public Box(double w, double h, double angle, double mass) {
    body = new Body();
    body.addFixture(new Rectangle(w, h), mass / w / h, FRICTION, RESTITUTION);
    body.setMass(MassType.NORMAL);
    body.rotate(angle);
  }

  @Override
  public void addTo(World<Body> world) {
    world.addBody(body);
  }

  @Override
  public Snapshot getSnapshot() {
    Rectangle rectangle = (Rectangle) body.getFixture(0).getShape();
    return new Snapshot(
        Poly.of(
            Point2.of(rectangle.getVertices()[0]),
            Point2.of(rectangle.getVertices()[1]),
            Point2.of(rectangle.getVertices()[2]),
            Point2.of(rectangle.getVertices()[3])
        ),
        getClass()
    );
  }

  public void translate(Vector2 v) {
    body.translate(new Vector2(v.x, v.y));
  }

}
