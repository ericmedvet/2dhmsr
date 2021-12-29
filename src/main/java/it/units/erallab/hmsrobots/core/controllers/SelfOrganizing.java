/*
 * Copyright (C) 2021 Federico Pigozzi <pigozzife@gmail.com> (as Federico Pigozzi <pigozzife@gmail.com>)
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

import com.fasterxml.jackson.annotation.*;
import it.units.erallab.hmsrobots.core.objects.Voxel;
import it.units.erallab.hmsrobots.util.Grid;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author federico
 */
public class SelfOrganizing implements Controller {

  @JsonProperty
  private final Map<Integer, Neuron> neurons;

  @JsonCreator
  public SelfOrganizing(@JsonProperty("neurons") Map<Integer, Neuron> neurons) {
    this.neurons = new HashMap<>();
    for (Neuron entry : neurons.values()) {
      copyNeuron(entry);
    }
  }

  public SelfOrganizing(SelfOrganizing other) {
    this(other.getNeuronsMap());
  }

  public static class ActuatorNeuron extends Neuron {

    @JsonCreator
    public ActuatorNeuron(
        @JsonProperty("index") int idx,
        @JsonProperty("x") int coord1,
        @JsonProperty("y") int coord2
    ) {
      super(idx, coord1, coord2, MultiLayerPerceptron.ActivationFunction.TANH);
    }

    @Override
    public void forward(Grid<Voxel> voxels, SelfOrganizing controller) {
      Voxel voxel = voxels.get(x, y);
      message = function.apply(ingoingEdges.stream().mapToDouble(e -> propagate(e, controller)).sum());
      voxel.applyForce(message);
    }

    @Override
    public boolean isActuator() {
      return true;
    }

    @Override
    public boolean isSensing() {
      return false;
    }

    @Override
    public String toString() {
      return super.toString().replace("Neuron", "Neuron.Actuator.");
    }

  }

  public static class Edge implements Serializable {

    @JsonProperty
    private final int source;
    @JsonProperty
    private final int target;
    @JsonProperty
    private double weight;
    @JsonProperty
    private double bias;

    @JsonCreator
    public Edge(
        @JsonProperty("source") int s,
        @JsonProperty("target") int t,
        @JsonProperty("weight") double w,
        @JsonProperty("bias") double b
    ) {
      source = s;
      target = t;
      weight = w;
      bias = b;
    }

    public double[] getParams() {
      return new double[]{weight, bias};
    }

    public void setParams(List<Double> params) {
      weight = params.get(0);
      bias = params.get(1);
    }

    public int getSource() {
      return source;
    }

    public int getTarget() {
      return target;
    }

    @Override
    public int hashCode() {
      return Objects.hash(source, target);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      Edge edge = (Edge) o;
      return source == edge.source && target == edge.target;
    }

    @Override
    public String toString() {
      return "Edge{" + String.join(",", String.valueOf(source), String.valueOf(target)) + "}";
    }

  }

  public static class HiddenNeuron extends Neuron {

    @JsonCreator
    public HiddenNeuron(
        @JsonProperty("index") int idx,
        @JsonProperty("x") int coord1,
        @JsonProperty("y") int coord2,
        @JsonProperty("function") MultiLayerPerceptron.ActivationFunction a
    ) {
      super(idx, coord1, coord2, a);
    }

    @Override
    public void forward(Grid<Voxel> voxels, SelfOrganizing controller) {
      message = function.apply(ingoingEdges.stream().mapToDouble(e -> propagate(e, controller)).sum());
    }

    @Override
    public boolean isActuator() {
      return false;
    }

    @Override
    public boolean isSensing() {
      return false;
    }

    @Override
    public String toString() {
      return super.toString().replace("Neuron", "Neuron.Hidden.");
    }

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
  @JsonSubTypes({
      @JsonSubTypes.Type(value = ActuatorNeuron.class, name = "actuator"),
      @JsonSubTypes.Type(value = SensingNeuron.class, name = "sensing"),
      @JsonSubTypes.Type(value = HiddenNeuron.class, name = "hidden")
  })
  public abstract static class Neuron implements Serializable {

    @JsonProperty
    protected final int index;
    @JsonProperty
    protected final int x;
    @JsonProperty
    protected final int y;
    @JsonProperty
    protected final MultiLayerPerceptron.ActivationFunction function;
    @JsonProperty
    protected final List<SelfOrganizing.Edge> ingoingEdges;

    protected transient double message;

    protected transient double cache;

    @JsonCreator
    public Neuron(
        @JsonProperty("index") int idx,
        @JsonProperty("x") int x,
        @JsonProperty("y") int y,
        @JsonProperty("function") MultiLayerPerceptron.ActivationFunction function
    ) {
      index = idx;
      this.x = x;
      this.y = y;
      this.function = function;
      ingoingEdges = new ArrayList<>();
      resetState();
    }

    public abstract void forward(Grid<Voxel> voxels, SelfOrganizing controller);

    public abstract boolean isActuator();

    public abstract boolean isSensing();

    public void addIngoingEdge(SelfOrganizing.Edge e) {
      ingoingEdges.add(e);
    }

    public void advance() {
      cache = message;
    }

    public MultiLayerPerceptron.ActivationFunction getActivation() {
      return function;
    }

    public int getIndex() {
      return index;
    }

    public List<SelfOrganizing.Edge> getIngoingEdges() {
      return ingoingEdges;
    }

    public int getX() {
      return x;
    }

    public int getY() {
      return y;
    }

    public boolean hasInNeighbour(int other) {
      return ingoingEdges.stream().mapToInt(Edge::getSource).anyMatch(i -> i == other);
    }

    @Override
    public int hashCode() {
      return Objects.hash(index);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      Neuron neuron = (Neuron) o;
      return index == neuron.index;
    }

    @Override
    public String toString() {
      return "Neuron." + function.toString().toLowerCase() + "{" + String.join(",", String.valueOf(index),
          String.valueOf(x), String.valueOf(y),
          "[" + String.join(",", ingoingEdges.stream().map(Edge::toString).toArray(String[]::new)) + "]"
      ) + "}";
    }

    public boolean isHidden() {
      return !(isSensing() || isActuator());
    }

    protected double propagate(SelfOrganizing.Edge e, SelfOrganizing controller) {
      double[] params = e.getParams();
      return controller.getNeuronsMap().get(e.getSource()).send() * params[0] + params[1];
    }

    public void resetState() {
      message = 0.0;
      cache = 0.0;
    }

    public double send() {
      return cache;
    }

  }

  public static class SensingNeuron extends Neuron {

    @JsonProperty
    private final int numSensor;

    @JsonCreator
    public SensingNeuron(
        @JsonProperty("index") int idx,
        @JsonProperty("x") int coord1,
        @JsonProperty("y") int coord2,
        @JsonProperty("numSensor") int s
    ) {
      super(idx, coord1, coord2, MultiLayerPerceptron.ActivationFunction.TANH);
      numSensor = s;
    }

    @Override
    public void forward(Grid<Voxel> voxels, SelfOrganizing controller) {
      Voxel voxel = voxels.get(x, y);
      message = function.apply(voxel.getSensors().stream().flatMapToDouble(x -> Arrays.stream(x.getReadings()))
          .toArray()[numSensor]);
    }

    @Override
    public boolean isActuator() {
      return false;
    }

    @Override
    public boolean isSensing() {
      return true;
    }

    @Override
    public String toString() {
      return super.toString().replace("Neuron", "Neuron.Sensing.");
    }

    public int getNumSensor() {
      return numSensor;
    }

  }

  public Neuron addActuatorNeuron(int x, int y) {
    int idx = getFirstAvailableIndex();
    Neuron newNeuron = new ActuatorNeuron(idx, x, y);
    neurons.put(idx, newNeuron);
    return newNeuron;
  }

  public void addEdge(int source, int dest, double weight, double bias) {
    if (neurons.get(dest).hasInNeighbour(source)) {
      throw new RuntimeException(String.format("Adding already-present edge: [%d,%d]", source, dest));
    }
    Edge edge = new Edge(source, dest, weight, bias);
    neurons.get(dest).addIngoingEdge(edge);
  }

  public Neuron addHiddenNeuron(MultiLayerPerceptron.ActivationFunction a, int x, int y) {
    int idx = getFirstAvailableIndex();
    Neuron newNeuron = new HiddenNeuron(idx, x, y, a);
    neurons.put(idx, newNeuron);
    return newNeuron;
  }

  public Neuron addSensingNeuron(int x, int y, int s) {
    int idx = getFirstAvailableIndex();
    Neuron newNeuron = new SensingNeuron(idx, x, y, s);
    neurons.put(idx, newNeuron);
    return newNeuron;
  }

  @Override
  public void control(double t, Grid<Voxel> voxels) {
    getNeurons().forEach(n -> n.forward(voxels, this));
    getNeurons().forEach(Neuron::advance);
  }

  public void copyNeuron(Neuron neuron) {
    int idx = neuron.getIndex();
    Neuron newComer;
    if (neuron instanceof SensingNeuron) {
      newComer = new SensingNeuron(idx, neuron.getX(), neuron.getY(), ((SensingNeuron) neuron).getNumSensor());
    } else if (neuron instanceof ActuatorNeuron) {
      newComer = new ActuatorNeuron(idx, neuron.getX(), neuron.getY());
    } else if (neuron instanceof HiddenNeuron) {
      newComer = new HiddenNeuron(idx, neuron.getX(), neuron.getY(), neuron.getActivation());
      if (neurons.containsKey(idx)) {
        throw new IllegalArgumentException(String.format("Inserting already-present neuron: %d", idx));
      }
    } else {
      throw new RuntimeException(String.format("Unknown Neuron type: %s", neuron.getClass()));
    }
    neurons.put(idx, newComer);
    for (Edge edge : neuron.getIngoingEdges()) {
      addEdge(edge.getSource(), edge.getTarget(), edge.getParams()[0], edge.getParams()[1]);
    }
  }

  public List<Edge> getEdges() {
    return neurons.values()
        .stream()
        .flatMap(n -> n.getIngoingEdges().stream()).toList();
  }

  private int getFirstAvailableIndex() {
    for (int i = 0; i < this.neurons.size(); ++i) {
      if (!neurons.containsKey(i)) {
        return i;
      }
    }
    return neurons.size();
  }

  public List<Neuron> getNeurons() {
    return neurons.values().stream().toList();
  }

  public Map<Integer, Neuron> getNeuronsMap() {
    return neurons;
  }

  public List<Edge> getOutgoingEdges(Neuron neuron) {
    return getOutgoingEdges(neuron.getIndex());
  }

  public List<Edge> getOutgoingEdges(int idx) {
    List<Edge> out = new ArrayList<>();
    for (Edge edge : getEdges()) {
      if (edge.getSource() == idx) {
        out.add(edge);
      }
    }
    return out;
  }

  public Grid.Key[] getValidAndDistinctCoordinates() {
    return getNeurons().stream().map(n -> new Grid.Key(n.getX(), n.getY())).distinct().toArray(Grid.Key[]::new);
  }

  public void removeEdge(Edge edge) {
    removeEdge(edge.getSource(), edge.getTarget());
  }

  public void removeEdge(int source, int target) {
    neurons.get(target).getIngoingEdges().removeIf(e -> e.getSource() == source);
  }

  public void removeNeuron(Neuron neuron) {
    for (Edge edge : getEdges()) {
      if (edge.getSource() == neuron.getIndex() || edge.getTarget() == neuron.getIndex()) {
        removeEdge(edge);
      }
    }
    neurons.remove(neuron.getIndex());
  }

  @Override
  public void reset() {
    getNeurons().forEach(Neuron::resetState);
  }

  @Override
  public String toString() {
    return "SelfOrganizing{" + neurons.values().stream().map(Neuron::toString).collect(Collectors.joining("-")) + "}";
  }

}