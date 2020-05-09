/*
 * Copyright (C) 2020 Eric Medvet <eric.medvet@gmail.com> (as luca)
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
package it.units.erallab.hmsrobots.viewers.drawers;

import it.units.erallab.hmsrobots.objects.immutable.ImmutableObject;
import it.units.erallab.hmsrobots.objects.immutable.ImmutableReading;
import it.units.erallab.hmsrobots.objects.immutable.Point2;
import it.units.erallab.hmsrobots.objects.immutable.Poly;
import it.units.erallab.hmsrobots.util.Configurable;
import it.units.erallab.hmsrobots.util.ConfigurableField;
import it.units.erallab.hmsrobots.util.Configuration;
import it.units.erallab.hmsrobots.viewers.GraphicsDrawer;

import java.awt.*;
import java.util.LinkedHashMap;

public class Lidar implements Configurable<Lidar>, Drawer {

    @ConfigurableField
    private final Color strokeColor = Color.RED;

    private Lidar() {
    }

    public static Lidar build()  {
        return new Lidar();
    }

    @Override
    public boolean draw(ImmutableObject object, Graphics2D g) {
        ImmutableReading reading = (ImmutableReading) object;
        Poly voxelPoly = (Poly) object.getShape();
        Point2 center = voxelPoly.center();
        LinkedHashMap<String, Configuration> configuration = (LinkedHashMap<String, Configuration>) reading.getConfiguration().getValue();
        double rayLength = (double) configuration.get("rayLength").getValue();
        double[] rayDirections = (double[]) configuration.get("rayDirections").getValue();
        g.setColor(strokeColor);
        for (double rayDirection : rayDirections) {
            g.draw(GraphicsDrawer.toPath(
                    center,
                    Point2.build(center.x + rayLength * Math.cos(rayDirection), center.y + rayLength * Math.sin(rayDirection))
            ));
        }
        return false;
    }

    @Override
    public boolean canDraw(Class c) {
        return it.units.erallab.hmsrobots.sensors.Lidar.class.isAssignableFrom(c);
    }
}
