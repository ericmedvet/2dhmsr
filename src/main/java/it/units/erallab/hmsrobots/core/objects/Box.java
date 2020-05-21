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

import it.units.erallab.hmsrobots.core.objects.immutable.Immutable;
import it.units.erallab.hmsrobots.util.Point2;
import it.units.erallab.hmsrobots.util.Poly;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Box implements WorldObject {

  private final static double FRICTION = 1d;
  private final static double RESTITUTION = 0.5d;

  private final Body body;

  public Box(double x, double y, double w, double h, double angle, double mass) {
    body = new Body(1);
    body.addFixture(new Rectangle(w, h), mass / w / h, FRICTION, RESTITUTION);
    body.setMass(MassType.NORMAL);
    body.rotate(angle);
    body.translate(new Vector2(x, y));
  }

  @Override
  public Immutable immutable() {
    Rectangle rectangle = (Rectangle) body.getFixture(0).getShape();
    return new it.units.erallab.hmsrobots.core.objects.immutable.Box(Poly.build(
        Point2.build(rectangle.getVertices()[0]),
        Point2.build(rectangle.getVertices()[1]),
        Point2.build(rectangle.getVertices()[2]),
        Point2.build(rectangle.getVertices()[3])
    ));
  }

  @Override
  public void addTo(World world) {
    world.addBody(body);
  }

}
