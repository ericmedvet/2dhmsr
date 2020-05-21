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
package it.units.erallab.hmsrobots.core.controllers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 *
 * @author eric
 */
public class MultiLayerPerceptronTest {

  public MultiLayerPerceptronTest() {
  }

  @org.junit.jupiter.api.BeforeAll
  public static void setUpClass() throws Exception {
  }

  @org.junit.jupiter.api.AfterAll
  public static void tearDownClass() throws Exception {
  }

  @org.junit.jupiter.api.BeforeEach
  public void setUp() throws Exception {
  }

  @org.junit.jupiter.api.AfterEach
  public void tearDown() throws Exception {
  }

  /**
   * Test of unflat method, of class MultiLayerPerceptron.
   */
  @org.junit.jupiter.api.Test
  public void testUnflat() {
    System.out.println("unflat");
    double[] flatWeights = new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
    int[] neurons = new int[]{2, 3, 1};
    double[][][] expResult = new double[2][][];
    expResult[0] = new double[][]{new double[]{1, 2, 3}, new double[]{4, 5, 6}};
    expResult[1] = new double[][]{new double[]{7}, new double[]{8}, new double[]{9}};
    double[][][] result = MultiLayerPerceptron.unflat(flatWeights, neurons);
    assertArrayEquals(expResult, result);
  }

  /**
   * Test of flat method, of class MultiLayerPerceptron.
   */
  @org.junit.jupiter.api.Test
  public void testFlat() {
    System.out.println("flat");
    double[][][] unflatWeights = new double[2][][];
    unflatWeights[0] = new double[][]{new double[]{1, 2, 3}, new double[]{4, 5, 6}};
    unflatWeights[1] = new double[][]{new double[]{7}, new double[]{8}, new double[]{9}};
    int[] neurons = new int[]{2, 3, 1};
    double[] expResult = new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
    double[] result = MultiLayerPerceptron.flat(unflatWeights, neurons);
    assertArrayEquals(expResult, result);
  }

  /**
   * Test of apply method, of class MultiLayerPerceptron.
   */
  @Test
  public void testApply() {
    System.out.println("apply");
    double[] input = new double[]{2};
    int[] neurons = new int[]{2, 2, 1};
    double[] weights = new double[]{1, 0, 1, 2, 1, -1};
    MultiLayerPerceptron instance = new MultiLayerPerceptron(MultiLayerPerceptron.ActivationFunction.RELU, neurons, weights);
    double[] expResult = new double[]{1};
    double[] result = instance.apply(input);
    assertArrayEquals(expResult, result);
  }

}
