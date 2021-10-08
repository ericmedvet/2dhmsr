/*
 * Copyright (C) 2021 Giorgia Nadizar <giorgia.nadizar@gmail.com> (as Giorgia Nadizar)
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

package it.units.erallab.hmsrobots.tasks.devolocomotion;

import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DevoOutcome {

  public static class DevoStageOutcome{
    private final Robot<?> robot;
    private final Outcome outcome;

    public DevoStageOutcome(Robot<?> robot, Outcome outcome) {
      this.robot = robot;
      this.outcome = outcome;
    }
  }

  private final List<DevoStageOutcome> outcomeList;

  public DevoOutcome(List<DevoStageOutcome> outcomeList) {
    this.outcomeList = outcomeList;
  }

  public DevoOutcome(DevoStageOutcome devoStageOutcome){
    outcomeList = new ArrayList<>();
    outcomeList.add(devoStageOutcome);
  }

  public DevoOutcome(){
    outcomeList = new ArrayList<>();
  }

  public void addDevoStageOutcome(DevoStageOutcome devoStageOutcome){
    outcomeList.add(devoStageOutcome);
  }

  public List<DevoStageOutcome> getDevoOutcomes() {
    return outcomeList;
  }

  public List<Outcome> getLocomotionOutcomes(){
    return outcomeList.stream().map(devoStageOutcome -> devoStageOutcome.outcome).collect(Collectors.toList());
  }

  public List<Robot<?>> getRobots(){
    return outcomeList.stream().map(devoStageOutcome -> devoStageOutcome.robot).collect(Collectors.toList());
  }

  public int getNumberOfStages(){
    return outcomeList.size();
  }

}
