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

import com.google.common.collect.EvictingQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import org.dyn4j.collision.AxisAlignedBounds;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Shape;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Starter {
  
  public static void main(String[] args) {
    World world = new World(new AxisAlignedBounds(100, 100));
    
    Body floor = new Body();
    floor.addFixture(new BodyFixture(new Rectangle(100, 2)));
    floor.setMass(MassType.INFINITE);
    floor.translate(0, 0);
    
    Body ball = new Body();
    ball.addFixture(new BodyFixture(new Circle(2)));
    ball.getFixtures().get(0).setDensity(100);
    ball.setMass(MassType.NORMAL);
    ball.translate(3, 10);
    
    world.addBody(floor);
    world.addBody(ball);
        
    Viewer viewer = new Viewer();
    viewer.start();
    
    double t = 0d;
    double dt = 0.1d;
    for (int i = 0; i<10; i++) {
      world.update(dt);
      t = t+dt;
      viewer.listen(buildWorldEvent(world));
    }
  }
  
  private static WorldEvent buildWorldEvent(World world) {
    Collection<Shape> shapes = new ArrayList<>();
    double time = world.getAccumulatedTime();
    for (Iterator<Body> worldIterator = world.getBodyIterator(); worldIterator.hasNext(); ) {
      Body body = worldIterator.next();
      
      System.out.println(body.isActive());
      
      for (Iterator<BodyFixture> bodyIterator = body.getFixtureIterator(); bodyIterator.hasNext(); ) {
        
        Shape shape = bodyIterator.next().getShape();
                
        
        Rectangle rectangle = new Rectangle(shape.createAABB().getWidth(),shape.createAABB().getHeight());
        rectangle.translate(shape.getCenter().x, shape.getCenter().y);
        shapes.add(rectangle);
        System.out.printf("%5.1f,%5.1f %5.1f,%5.1f%n", rectangle.getCenter().x, rectangle.getCenter().y, rectangle.getWidth(), rectangle.getHeight());
      }
    }
    return new WorldEvent(time, shapes);
  }  
}
