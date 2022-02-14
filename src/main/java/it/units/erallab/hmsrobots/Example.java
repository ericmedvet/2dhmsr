/*
 * Copyright (c) "Eric Medvet" 2022.
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

package it.units.erallab.hmsrobots;

import it.units.erallab.hmsrobots.core.controllers.TimeFunctions;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.tasks.locomotion.Locomotion;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.RobotUtils;
import it.units.erallab.hmsrobots.viewers.FramesImageBuilder;
import it.units.erallab.hmsrobots.viewers.drawers.Drawers;
import org.dyn4j.dynamics.Settings;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author "Eric Medvet" on 2021/12/29 for 2dhmsr
 */
public class Example {
  public static void main(String[] args) throws IOException {
    Locomotion locomotion = new Locomotion(20, Locomotion.createTerrain("flat"), new Settings());
    Grid<Boolean> shape = RobotUtils.buildShape("biped-4x3");
    Grid<Voxel> body = RobotUtils.buildSensorizingFunction("uniform-ax+t-0").apply(shape);
    Robot robot = new Robot(new TimeFunctions(Grid.create(
        body.getW(),
        body.getH(),
        (x, y) -> (Double t) -> Math.sin(-2 * Math.PI * t + Math.PI * ((double) x / (double) body.getW()))
    )), body);
    FramesImageBuilder framesImageBuilder = new FramesImageBuilder(
        4,
        5,
        0.2,
        300,
        200,
        FramesImageBuilder.Direction.HORIZONTAL,
        Drawers.basic()
    );
    Outcome result = locomotion.apply(robot, framesImageBuilder);
    BufferedImage image = framesImageBuilder.getImage();
    ImageIO.write(image, "png", new File("frames.png"));
    System.out.println("Outcome: " + result);
  }
}
