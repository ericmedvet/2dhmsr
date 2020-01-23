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
package it.units.erallab.hmsrobots.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class UtilTest {

  @BeforeAll
  public static void setUpClass() throws Exception {
  }

  @AfterAll
  public static void tearDownClass() throws Exception {
  }

  @BeforeEach
  public void setUp() throws Exception {
  }

  @AfterEach
  public void tearDown() throws Exception {
  }

  /**
   * Test of gridLargestConnected method, of class Util.
   */
  @Test
  public void testGridLargestConnected() {
    System.out.println("gridLargestConnected");
    Grid<Boolean> kGrid = Grid.create(7, 3, false);
    //1st region
    kGrid.set(0, 0, true);
    kGrid.set(1, 0, true);
    kGrid.set(0, 1, true);
    //2nd regions
    kGrid.set(3, 0, true);
    kGrid.set(3, 1, true);
    kGrid.set(3, 2, true);
    kGrid.set(2, 2, true);
    //3rd region
    kGrid.set(6, 2, true);
    Grid<Boolean> expResult = Grid.create(kGrid);
    //2nd regions
    expResult.set(3, 0, true);
    expResult.set(3, 1, true);
    expResult.set(3, 2, true);
    expResult.set(2, 2, true);
    Grid<Boolean> result = Util.gridLargestConnected(kGrid, b -> b);
    assertEquals(expResult, result);
  }

  /**
   * Test of cropGrid method, of class Util.
   */
  @Test
  public void testCropGrid() {
    System.out.println("cropGrid");
    Grid<Integer> inGrid = Grid.create(5, 4, 0);
    inGrid.set(3, 1, 1);
    inGrid.set(3, 2, 2);
    inGrid.set(2, 3, 2);
    inGrid.set(4, 3, 3);
    Grid<Integer> expResult = Grid.create(3, 3, 0);
    expResult.set(1, 0, 1);
    expResult.set(1, 1, 2);
    expResult.set(0, 2, 2);
    expResult.set(2, 2, 3);
    Grid<Integer> result = Util.cropGrid(inGrid, i -> i > 0);
    assertEquals(expResult, result);
  }

}
