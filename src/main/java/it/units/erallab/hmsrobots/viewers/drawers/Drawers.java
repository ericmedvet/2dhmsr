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
import it.units.erallab.hmsrobots.core.controllers.DistributedSensing;
import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.objects.Ground;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.snapshots.MLPState;
import it.units.erallab.hmsrobots.core.snapshots.RobotShape;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.VoxelPoly;
import it.units.erallab.hmsrobots.tasks.devolocomotion.DistanceBasedDevoLocomotion;
import it.units.erallab.hmsrobots.viewers.AllRobotFollower;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class Drawers {
  private Drawers() {
  }

  public static Drawer basic(String string) {
    return Drawer.of(
        Drawer.clear(),
        world(),
        new InfoDrawer(string)
    );
  }

  public static Drawer basic() {
    return basic("");
  }

  public static Drawer basicDistributedWithMiniWorld(String string) {
    return Drawer.of(
        Drawer.clip(
            BoundingBox.of(0d, 0d, 1d, 0.5d),
            Drawers.basicWithMiniWorld(string)
        ),
        Drawer.clip(
            BoundingBox.of(0d, 0.5d, 1d, 1d),
            Drawer.of(
                Drawer.clear(),
                new DistributedDrawer(SubtreeDrawer.Extractor.matches(
                    DistributedSensing.DistributedSensingState.class,
                    null,
                    null
                ), 15d
                )
            )
        )
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

  public static Drawer basicWithMiniWorld() {
    return basicWithMiniWorld("");
  }

  public static Drawer basicWithMiniWorldAndBrain(String string) {
    return Drawer.of(
        Drawer.clip(
            BoundingBox.of(0d, 0d, 1d, 0.5d),
            Drawers.basicWithMiniWorld(string)
        ),
        Drawer.clip(
            BoundingBox.of(0d, 0.5d, 1d, 1d),
            Drawer.of(
                Drawer.clear(),
                new MLPDrawer(SubtreeDrawer.Extractor.matches(MLPState.class, null, null), 15d,
                    Set.of(
                        MLPDrawer.Part.ACTIVATION_VALUES,
                        MLPDrawer.Part.WEIGHTS,
                        MLPDrawer.Part.LEGEND,
                        MLPDrawer.Part.T_AXIS,
                        MLPDrawer.Part.STRUCTURE_AXIS,
                        MLPDrawer.Part.HISTOGRAM
                    )
                )
            )
        )
    );
  }

  public static Drawer basicWithMiniWorldAndBrainUsage(String string) {
    return Drawer.of(
        Drawer.clip(
            BoundingBox.of(0d, 0d, 1d, 0.5d),
            Drawers.basicWithMiniWorld(string)
        ),
        Drawer.clip(
            BoundingBox.of(0d, 0.5d, 1d, 1d),
            Drawer.of(
                Drawer.clear(),
                new MLPDrawer(SubtreeDrawer.Extractor.matches(MLPState.class, null, null), 15d,
                    Set.of(
                        MLPDrawer.Part.ACTIVATION_VALUES,
                        MLPDrawer.Part.WEIGHTS,
                        MLPDrawer.Part.VARIANCE_AND_WEIGHTS,
                        MLPDrawer.Part.LEGEND,
                        MLPDrawer.Part.T_AXIS,
                        MLPDrawer.Part.STRUCTURE_AXIS,
                        MLPDrawer.Part.HISTOGRAM
                    )
                )
            )
        )
    );
  }

  public static Drawer basicWithMiniWorldAndFootprintsAndPosture(String string) {
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
            footprintsAndPosture(0, 5, 4, 8)
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
            spectra(0, 5, 0, 2, 8)
        ),
        new InfoDrawer(string)
    );
  }

  public static Drawer footprintsAndPosture(int robotIndex, double windowT, int nFootprint, int nPosture) {
    return Drawer.of(
        Drawer.clip(
            BoundingBox.of(0d, 0.0d, .666d, 1d),
            new FootprintDrawer(
                SubtreeDrawer.Extractor.matches(RobotShape.class, Robot.class, robotIndex),
                windowT,
                nFootprint
            )
        ),
        Drawer.clip(
            BoundingBox.of(0.666d, 0.0d, 1d, 1d),
            new PostureDrawer(
                SubtreeDrawer.Extractor.matches(RobotShape.class, Robot.class, robotIndex),
                windowT,
                nPosture,
                true
            )
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

  public static Drawer signalAndSpectrum(
      int robotIndex,
      double windowT,
      double minF,
      double maxF,
      int nBins,
      String title,
      Supplier<Function<Snapshot, Double>> functionSupplier
  ) {
    return Drawer.of(
        Drawer.clip(
            BoundingBox.of(0d, 0d, 1d, .5d),
            Drawer.of(
                Drawer.clear(),
                new SignalDrawer(
                    SubtreeDrawer.Extractor.matches(RobotShape.class, Robot.class, robotIndex),
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
                    SubtreeDrawer.Extractor.matches(RobotShape.class, Robot.class, robotIndex),
                    functionSupplier.get(),
                    windowT, minF, maxF, nBins
                )
            )
        )
    );
  }

  public static Drawer spectra(int robotIndex, double windowT, double minF, double maxF, int nBins) {
    return Drawer.of(
        Drawer.clip(
            BoundingBox.of(0d, 0d, .333d, 1d),
            signalAndSpectrum(
                robotIndex, windowT, minF, maxF, nBins, "vx",
                () -> BehaviorUtils.voxelPolyGrid()
                    .andThen(BehaviorUtils::getCentralElement)
                    .andThen(p -> p.getLinearVelocity().x())
            )
        ),
        Drawer.clip(
            BoundingBox.of(0.333d, 0d, .666d, 1d),
            signalAndSpectrum(
                robotIndex, windowT, minF, maxF, nBins, "vy",
                () -> BehaviorUtils.voxelPolyGrid()
                    .andThen(BehaviorUtils::getCentralElement)
                    .andThen(p -> p.getLinearVelocity().y())
            )
        ),
        Drawer.clip(
            BoundingBox.of(0.666d, 0d, 1d, 1d),
            signalAndSpectrum(
                robotIndex, windowT, minF, maxF, nBins, "angle",
                () -> BehaviorUtils.voxelPolyGrid()
                    .andThen(BehaviorUtils::getCentralElement)
                    .andThen(VoxelPoly::getAngle)
            )
        )
    );
  }

  public static Drawer world() {
    return Drawer.transform(
        new AllRobotFollower(1.5d, 2),
        Drawer.of(
            new TargetDrawer(SubtreeDrawer.Extractor.matches(
                null,
                DistanceBasedDevoLocomotion.CurrentTarget.class,
                null
            )),
            new PolyDrawer(PolyDrawer.TEXTURE_PAINT, SubtreeDrawer.Extractor.matches(null, Ground.class, null)),
            new VoxelDrawer(),
            new SensorReadingsSectorDrawer(),
            new LidarDrawer()
        )
    );
  }

}