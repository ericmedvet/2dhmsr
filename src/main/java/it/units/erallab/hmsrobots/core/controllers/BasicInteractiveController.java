package it.units.erallab.hmsrobots.core.controllers;

import it.units.erallab.hmsrobots.behavior.PoseUtils;
import it.units.erallab.hmsrobots.core.objects.ControllableVoxel;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.math3.util.Pair;

import java.util.*;

public class BasicInteractiveController extends AbstractController<ControllableVoxel> {

  private List<Boolean> isKeyPressed;
  private List<Collection<Pair<Integer, Integer>>> voxelDivision;

  public BasicInteractiveController() {
    isKeyPressed = new ArrayList<>();
    for (int i = 0; i<4; i++) {
      isKeyPressed.add(false);
    }
    System.out.println("hello");
  }

  private Grid<Boolean> getShape(Grid<? extends ControllableVoxel> voxels) {
    Grid<Boolean> shape = Grid.create(voxels, Objects::isNull);
    for (var val : voxels) {
        shape.set(val.getX(), val.getY(), voxels.get(val.getX(), val.getY()) != null);
    }
    return shape;
  }

  @Override
  public Grid<Double> computeControlSignals(double t, Grid<? extends ControllableVoxel> voxels) {
    Grid<Boolean> shape = getShape(voxels);
    List<Set<Grid.Key>> poses = new ArrayList<>(PoseUtils.computeCardinalPoses(shape));
    Grid<Double> values = Grid.create(voxels, v -> 1d);
    for (int i = 0; i < isKeyPressed.size(); i++) {
      for (Grid.Key key : poses.get(i)) {
        values.set(key.getX(), key.getY(), isKeyPressed.get(i)?-1d:1d);
        /*if (key.getX() >= 0 && key.getX() < values.getW() && key.getY() >= 0 && key.getY() < values.getH()) {
          values.set(key.getX(), key.getY(), isKeyPressed[i]?-1d:1d);
        }*/
      }
    }
    return values;
  }

  @Override
  public void reset() {

  }

  public void setKeyPressed(boolean keyPressed, int index) {
    this.isKeyPressed.set(index, keyPressed);
  }
}
