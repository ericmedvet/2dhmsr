/*
 * Copyright (C) 2021 Eric Medvet <eric.medvet@gmail.com> (as Eric Medvet <eric.medvet@gmail.com>)
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

package it.units.erallab.hmsrobots.viewers.drawers;

import it.units.erallab.hmsrobots.behavior.BehaviorUtils;
import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.objects.Ground;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.snapshots.RobotShape;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.VoxelPoly;
import it.units.erallab.hmsrobots.viewers.AllRobotFollower;

import java.util.function.Function;
import java.util.function.Supplier;

public class Drawers {
  private Drawers() {
  }

  public static Drawer world() {
    return Drawer.transform(
        new AllRobotFollower(1.5d, 2),
        Drawer.of(
            new PolyDrawer(PolyDrawer.TEXTURE_PAINT, SubtreeDrawer.Extractor.matches(null, Ground.class, null)),
            new VoxelDrawer(),
            new SensorReadingsSectorDrawer(),
            new LidarDrawer()
        )
    );
  }

  public static Drawer miniWorld() {
    return Drawer.transform(
        new AllRobotFollower(5, 4),
        Drawer.of(
            new PolyDrawer(SubtreeDrawer.Extractor.matches(null, Ground.class, null)),
            new VoxelDrawer()
        )
    );
  }

  public static Drawer signalAndSpectrum(int robotIndex, double windowT, double minF, double maxF, int nBins, String title, Supplier<Function<Snapshot, Double>> functionSupplier) {
    return Drawer.of(
        Drawer.clip(
            BoundingBox.of(0d, 0d, 1d, .5d),
            Drawer.of(
                Drawer.clear(),
                new SignalDrawer(
                    SubtreeDrawer.Extractor.matches(RobotShape.class, Robot.class, null), //TODO should use robotIndex
                    functionSupplier.get(),
                    windowT
                ),
                Drawer.text(title)
            )
        ),
        Drawer.clip(
            BoundingBox.of(0d, 0.5d, 1d, 1d),
            Drawer.of(
                Drawer.clear(),
                new SpectrumDrawer(
                    SubtreeDrawer.Extractor.matches(RobotShape.class, Robot.class, null), //TODO should use robotIndex
                    functionSupplier.get(),
                    windowT, minF, maxF, nBins
                )
            )
        )
    );
  }

  public static Drawer spectra(int robotIndex, double windowT, double minF, double maxF, int nBins, boolean firstDifference) {
    return Drawer.of(
        Drawer.clip(
            BoundingBox.of(0d, 0d, .333d, 1d),
            signalAndSpectrum(
                robotIndex, windowT, minF, maxF, nBins, "x" + (firstDifference ? "'" : ""),
                () -> BehaviorUtils.voxelPolies()
                    .andThen(BehaviorUtils::getCentralElement)
                    .andThen(p -> p.center().x)
            )
        ),
        Drawer.clip(
            BoundingBox.of(0.333d, 0d, .666d, 1d),
            signalAndSpectrum(
                robotIndex, windowT, minF, maxF, nBins, "y" + (firstDifference ? "'" : ""),
                () -> BehaviorUtils.voxelPolies()
                    .andThen(BehaviorUtils::getCentralElement)
                    .andThen(p -> p.center().y)
            )
        ),
        Drawer.clip(
            BoundingBox.of(0.666d, 0d, 1d, 1d),
            signalAndSpectrum(
                robotIndex, windowT, minF, maxF, nBins, "angle" + (firstDifference ? "'" : ""),
                () -> BehaviorUtils.voxelPolies()
                    .andThen(BehaviorUtils::getCentralElement)
                    .andThen(VoxelPoly::getAngle)
            )
        )
    );
  }

  public static Drawer basic(String string) {
    return Drawer.of(
        Drawer.clear(),
        world(),
        new InfoDrawer(string)
    );
  }

  public static Drawer basicWithMiniWorld(String string) {
    return Drawer.of(
        Drawer.clear(),
        world(),
        Drawer.clip(
            BoundingBox.of(0.5d, 0.01d, 0.95d, 0.2d),
            miniWorld()
        ),
        new InfoDrawer(string)
    );
  }

  public static Drawer basicWithMiniWorldAndSpectra(String string) {
    return Drawer.of(
        Drawer.clear(),
        Drawer.clip(
            BoundingBox.of(0d, 0.0d, 1d, 0.5d),
            Drawer.of(
                world(),
                Drawer.clip(
                    BoundingBox.of(0.5d, 0.01d, 0.95d, 0.2d),
                    miniWorld()
                )
            )
        ),
        Drawer.clip(
            BoundingBox.of(0d, 0.5d, 1d, 1d),
            spectra(0, 5, 0, 2, 8, true)
        ),
        new InfoDrawer(string)
    );
  }

  public static Drawer basic() {
    return basic("");
  }

  public static Drawer basicWithMiniWorld() {
    return basicWithMiniWorld("");
  }


}
