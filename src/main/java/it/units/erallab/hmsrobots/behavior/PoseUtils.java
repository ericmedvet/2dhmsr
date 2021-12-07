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

package it.units.erallab.hmsrobots.behavior;

import it.units.erallab.hmsrobots.core.controllers.PosesController;
import it.units.erallab.hmsrobots.core.objects.ControllableVoxel;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.tasks.FinalPosture;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.RobotUtils;
import it.units.erallab.hmsrobots.util.SerializationUtils;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author "Eric Medvet" on 2021/12/03 for 2dhmsr
 */
public class PoseUtils {

  private PoseUtils() {
  }

  public static List<BinaryPose> computeCardinalPoses(Grid<Boolean> shape) {
    List<Grid.Key> left = shape.stream()
        .filter(e -> e.getX() < shape.getW() / 4d)
        .filter(Grid.Entry::getValue)
        .collect(Collectors.toList());
    List<Grid.Key> right = shape.stream()
        .filter(e -> e.getX() >= shape.getW() * 3d / 4d)
        .filter(Grid.Entry::getValue)
        .collect(Collectors.toList());
    List<Grid.Key> center = shape.stream()
        .filter(e -> e.getX() >= shape.getW() / 4d && e.getX() < shape.getW() * 3d / 4d)
        .filter(Grid.Entry::getValue)
        .collect(Collectors.toList());
    double midCenterY = center.stream().mapToDouble(Grid.Key::getY).average().orElse(0d);
    List<Grid.Key> top = center.stream().filter(e -> e.getY() <= midCenterY).collect(Collectors.toList());
    List<Grid.Key> bottom = center.stream().filter(e -> e.getY() > midCenterY).collect(Collectors.toList());
    return List.of(
        new BinaryPose(left),
        new BinaryPose(top),
        new BinaryPose(bottom),
        new BinaryPose(right)
    );
  }

  public static Grid<Boolean> computeDynamicPosture(Grid<Boolean> shape, BinaryPose pose, ControllableVoxel voxelPrototype, double finalT, double n) {
    Grid<ControllableVoxel> body = Grid.create(shape, b -> b == null ? null : SerializationUtils.clone(voxelPrototype));
    PosesController controller = new PosesController(0.5d, List.of(pose));
    Robot<ControllableVoxel> robot = new Robot<>(controller, body);
    FinalPosture finalPosture = new FinalPosture(16, 4);
    return finalPosture.apply(robot);
  }

  public static List<BinaryPose> computeClusteredPoses(Grid<Boolean> shape, int nPoses, int nRegions) {
    //cluster voxels in nRegions clusters
    //compute 2^nRegions poses
    //compute all 2^nRegions postures comping from 2^nRegions poses
    //clusters postures in nPoses clusters
    return null;
  }

  public static void main(String[] args) {
    Grid<Boolean> shape = RobotUtils.buildShape("worm-8x4");
    List<BinaryPose> poses = computeCardinalPoses(shape);
    for (BinaryPose pose : poses) {
      Grid<Boolean> posture = computeDynamicPosture(shape, pose, new ControllableVoxel(), 2, 8);
      System.out.println(Grid.toString(posture, (Predicate<Boolean>) b -> b));
    }
  }
}
