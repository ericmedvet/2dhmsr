package it.units.erallab.hmsrobots.core.controllers;

import it.units.erallab.hmsrobots.core.objects.ControllableVoxel;
import it.units.erallab.hmsrobots.util.Grid;

public class ArrowInteractiveController  extends AbstractController<ControllableVoxel> {

  boolean keyPressed = false;
  boolean[] keyPressedArray;

  @Override
  public Grid<Double> computeControlSignals(double t, Grid<? extends ControllableVoxel> voxels) {
    //Grid<Double> values = Grid.create(voxels, v -> keyPressed?-1d:1d);
    Grid<Double> values = Grid.create(voxels, v -> 0d);
    //values.set(); // x,y,value -> posso farlo per le varie posizioni con set
    return values;
  }

  @Override
  public void reset() {

  }

  public void setKeyPressed(boolean keyPressed) {
    this.keyPressed = keyPressed;
  }
}
