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
import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Ground implements WorldObject {

  private static final double MIN_Y_THICKNESS = 10d;

  private final List<Body> bodies;
  private final List<Vector2> polygon;
  
  public Ground(double[] xs, double[] ys) {
    if (xs.length != ys.length) {
      throw new IllegalArgumentException("xs[] and ys[] must have the same length");
    }
    if (xs.length<2) {
      throw new IllegalArgumentException("There must be at least 2 points");
    }
    double[] sortedXs = Arrays.copyOf(xs, xs.length);
    Arrays.sort(sortedXs);
    if (!Arrays.equals(xs, sortedXs)) {
      throw new IllegalArgumentException("x coordinates must be sorted");
    }
    //init collections
    bodies = new ArrayList<>(xs.length-1);
    polygon = new ArrayList<>(xs.length+2);
    //find min y
    double minY = Arrays.stream(ys).min().orElse(0d);
    polygon.add(new Vector2(0,-MIN_Y_THICKNESS));
    //build bodies and polygon
    for (int i = 1; i<xs.length; i++) {
      Polygon bodyPoly = new Polygon(              
              new Vector2(0, ys[i-1]),
              new Vector2(0, minY-MIN_Y_THICKNESS),
              new Vector2(xs[i]-xs[i-1], minY-MIN_Y_THICKNESS),
              new Vector2(xs[i]-xs[i-1], ys[i])     
      );
      Body body = new Body(1);
      body.addFixture(bodyPoly);
      body.setMass(MassType.INFINITE);
      body.translate(xs[i-1], 0);
      body.translate(-xs[0], -minY);
      bodies.add(body);
      polygon.add(new Vector2(xs[i-1]-xs[0], ys[i-1]-minY));
    }
    polygon.add(new Vector2(xs[xs.length-1]-xs[0], ys[xs.length-1]-minY));
    polygon.add(new Vector2(xs[xs.length-1]-xs[0], -MIN_Y_THICKNESS));
  }

  @Override
  public Immutable immutable() {
    Point2[] vertices = new Point2[polygon.size()];
    for (int i = 0; i<vertices.length; i++) {
      vertices[i] = Point2.build(polygon.get(i));
    }
    return new it.units.erallab.hmsrobots.core.objects.immutable.Ground(Poly.build(vertices));
  }

  @Override
  public void addTo(World world) {
    for (Body body : bodies) {
      world.addBody(body);
    }
  }

  public List<Body> getBodies() {
    return bodies;
  }

}
