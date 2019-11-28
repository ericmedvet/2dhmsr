/*
 * Copyright (C) 2019 Eric Medvet <eric.medvet@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.viewers;

import com.google.common.collect.Lists;
import it.units.erallab.hmsrobots.controllers.TimeFunction;
import it.units.erallab.hmsrobots.objects.Voxel;
import it.units.erallab.hmsrobots.objects.VoxelCompound;
import it.units.erallab.hmsrobots.problems.Episode;
import it.units.erallab.hmsrobots.problems.Locomotion;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.Util;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.Pair;
import org.dyn4j.dynamics.Settings;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class VideoGridWriter<S> implements Runnable {

  static {
    try {
      LogManager.getLogManager().readConfiguration(VideoGridWriter.class.getClassLoader().getResourceAsStream("logging.properties"));
    } catch (IOException ex) {
      //ignore
    } catch (SecurityException ex) {
      //ignore
    }
  }

  private final Grid<Pair<String, S>> namedSolutionGrid;
  private final Episode<S, ?> episode;

  private final VideoFileWriter videoFileWriter;
  private final ExecutorService executor;

  private static final Logger L = Logger.getLogger(VideoGridWriter.class.getName());

  public VideoGridWriter(Grid<Pair<String, S>> namedSolutionGrid, Episode<S, ?> episode, int w, int h, double frameRate, File file, ExecutorService executor, GraphicsDrawer.RenderingDirectives renderingDirectives) throws IOException {
    this.namedSolutionGrid = namedSolutionGrid;
    this.episode = episode;
    this.executor = executor;
    this.videoFileWriter = new VideoFileWriter(w, h, frameRate, file, Grid.create(namedSolutionGrid, Pair::getLeft), executor, renderingDirectives);
  }

  @Override
  public void run() {
    //start episodes
    List<Future<?>> results = new ArrayList<>();
    for (final Grid.Entry<Pair<String, S>> entry : namedSolutionGrid) {
      results.add(executor.submit(() -> {
        L.info(String.format("Starting %s in position (%d,%d)", episode.getClass().getSimpleName(), entry.getX(), entry.getY()));
        episode.apply(entry.getValue().getRight(), videoFileWriter.listener(entry.getX(), entry.getY()));
        L.info(String.format("Ended %s in position (%d,%d)", episode.getClass().getSimpleName(), entry.getX(), entry.getY()));
      }));
    }
    //wait for results
    for (Future<?> result : results) {
      try {
        result.get();
      } catch (InterruptedException | ExecutionException ex) {
        L.log(Level.SEVERE, String.format("Cannot obtain one result due to %s", ex), ex);
      }
    }
    //flush and write
    try {
      L.info(String.format("Starting flushing of video"));
      videoFileWriter.flush();
      L.info(String.format("Video saved"));
    } catch (IOException ex) {
      L.log(Level.SEVERE, String.format("Cannot flush video due to %s", ex), ex);
    }
  }

}
