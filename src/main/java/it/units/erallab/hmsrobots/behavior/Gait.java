/*
 * Copyright (c) "Eric Medvet" 2021.
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

package it.units.erallab.hmsrobots.behavior;

import java.util.List;
import java.util.stream.IntStream;

/**
 * @author "Eric Medvet" on 2021/09/16 for 2dhmsr
 */
public class Gait {
  private final List<Footprint> footprints;
  private final double modeInterval;
  private final double coverage;
  private final double duration;
  private final double purity;

  public Gait(List<Footprint> footprints, double modeInterval, double coverage, double duration, double purity) {
    this.footprints = footprints;
    this.modeInterval = modeInterval;
    this.coverage = coverage;
    this.duration = duration;
    this.purity = purity;
  }

  public double getAvgTouchArea() {
    return footprints.stream()
        .mapToDouble(f -> IntStream.range(0, f.length())
            .mapToDouble(i -> f.getMask()[i] ? 1d : 0d)
            .sum() / (double) f.length())
        .average()
        .orElse(0d);
  }

  public double getCoverage() {
    return coverage;
  }

  public double getDuration() {
    return duration;
  }

  public List<Footprint> getFootprints() {
    return footprints;
  }

  public double getModeInterval() {
    return modeInterval;
  }

  public double getPurity() {
    return purity;
  }

  @Override
  public String toString() {
    return String.format("Gait{footprints=%s, modeInterval=%.1fs, coverage=%.2f, duration=%.1fs, purity=%.2f}",
        footprints, modeInterval, coverage, duration, purity
    );
  }

}
