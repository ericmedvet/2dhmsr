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
package it.units.erallab.hmsrobots.objects.snapshot;

import java.util.ArrayList;
import java.util.List;
import org.dyn4j.geometry.Vector2;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Poly {
  
  private final Vector2[] vertexes;

  public Poly(List<Vector2> vertexes) {
    this.vertexes = new Vector2[vertexes.size()];
    for (int i = 0; i<vertexes.size(); i++) {
      this.vertexes[i] = vertexes.get(i);
    }
  }
  
  public Poly(Vector2... vertexes) {
    this.vertexes = vertexes;
  }

  public Vector2[] getVertexes() {
    return vertexes;
  }    
  
}
