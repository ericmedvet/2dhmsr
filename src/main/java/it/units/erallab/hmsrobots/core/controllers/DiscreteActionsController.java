/*
 * Copyright (C) 2022 Eric Medvet <eric.medvet@gmail.com> (as Eric Medvet <eric.medvet@gmail.com>)
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

package it.units.erallab.hmsrobots.core.controllers;

import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.tasks.locomotion.Locomotion;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.RobotUtils;
import it.units.erallab.hmsrobots.viewers.GridOnlineViewer;
import org.apache.commons.lang3.ArrayUtils;
import org.dyn4j.dynamics.Settings;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class DiscreteActionsController extends AbstractController {

  //TODO add serialization annotation

  public interface Action extends BiFunction<Double, Grid.Key, Double> {
  }

  private final int nOfInputs;
  private final List<Action> actions;
  private TimedRealFunction function;

  private final List<List<Double>> actionLastStartingTimes;
  private final int maxConcurrentActions;
  private final double actionDuration;

  public DiscreteActionsController(int nOfInputs, List<Action> actions, TimedRealFunction function, int maxConcurrentActions, double actionDuration) {
    this.nOfInputs = nOfInputs;
    this.actions = actions;
    setFunction(function);
    this.maxConcurrentActions = maxConcurrentActions;
    this.actionDuration = actionDuration;
    actionLastStartingTimes = new ArrayList<>(actions.size());
    actionLastStartingTimes.addAll(Collections.nCopies(actions.size(), new ArrayList<>()));
  }

  public DiscreteActionsController(Grid<Voxel> voxels, List<Action> actions, TimedRealFunction function, int maxConcurrentActions, double actionDuration) {
    this(CentralizedSensing.nOfInputs(voxels), actions, function, maxConcurrentActions, actionDuration);
  }

  public void setFunction(TimedRealFunction function) {
    if (function.getInputDimension() != nOfInputs || function.getOutputDimension() != actions.size() + 1) {
      throw new IllegalArgumentException(String.format(
          "Wrong dimension of input or output in provided function: R^%d->R^%d expected, R^%d->R^%d found",
          nOfInputs, actions.size() + 1,
          function.getInputDimension(), function.getOutputDimension()
      ));
    }
    this.function = function;
  }

  @Override
  public Grid<Double> computeControlSignals(double t, Grid<Voxel> voxels) {
    // 1. read the inputs and put them in a double[] inputs
    double[] inputs = voxels.values().stream()
        .filter(Objects::nonNull)
        .map(Voxel::getSensorReadings)
        .reduce(ArrayUtils::addAll)
        .orElse(new double[nOfInputs]);
    // 2. apply the function and obtain a double[] outputs
    double[] outputs = function != null ? function.apply(t, inputs) : new double[actions.size() + 1];
    // 3. obtain an action index (including 0=no action) from outputs
    int actionIndex = maxIndex(outputs);
    // 4. add the action to ongoing actions
    if (actionIndex < actions.size()) { // actions.size() means no action
      actionLastStartingTimes.get(actionIndex).add(t);
    }
    // 5. update ongoing actions (remove old actions and/or take just last ones)
    actionLastStartingTimes.replaceAll(times -> times.stream()
        .filter(t0 -> t0 > t - actionDuration)
        .sorted(Comparator.reverseOrder())
        .limit(maxConcurrentActions)
        .collect(Collectors.toList()));
    // 6. apply all ongoing actions
    Grid<Double> actuations = Grid.create(voxels, v -> 0d);
    for (int i = 0; i < actions.size(); i++) {
      for (double t0 : actionLastStartingTimes.get(i)) { // ongoing i-th actions
        double dt = t - t0;
        for (Grid.Entry<Double> e : actuations) {
          double deltaActuation = actions.get(i).apply(dt, e.key());
          actuations.set(e.key().x(), e.key().y(), e.value() + deltaActuation);
        }
      }
    }
    return actuations;
  }

  @Override
  public void reset() {
  }

  private static int maxIndex(double[] vs) {
    int index = 0;
    for (int i = 1; i < vs.length; i++) {
      if (vs[i] > vs[index]) {
        index = i;
      }
    }
    return index;
  }

  public static void main(String[] args) {
    Action a1 = (t, k) -> t > 0 ? 0 : (k.x() < 2 ? 1d : 0d);
    Action a2 = (t, k) -> t > 0.2 ? 0 : (k.x() >= 2 ? 1d : 0d);
    a2 = (t, k) -> Math.sin(2d * Math.PI * 0.5d * t + k.x() / 4d * Math.PI * 2d);
    Grid<Voxel> body = RobotUtils.buildSensorizingFunction("spinedTouch-f-f-0").apply(RobotUtils.buildShape("worm-4x2"));
    TimedRealFunction f = TimedRealFunction.from(
        (t, in) -> new double[]{
            Math.sin(2d * Math.PI * 0.5d * t) - 10d,
            Math.sin(2d * Math.PI * 0.15d * t + 1d) - 0.95d,
            0.0d
        },
        CentralizedSensing.nOfInputs(body),
        3
    );
    AbstractController controller = new DiscreteActionsController(
        RobotUtils.buildSensorizingFunction("spinedTouch-f-f-0").apply(RobotUtils.buildShape("worm-4x2")),
        List.of(a1, a2),
        f,
        1,
        1d
    );
    Robot robot = new Robot(controller, body);
    Locomotion locomotion = new Locomotion(30, Locomotion.createTerrain("hilly-1-10-0"), new Settings());
    GridOnlineViewer.run(locomotion, robot);
  }
}
