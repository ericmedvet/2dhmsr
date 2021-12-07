package it.units.erallab.hmsrobots.core.controllers;

import it.units.erallab.hmsrobots.core.objects.ControllableVoxel;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.math3.util.Pair;

import java.util.*;

public class BasicInteractiveController extends AbstractController<ControllableVoxel> {

  boolean keyPressed = false;
  boolean[] isKeyPressed;
  private List<Collection<Pair<Integer, Integer>>> voxelDivision; //Lista di collection di coordinate

  public BasicInteractiveController() {
    isKeyPressed = new boolean[2];
  }

  private void setVoxelDivision(Grid<Boolean> shape) {
    int numberOfVoxels = (int)shape.count(Objects::nonNull);
    int index = 0;
    voxelDivision = new ArrayList<>();
    Collection<Pair<Integer, Integer>> division1 = new ArrayList<>();
    Collection<Pair<Integer, Integer>> division2 = new ArrayList<>();
    for (var val : shape) {
      if (val.getValue() != null) {
        Pair<Integer, Integer> pair = new Pair<>(val.getX(), val.getY());
        if (index < numberOfVoxels / 2){
          //System.out.println(val.getX()+" "+numberOfVoxels/2);
          division1.add(pair);
        } else {
          division2.add(pair);
        }
        index++;
      }
    }
    voxelDivision.add(division1);
    voxelDivision.add(division2);
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

    //Grid<Double> values = Grid.create(voxels, v -> keyPressed?-1d:1d);

    Grid<Boolean> shape = getShape(voxels);
    //System.out.println(isKeyPressed[0]+" "+isKeyPressed[1]);
    setVoxelDivision(shape);
    Grid<Double> values = Grid.create(voxels, v -> -1d);
    for (int i = 0; i < isKeyPressed.length; i++) {
      for (var cord : voxelDivision.get(i)) {
        values.set(cord.getFirst(), cord.getSecond(), isKeyPressed[i]?-1d:1d);
      }
    }
    return values;
  }

  @Override
  public void reset() {

  }

  public void setKeyPressed(boolean keyPressed, int index) {
    //System.out.println(keyPressed);
    this.isKeyPressed[index] = keyPressed;
    //this.keyPressed = keyPressed;
  }
}
