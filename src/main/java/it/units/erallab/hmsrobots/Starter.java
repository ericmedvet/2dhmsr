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
package it.units.erallab.hmsrobots;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dyn4j.collision.AxisAlignedBounds;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.World;
import org.dyn4j.dynamics.joint.DistanceJoint;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Rectangle;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Starter {

  public static void main(String[] args) {
    World world = new World(new AxisAlignedBounds(250, 250));

    Body floor = new Body();
    floor.addFixture(new BodyFixture(new Rectangle(100, 2)));
    floor.setMass(MassType.INFINITE);
    floor.translate(0, 0);

    Body r1 = new Body();
    r1.addFixture(new BodyFixture(new Rectangle(8, 2)));
    r1.getFixtures().get(0).setDensity(100);
    r1.setMass(MassType.NORMAL);
    r1.translate(0, 70);
    Body r2 = new Body();
    r2.addFixture(new BodyFixture(new Rectangle(4, 1)));
    r2.getFixtures().get(0).setDensity(100);
    r2.setMass(MassType.NORMAL);
    r2.translate(0, 50);

    DistanceJoint j = new DistanceJoint(r1, r2, r1.getLocalCenter(), r2.getLocalCenter());
    j.setFrequency(10);
    j.setDampingRatio(1d);
    j.setDistance(5);

    world.addBody(floor);
    world.addBody(r1);
    world.addBody(r2);
    world.addJoint(j);

    ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    Viewer viewer = new Viewer(executor);
    viewer.start();

    double dt = 0.01d;
    Runnable runnable = () -> {
      world.update(dt);
      viewer.listen(buildWorldEvent(world));
    };
    executor.scheduleAtFixedRate(runnable, 0, Math.round(dt*1000d), TimeUnit.MILLISECONDS);
  }

  private static WorldEvent buildWorldEvent(World world) {
    Collection<List<Point2D>> shapes = new ArrayList<>();
    double time = world.getAccumulatedTime();
    for (Iterator<Body> worldIterator = world.getBodyIterator(); worldIterator.hasNext();) {
      Body body = worldIterator.next();
      AffineTransform transform = new AffineTransform();
      transform.translate(body.getTransform().getTranslationX(), body.getTransform().getTranslationY());
      transform.rotate(body.getTransform().getRotation());
      for (Iterator<BodyFixture> bodyIterator = body.getFixtureIterator(); bodyIterator.hasNext();) {
        Rectangle rectangle = (Rectangle) bodyIterator.next().getShape();
        List<Point2D> shape = new ArrayList<>();
        shape.add(transform.transform(new Point2D.Double(rectangle.getCenter().x - rectangle.getWidth() / 2, rectangle.getCenter().y - rectangle.getHeight() / 2), null));
        shape.add(transform.transform(new Point2D.Double(rectangle.getCenter().x + rectangle.getWidth() / 2, rectangle.getCenter().y - rectangle.getHeight() / 2), null));
        shape.add(transform.transform(new Point2D.Double(rectangle.getCenter().x + rectangle.getWidth() / 2, rectangle.getCenter().y + rectangle.getHeight() / 2), null));
        shape.add(transform.transform(new Point2D.Double(rectangle.getCenter().x - rectangle.getWidth() / 2, rectangle.getCenter().y + rectangle.getHeight() / 2), null));
        shapes.add(shape);
      }
    }
    return new WorldEvent(time, shapes);
  }
}
