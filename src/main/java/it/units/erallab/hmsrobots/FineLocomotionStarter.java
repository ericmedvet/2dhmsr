/*
 * Copyright (C) 2020 Eric Medvet <eric.medvet@gmail.com> (as eric)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots;

import it.units.erallab.hmsrobots.core.objects.ControllableVoxel;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.tasks.Locomotion;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.SerializableFunction;
import org.dyn4j.dynamics.Settings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author eric
 * @created 2020/07/14
 * @project TwoDimHighlyModularSoftRobots
 */
public class FineLocomotionStarter {

  public static void main(String[] args) {
    //stdin: description of the robot: grid coords, amplitude, frequency, phase
    //stout: position/rotation of voxels at each time step
    //args: type (video/headless), terrain string, starting y, simulation time
    //example: java -cp classes/artifacts/2dhmsr_jar/2dhmsr.jar it.units.erallab.hmsrobots.FineLocomotionStarter csv 1,0:1000,100:2000,10 10

    //create locomotion task
    Locomotion locomotion = new Locomotion(
        Double.parseDouble(args[3]),
        new double[][]{
            Arrays.stream(args[1].split(":"))
                .map(s -> s.split(",")[0])
                .mapToDouble(Double::parseDouble)
                .toArray(),
            Arrays.stream(args[1].split(":"))
                .map(s -> s.split(",")[1])
                .mapToDouble(Double::parseDouble)
                .toArray()
        },
        List.of(Locomotion.Metric.values()),
        new Settings()
    );
    //create robot
    final Grid<SerializableFunction<Double, Double>> timeFunctionGrid;
    try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
      List<Grid.Entry<SerializableFunction<Double, Double>>> entries = new ArrayList<>();
      while (true) {
        String line = br.readLine();
        if ((line == null) || (line.isEmpty())) {
          break;
        }
        String[] pieces = line.split(",");
        int x = Integer.parseInt(pieces[0]);
        int y = Integer.parseInt(pieces[1]);
        double amplitude = Double.parseDouble(pieces[2]);
        double frequency = Double.parseDouble(pieces[3]);
        double phase = Double.parseDouble(pieces[4]);
        entries.add(new Grid.Entry<>(
            x, y,
            t -> amplitude * Math.sin(-2d * Math.PI * t * frequency + phase))
        );
      }
      int minX = entries.stream().mapToInt(Grid.Entry::getX).min().orElse(0);
      int maxX = entries.stream().mapToInt(Grid.Entry::getX).max().orElse(0);
      int minY = entries.stream().mapToInt(Grid.Entry::getY).min().orElse(0);
      int maxY = entries.stream().mapToInt(Grid.Entry::getY).max().orElse(0);
      timeFunctionGrid = Grid.create(maxX - minX + 1, maxY - minY + 1);
      entries.forEach(e -> {
        timeFunctionGrid.set(e.getX() - minX, e.getY() - minY, e.getValue());
      });
    } catch (IOException e) {
      System.err.printf(String.format("Cannot read robot description from standard input: %s%n", e));
      return;
    }
    Robot<ControllableVoxel> robot;

    System.out.println(timeFunctionGrid);
  }
}
