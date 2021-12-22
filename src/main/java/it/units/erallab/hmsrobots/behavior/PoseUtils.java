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

  private static class ClusterableGridKey implements Clusterable {
    private final Grid.Key key;

    public ClusterableGridKey(Grid.Key key) {
      this.key = key;
    }

    @Override
    public double[] getPoint() {
      return new double[]{key.getX(), key.getY()};
    }
  }

  private static class ClusterablePosture implements Clusterable {
    private final Set<Grid.Key> pose;
    private final Grid<Boolean> posture;

    public ClusterablePosture(Set<Grid.Key> pose, Grid<Boolean> posture) {
      this.pose = pose;
      this.posture = posture;
    }

    @Override
    public double[] getPoint() {
      return posture.values().stream().mapToDouble(b -> b ? 1d : 0d).toArray();
    }
  }

  private PoseUtils() {
  }

  public static Set<Set<Grid.Key>> computeCardinalPoses(Grid<Boolean> shape) {
    Set<Grid.Key> left = shape.stream().filter(e -> e.getX() < shape.getW() / 4d).filter(Grid.Entry::getValue).collect(Collectors.toSet());
    Set<Grid.Key> right = shape.stream().filter(e -> e.getX() >= shape.getW() * 3d / 4d).filter(Grid.Entry::getValue).collect(Collectors.toSet());
    Set<Grid.Key> center = shape.stream().filter(e -> e.getX() >= shape.getW() / 4d && e.getX() < shape.getW() * 3d / 4d).filter(Grid.Entry::getValue).collect(Collectors.toSet());
    double midCenterY = center.stream().mapToDouble(Grid.Key::getY).average().orElse(0d);
    Set<Grid.Key> top = center.stream().filter(e -> e.getY() <= midCenterY).collect(Collectors.toSet());
    Set<Grid.Key> bottom = center.stream().filter(e -> e.getY() > midCenterY).collect(Collectors.toSet());
    return new LinkedHashSet<>(List.of(left, top, bottom, right));
  }

  public static Grid<Boolean> computeDynamicPosture(Grid<Boolean> shape, Set<Grid.Key> pose, ControllableVoxel voxelPrototype, double finalT, int gridSize) {
    Grid<ControllableVoxel> body = Grid.create(shape, b -> b ? SerializationUtils.clone(voxelPrototype) : null);
    PosesController controller = new PosesController(0.5d, List.of(pose));
    Robot<ControllableVoxel> robot = new Robot<>(controller, body);
    FinalPosture finalPosture = new FinalPosture(gridSize, finalT);
    return finalPosture.apply(robot);
  }

  public static Set<Set<Grid.Key>> computeClusteredByPosturePoses(Grid<Boolean> shape, Set<Set<Grid.Key>> startingPoses, int n, int seed, ControllableVoxel voxelPrototype, double finalT, int gridSize) {
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
    Collection<ClusterablePosture> points = allPoses.stream().map(p -> new ClusterablePosture(p, computeDynamicPosture(shape, p, voxelPrototype, finalT, gridSize))).collect(Collectors.toList());
    //cluster postures in nPoses clusters
    KMeansPlusPlusClusterer<ClusterablePosture> clusterer = new KMeansPlusPlusClusterer<>(n, -1, new ManhattanDistance(), new JDKRandomGenerator(seed));
    List<CentroidCluster<ClusterablePosture>> clusters = clusterer.cluster(points);
    //find representatives
    return clusters.stream().map(c -> center(c, new ManhattanDistance()).pose).collect(Collectors.toSet());
  }

  private static <K extends Clusterable> K center(CentroidCluster<K> cluster, DistanceMeasure d) {
    return cluster.getPoints().stream().min(Comparator.comparingDouble(k -> d.compute(k.getPoint(), cluster.getCenter().getPoint()))).orElseThrow();
  }

  public static Set<Set<Grid.Key>> computeClusteredByPositionPoses(Grid<Boolean> shape, int n, int seed) {
    Collection<ClusterableGridKey> points = shape.stream().filter(Grid.Entry::getValue).map(ClusterableGridKey::new).collect(Collectors.toList());
    KMeansPlusPlusClusterer<ClusterableGridKey> clusterer = new KMeansPlusPlusClusterer<>(n, -1, new EuclideanDistance(), new JDKRandomGenerator(seed));
    List<CentroidCluster<ClusterableGridKey>> clusters = clusterer.cluster(points);
    return clusters.stream().map(c -> c.getPoints().stream().map(cgk -> cgk.key).collect(Collectors.toSet())).collect(Collectors.toSet());
  }

}
