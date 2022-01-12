/*
 * Copyright (C) 2020 Eric Medvet <eric.medvet@gmail.com> (as Eric Medvet <eric.medvet@gmail.com>)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.core.objects;

import it.units.erallab.hmsrobots.core.geometry.Point2;
import it.units.erallab.hmsrobots.core.geometry.Poly;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Ground extends RigidBody {

  private static final double MIN_Y_THICKNESS = 50d;

  private final double[] xs;
  private final double[] ys;
  private final List<Vector2> polygon;

  public Ground(double[] xs, double[] ys) {
    this.xs = xs;
    this.ys = ys;
    if (xs.length != ys.length) {
      throw new IllegalArgumentException("xs[] and ys[] must have the same length");
    }
    if (xs.length < 2) {
      throw new IllegalArgumentException("There must be at least 2 points");
    }
    double[] sortedXs = Arrays.copyOf(xs, xs.length);
    Arrays.sort(sortedXs);
    if (!Arrays.equals(xs, sortedXs)) {
      throw new IllegalArgumentException("x coordinates must be sorted");
    }
    //init collections
    bodies = new ArrayList<>(xs.length - 1);
    polygon = new ArrayList<>(xs.length + 2);
    //find min y
    double baseY = Arrays.stream(ys).min().getAsDouble() - MIN_Y_THICKNESS;
    polygon.add(new Vector2(0, baseY));
    //build bodies and polygon
    for (int i = 1; i < xs.length; i++) {
      Polygon bodyPoly = new Polygon(
          new Vector2(0, ys[i - 1]),
          new Vector2(0, baseY),
          new Vector2(xs[i] - xs[i - 1], baseY),
          new Vector2(xs[i] - xs[i - 1], ys[i])
      );
      Body body = new Body();
      body.addFixture(bodyPoly);
      body.setMass(MassType.INFINITE);
      body.translate(xs[i - 1], 0);
      body.setUserData(Ground.class);
      //body.translate(-xs[0], -minY);
      bodies.add(body);
      polygon.add(new Vector2(xs[i - 1], ys[i - 1]));
    }
    polygon.add(new Vector2(xs[xs.length - 1], ys[xs.length - 1]));
    polygon.add(new Vector2(xs[xs.length - 1], baseY));
  }

  @Override
  public void addTo(World<Body> world) {
    for (Body body : bodies) {
      world.addBody(body);
    }
  }

  public List<Body> getBodies() {
    return bodies;
  }

  @Override
  public Snapshot getSnapshot() {
    Point2[] vertices = new Point2[polygon.size()];
    for (int i = 0; i < vertices.length; i++) {
      vertices[i] = Point2.of(polygon.get(i));
    }
    return new Snapshot(Poly.of(vertices), getClass());
  }

  public double yAt(double x) {
    for (int i = 1; i < xs.length; i++) {
      if ((xs[i - 1] <= x) && (x <= xs[i])) {
        return (x - xs[i - 1]) * (ys[i] - ys[i - 1]) / (xs[i] - xs[i - 1]) + ys[i - 1];
      }
    }
    return Double.NaN;
  }

}
