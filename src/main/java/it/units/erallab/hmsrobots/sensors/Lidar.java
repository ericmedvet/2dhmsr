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
package it.units.erallab.hmsrobots.sensors;

import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.util.Configurable;
import it.units.erallab.hmsrobots.util.ConfigurableField;
import org.dyn4j.dynamics.RaycastResult;
import org.dyn4j.geometry.Ray;

import java.util.*;

public class Lidar implements Sensor, Configurable<Lidar> {

    public enum Side {
        // between -180 and 180 degrees
        N(135, 45),
        E(45, -45),
        S(-45, -135),
        W(-135, 135);

        // in degrees
        private final int startAngle;
        private final int endAngle;

        Side(int startAngle, int endAngle) {
            this.startAngle = startAngle;
            this.endAngle = endAngle;
        }

        public double getStartAngle() {
            return startAngle;
        }

        public double getEndAngle() {
            return endAngle;
        }

        public double angleDifference() {
            double angleDiff = Math.abs(startAngle - endAngle);
            if (angleDiff > 180) {
                angleDiff = 360 - angleDiff;
            }
            return angleDiff;
        }
    }

    private final HashMap<Side, Integer> raysPerSide;
    @ConfigurableField
    private final double rayLength;
    @ConfigurableField
    private final double[] rayDirections;
    private final Domain[] domains;

    public Lidar(HashMap<Side, Integer> raysPerSide, double rayLength) {
        this.raysPerSide = raysPerSide;
        this.rayLength = rayLength;
        int numRays = 0;
        for (Integer rays : raysPerSide.values()) {
            numRays += rays;
        }
        rayDirections = new double[numRays];
        domains = new Domain[numRays];
        Arrays.fill(domains, Domain.build(0d, 1d));
    }

    @Override
    public Domain[] domains() {
        return domains;
    }

    @Override
    public double[] sense(Voxel voxel, double t) {
        double[] rayHits = new double[domains.length];
        int rayHitsIdx = 0;
        // List of objects the ray intersects
        List<RaycastResult> results = new ArrayList<>();

        for (Map.Entry<Side, Integer> entry : raysPerSide.entrySet()) {
            Side side = entry.getKey();
            Integer numRays = entry.getValue();
            for (int rayIdx = 0; rayIdx < numRays; rayIdx++) {
                double direction = Math.toRadians(side.getStartAngle() - rayIdx * (Math.abs(side.angleDifference()) / numRays));
                if (Math.abs(direction) > Math.PI) {
                    direction = 2 * Math.PI - Math.abs(direction);
                }
                // Create a ray from the given start point towards the given direction
                Ray ray = new Ray(voxel.getCenter(), direction);
                rayDirections[rayHitsIdx] = direction;
                results.clear();
                // if the flag is false, the results list will contain the closest result (if any)
                voxel.getWorld().raycast(ray, rayLength, true, false, true, results);
                if (results.isEmpty()) {
                    rayHits[rayHitsIdx] = 1d;
                } else {
                    rayHits[rayHitsIdx] = results.get(0).getRaycast().getDistance() / rayLength;
                }
                rayHitsIdx++;
            }
        }

        return rayHits;
    }
}
