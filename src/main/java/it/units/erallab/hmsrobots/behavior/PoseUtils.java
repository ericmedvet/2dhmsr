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
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.tasks.FinalPosture;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.SerializationUtils;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.apache.commons.math3.ml.distance.ManhattanDistance;
import org.apache.commons.math3.random.JDKRandomGenerator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author "Eric Medvet" on 2021/12/03 for 2dhmsr
 */
public class PoseUtils {

  private PoseUtils() {
  }

  private record ClusterableGridKey(Grid.Key key) implements Clusterable {

    @Override
    public double[] getPoint() {
      return new double[]{key.x(), key.y()};
    }
  }

  private record ClusterablePosture(
      Set<Grid.Key> pose,
      Grid<Boolean> posture
  ) implements Clusterable {

    @Override
    public double[] getPoint() {
      return posture.values().stream().mapToDouble(b -> b ? 1d : 0d).toArray();
    }
  }

  private static <K extends Clusterable> K center(CentroidCluster<K> cluster, DistanceMeasure d) {
    return cluster.getPoints()
        .stream()
        .min(Comparator.comparingDouble(k -> d.compute(k.getPoint(), cluster.getCenter().getPoint())))
        .orElseThrow();
  }

  public static Set<Set<Grid.Key>> computeCardinalPoses(Grid<Boolean> shape) {
    Set<Grid.Key> left = shape.stream()
        .filter(e -> e.key().x() < shape.getW() / 4d)
        .filter(Grid.Entry::value)
        .map(Grid.Entry::key)
        .collect(Collectors.toSet());
    Set<Grid.Key> right = shape.stream()
        .filter(e -> e.key().x() >= shape.getW() * 3d / 4d)
        .filter(Grid.Entry::value)
        .map(Grid.Entry::key)
        .collect(Collectors.toSet());
    Set<Grid.Key> center = shape.stream()
        .filter(e -> e.key().x() >= shape.getW() / 4d && e.key().x() < shape.getW() * 3d / 4d)
        .filter(Grid.Entry::value)
        .map(Grid.Entry::key)
        .collect(Collectors.toSet());
    double midCenterY = center.stream().mapToDouble(Grid.Key::y).average().orElse(0d);
    Set<Grid.Key> top = center.stream().filter(k -> k.y() <= midCenterY).collect(Collectors.toSet());
    Set<Grid.Key> bottom = center.stream().filter(k -> k.y() > midCenterY).collect(Collectors.toSet());
    return new LinkedHashSet<>(List.of(left, top, bottom, right));
  }

  public static Set<Set<Grid.Key>> computeClusteredByPositionPoses(Grid<Boolean> shape, int n, int seed) {
    Collection<ClusterableGridKey> points = shape.stream()
        .filter(Grid.Entry::value)
        .map(e -> new ClusterableGridKey(e.key()))
        .toList();
    KMeansPlusPlusClusterer<ClusterableGridKey> clusterer = new KMeansPlusPlusClusterer<>(
        n,
        -1,
        new EuclideanDistance(),
        new JDKRandomGenerator(seed)
    );
    List<CentroidCluster<ClusterableGridKey>> clusters = clusterer.cluster(points);
    return clusters.stream()
        .map(c -> c.getPoints().stream().map(cgk -> cgk.key).collect(Collectors.toSet()))
        .collect(Collectors.toSet());
  }

  public static Set<Set<Grid.Key>> computeClusteredByPosturePoses(
      Grid<Boolean> shape,
      Set<Set<Grid.Key>> startingPoses,
      int n,
      int seed,
      Voxel voxelPrototype,
      double finalT,
      int gridSize
  ) {
    List<Set<Grid.Key>> sPoses = new ArrayList<>(startingPoses);
    List<Set<Grid.Key>> allPoses = new ArrayList<>((int) Math.pow(2, sPoses.size()));
    //build expanded poses (2^|poses.size|)
    for (int i = 0; i < Math.pow(2, sPoses.size()); i++) {
      Set<Grid.Key> combinedPose = new HashSet<>();
      for (int j = 0; j < sPoses.size(); j++) {
        int remainder = (i / (int) Math.pow(2, j)) % 2;
        if (remainder == 1) {
          combinedPose.addAll(sPoses.get(j));
        }
      }
      allPoses.add(combinedPose);
    }
    //compute all postures
    Collection<ClusterablePosture> points = allPoses.stream()
        .map(p -> new ClusterablePosture(p, computeDynamicPosture(shape, p, voxelPrototype, finalT, gridSize)))
        .toList();
    //cluster postures in nPoses clusters
    KMeansPlusPlusClusterer<ClusterablePosture> clusterer = new KMeansPlusPlusClusterer<>(
        n,
        -1,
        new ManhattanDistance(),
        new JDKRandomGenerator(seed)
    );
    List<CentroidCluster<ClusterablePosture>> clusters = clusterer.cluster(points);
    //find representatives
    return clusters.stream().map(c -> center(c, new ManhattanDistance()).pose).collect(Collectors.toSet());
  }

  public static Grid<Boolean> computeDynamicPosture(
      Grid<Boolean> shape, Set<Grid.Key> pose, Voxel voxelPrototype, double finalT, int gridSize
  ) {
    Grid<Voxel> body = Grid.create(shape, b -> b ? SerializationUtils.clone(voxelPrototype) : null);
    PosesController controller = new PosesController(0.5d, List.of(pose));
    Robot robot = new Robot(controller, body);
    FinalPosture finalPosture = new FinalPosture(gridSize, finalT);
    return finalPosture.apply(robot);
  }

}
