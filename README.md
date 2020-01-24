# 2D-VSR-Sim
2D-VSR-Sim is a Java framework for experimenting with a 2-D version of the *voxel-based soft robots* (VSRs) [1].

VSRs are composed of many simple soft blocks (called *voxels*) that can change their volumes: the way voxels are assembled defines the *body* of the VSR, whereas the law according to which voxels change their volume over the time defines the *brain* of the VSR.
Design of VSRs body and brain can be automatized by means of optimization techniques.

**2D-VSR-Sim is an optimization-friendly VSR simulator** that focuses on two key steps of optimization: what to optimize and towards which goal. It offers a consistent interface to the different components (e.g., body, brain, specific mechanisms for control signal propagation) of a VSR which are suitable for optimization and to the task the VSR is requested to perform (e.g., locomotion, grasping of moving objects).
2D-VSR-Sim **is not** a software for doing the actual optimization. As a consequence, it leaves users (i.e., researchers and practitioners) great freedom on how to optimize: different techniques, e.g., evolutionary computation or reinforcement learning, can be used.

## VSR model in brief
All the details of the model can be found in [2].
In brief, a voxel is a soft 2-D block, i.e., a deformable square modeled with four rigid bodies (square masses), a number of spring-damper systems that constitute a scaffolding, and a number of ropes. A VSR is modeled as a collection of voxels organized in a 2-D grid, each voxel in the grid being rigidly connected with the voxel above, below, on the left, and on the right. The way a VSR behaves is determined by a *controller*. Most of the properties of the VSR model are **configurable by the user**.
2D-VSR-Sim exploits an existing physics engine, [dyn4j](http://www.dyn4j.org/), for solving the mechanical model defined by a VSR subjected to the forces caused by the actuation determined by its controller and by the interaction with other bodies (typically, the ground).

A graphical representation of a moving VSR:
![A graphical representation of a moving VSR](/assets/frames.png)

## Using the sofware
2D-VSR-Sim is meant to be used within or together with another software that performs the actual optimization.
This software is organized as a Java package containing the classes and the interfaces that represent the VSR model and related concepts.
The voxel is represented by the `Voxel` class and its parameters can be specified using the `Voxel.Builder` class, using the builder pattern. The VSR is represented by the `VoxelCompound` class; a description of a VSR, that can be used for building a VSR accordingly, is represented by the `VoxelCompound.Description` class.
A controller is represented by the interface `Controller`: a controller exploiting sensing can be realized by extending the abstract class `ClosedLoopController`, that takes care of collecting the sensor readings and makes them available to the inheriting class. A task, i.e., some activity whose degree of accomplishment can be evaluated quantitatively according to one or more indexes, is described by the interface `Task`.

2D-VSR-Sim provides a mechanism for keeping track of an ongoing simulation based on the observer pattern. A `SnapshotListener` interface represents the observer that is notified of progresses in the simulation, each in the form of a `Snapshot`: the latter is an immutable representation of the state of all the objects (e.g., positions of voxels, values of their sensor readings) in the simulation at a given time. There are two listeners implementing this interface:
 - `GridOnlineViewer` renders a visualization of the simulated world within a GUI;
 - `GridFileWriter` produces a video file.

Both can process multiple simulations together, organized in a grid. The possibility of visualizing many simulations together can be useful, for example, for comparing different stages of an optimization.

The GUI for `GridOnlineViewer`:
![The GUI of the simulation viewer](/assets/gui.png)
On top of the GUI, a set of checkboxes allows the user to customize the visualization with immediate effect. Several measures can be visualized through the fill color of the voxel or other means; voxels SDSs and masses can be visualized too, as well as other useful information.

### Sample code
A brief fragment of code using for setting up a VSR and assessing it in the task of locomotion.
```java
Locomotion locomotion = new Locomotion(
  60,
  Locomotion.createTerrain("flat"),
  Lists.newArrayList(
    Locomotion.Metric.TRAVEL_X_VELOCITY,
    Locomotion.Metric.AVG_SUM_OF_SQUARED_CONTROL_SIGNALS
  ),
  1,
  new Settings()
);
final Voxel.Builder hardMaterial = Voxel.Builder.create()
  .springF(25)
  .springScaffoldings(EnumSet.allOf(
    Voxel.SpringScaffolding.class));
final Voxel.Builder softMaterial = Voxel.Builder.create()
  .springF(5)
  .springScaffoldings(EnumSet.of(
    Voxel.SpringScaffolding.SIDE_EXTERNAL,
    Voxel.SpringScaffolding.CENTRAL_CROSS));
int w = 4;
int h = 2;
VoxelCompound.Description description = new VoxelCompound.Description(
  Grid.create(
    w, h,
    (x, y) -> (y == 0) ? materialHard : materialSoft
  ),
  new TimeFunction(Grid.create(
    w, h,
    (x, y) -> (Double t) -> Math.sin(-2 * Math.PI * t + Math.PI * ((double) x / (double) w))
  ))
);
List<Double> result = locomotion.apply(description);
```

## References
1. Hiller, Lipson. "[Automatic design and manufacture of soft robots.](https://ieeexplore.ieee.org/abstract/document/6096440)" IEEE Transactions on Robotics 28.2 (2011): 457-466 
2. Medvet, Bartoli, De Lorenzo, Seriani. "[2D-VSR-Sim: an Optimization-friendly Simulator of 2-D Voxel-based Soft Robots](https://arxiv.org/abs/2001.08617)" arXiv cs.RO: 2001.08617
