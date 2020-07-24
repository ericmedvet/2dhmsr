# 2D-VSR-Sim
2D-VSR-Sim is a Java framework for experimenting with a 2-D version of the *voxel-based soft robots* (VSRs) [1].

If you use this software, please cite: Medvet, Bartoli, De Lorenzo, Seriani. "[Design, Validation, and Case Studies of 2D-VSR-Sim, an Optimization-friendly Simulator of 2-D Voxel-based Soft Robots](https://arxiv.org/abs/2001.08617)" arXiv cs.RO: 2001.08617
```bibtex
@article{medvet20202d,
  title={Design, Validation, and Case Studies of 2D-VSR-Sim, an Optimization-friendly Simulator of 2-D Voxel-based Soft Robots},
  author={Medvet, Eric and Bartoli, Alberto and De Lorenzo, Andrea and Seriani, Stefano},
  journal={arXiv preprint arXiv:2001.08617},
  year={2020}
}
```

VSRs are composed of many simple soft blocks (called *voxels*) that can change their volumes: the way voxels are assembled defines the *body* of the VSR, whereas the law according to which voxels change their volume over the time defines the *brain* of the VSR.
Design of VSRs body and brain can be automatized by means of optimization techniques.

**2D-VSR-Sim is an optimization-friendly VSR simulator** that focuses on two key steps of optimization: what to optimize and towards which goal. It offers a consistent interface to the different components (e.g., body, brain, sensors, specific mechanisms for control signal propagation) of a VSR which are suitable for optimization and to the task the VSR is requested to perform (e.g., locomotion, grasping of moving objects).
2D-VSR-Sim **is not** a software for doing the actual optimization. As a consequence, it leaves users (i.e., researchers and practitioners) great freedom on how to optimize: different techniques, e.g., evolutionary computation or reinforcement learning, can be used.

## VSR model in brief
All the details of the model can be found in [2].
In brief, a voxel is a soft 2-D block, i.e., a deformable square modeled with four rigid bodies (square masses), a number of spring-damper systems that constitute a scaffolding, and a number of ropes. A VSR is modeled as a collection of voxels organized in a 2-D grid, each voxel in the grid being rigidly connected with the voxel above, below, on the left, and on the right. The way a VSR behaves is determined by a *controller* that may exploit the readings of a number of *sensors* that each voxel may be equipped with. Most of the properties of the VSR model are **configurable by the user**.
2D-VSR-Sim exploits an existing physics engine, [dyn4j](http://www.dyn4j.org/), for solving the mechanical model defined by a VSR subjected to the forces caused by the actuation determined by its controller and by the interaction with other bodies (typically, the ground).

A graphical representation of a moving VSR:
![A graphical representation of a moving VSR](/assets/frames.png)

## Using the sofware
2D-VSR-Sim is meant to be used within or together with another software that performs the actual optimization.
This software is organized as a Java package containing the classes and the interfaces that represent the VSR model and related concepts.
The voxel is represented by the `Voxel` class and its parameters, together with its sensors, can be specified using the `Voxel.Description` class. The VSR is represented by the `Robot` class; a description of a VSR, that can be used for building a VSR accordingly, is represented by the `Robot.Description` class.
A controller is represented by the interface `Controller`, a functional interface that takes ad input the sensor readings and gives as outputs the control values (one for each voxel).
A task, i.e., some activity whose degree of accomplishment can be evaluated quantitatively according to one or more indexes, is described by the interface `Task`.

2D-VSR-Sim provides a mechanism for keeping track of an ongoing simulation based on the observer pattern. A `SnapshotListener` interface represents the observer that is notified of progresses in the simulation, each in the form of a `Snapshot`: the latter is an immutable representation of the state of all the objects (e.g., positions of voxels, values of their sensor readings) in the simulation at a given time. There are two listeners implementing this interface:
 - `GridOnlineViewer` renders a visualization of the simulated world within a GUI;
 - `GridFileWriter` produces a video file.

Both can process multiple simulations together, organized in a grid. The possibility of visualizing many simulations together can be useful, for example, for comparing different stages of an optimization.

The GUI for `GridOnlineViewer`:
![The GUI of the simulation viewer](/assets/gui.png)
On top of the GUI, a set of UI controls allows the user to customize the visualization with immediate effect. Sensor readings can be visualized as well as voxels SDSs and masses.

### Sample code
A brief fragment of code using for setting up a VSR, testing it in the task of locomotion and saving an image with a few frames of the resulting behavior.
This VSR is composed of voxel of two different materials that are actuated with a periodic sinusoidal signal whose phase changes along the x-direction of the robot. 
```java
final Locomotion locomotion = new Locomotion(
    20,
    Locomotion.createTerrain("flat"),
    Lists.newArrayList(
        Locomotion.Metric.TRAVEL_X_VELOCITY,
        Locomotion.Metric.RELATIVE_CONTROL_POWER
    ),
    new Settings()
);
final ControllableVoxel hardMaterialVoxel = new ControllableVoxel(
    Voxel.SIDE_LENGTH,
    Voxel.MASS_SIDE_LENGTH_RATIO,
    50d,
    Voxel.SPRING_D,
    Voxel.MASS_LINEAR_DAMPING,
    Voxel.MASS_ANGULAR_DAMPING,
    Voxel.FRICTION,
    Voxel.RESTITUTION,
    Voxel.MASS,
    Voxel.LIMIT_CONTRACTION_FLAG,
    Voxel.MASS_COLLISION_FLAG,
    Voxel.AREA_RATIO_MAX_DELTA,
    Voxel.SPRING_SCAFFOLDINGS,
    ControllableVoxel.MAX_FORCE,
    ControllableVoxel.ForceMethod.DISTANCE
);
final ControllableVoxel softMaterialVoxel = new ControllableVoxel(
    Voxel.SIDE_LENGTH,
    Voxel.MASS_SIDE_LENGTH_RATIO,
    5d,
    Voxel.SPRING_D,
    Voxel.MASS_LINEAR_DAMPING,
    Voxel.MASS_ANGULAR_DAMPING,
    Voxel.FRICTION,
    Voxel.RESTITUTION,
    Voxel.MASS,
    Voxel.LIMIT_CONTRACTION_FLAG,
    Voxel.MASS_COLLISION_FLAG,
    Voxel.AREA_RATIO_MAX_DELTA,
    EnumSet.of(Voxel.SpringScaffolding.SIDE_EXTERNAL, Voxel.SpringScaffolding.CENTRAL_CROSS),
    ControllableVoxel.MAX_FORCE,
    ControllableVoxel.ForceMethod.DISTANCE
);
int w = 20;
int h = 5;
Robot robot = new Robot(
    new TimeFunctions(Grid.create(
        w, h,
        (x, y) -> (Double t) -> Math.sin(-2 * Math.PI * t + Math.PI * ((double) x / (double) w))
    )),
    Grid.create(
        w, h,
        (x, y) -> (y == 0) ? SerializationUtils.clone(hardMaterialVoxel) : SerializationUtils.clone(softMaterialVoxel)
    )
);
FramesFileWriter framesFileWriter = new FramesFileWriter(
    5, 5.5, 0.1, 300, 200, FramesFileWriter.Direction.HORIZONTAL,
    new File(pathToFile),
    Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
);
List<Double> result = locomotion.apply(robot, framesFileWriter);
framesFileWriter.flush();
System.out.println("Outcome: " + result);
```

#### Optimization examples: optimize phases

This piece of code shows a method for assessing a VSR whose voxels are actuated with a sinusoidal signal with different phases given the vector of phases.
This method might be the one being called by an external optimization software.
In this example, the VSR is a 10x4 worm that is assessed on locomotion on a flat terrain: the single objective is the traveled distance.

```java
public static double assessOnLocomotion(double[] phases) {
    // set robot shape and sensors
    Grid<ControllableVoxel> voxels = Grid.create(10, 4, (x, y) -> new ControllableVoxel());
    // set controller
    double f = 1d;
    Controller<ControllableVoxel> controller = new TimeFunctions(Grid.create(
        voxels.getW(),
        voxels.getH(),
        (x, y) -> (t) -> Math.sin(-2 * Math.PI * f * t + Math.PI * phases[(x + (int) Math.floor(y / voxels.getH()))]))
    ));
    Robot<ControllableVoxel> robot = new Robot<>(controller, voxels);
    // set task
    Settings settings = new Settings();
    settings.setStepFrequency(1d / 30d);
    Locomotion locomotion = new Locomotion(
        60,
        Locomotion.createTerrain("flat"),
        Lists.newArrayList(Locomotion.Metric.TRAVEL_X_VELOCITY),
        settings
    );
    // do task
    List<Double> results = locomotion.apply(robot);
    return results.get(0);
}
``` 

#### Optimization examples: optimize neural network weights

This example is similar to the one above, but here the controller of the robot is a *neural network* whose wights are subjected to optimization.
Here the robot is a 7x4 biped with a 7x2 trunk and two 2x2 legs.
The voxels have different sensors dependin on their position:
- "feet" have touch sensors (whose signal is averaged in a 1 second time window)
- "spine", i.e., the top-row of trunk have velocity (along the 2-axes) and average velocity sensors
- the remaining voxels have an area ratio sensor

```java
public static double assessOnLocomotion(double[] weights) {
    // set robot shape and sensors
    final Grid<Boolean> structure = Grid.create(7, 4, (x, y) -> (x < 2) || (x >= 5) || (y > 0));
    Grid<SensingVoxel> voxels = Grid.create(structure.getW(), structure.getH(), (x, y) -> {
      if (structure.get(x, y)) {
        if (y > 2) {
          return new SensingVoxel(Arrays.asList(
              new Velocity(true, 3d, Velocity.Axis.X, Velocity.Axis.Y),
              new Average(new Velocity(true, 3d, Velocity.Axis.X, Velocity.Axis.Y), 1d)
          ));
        }
        if (y == 0) {
          return new SensingVoxel(Arrays.asList(
              new Average(new Touch(), 1d)
          ));
        }
        return new SensingVoxel(Arrays.asList(
            new AreaRatio()
        ));
      }
      return null;
    });
    // set controller
    CentralizedMLP controller = new CentralizedMLP(
        voxels,
        new int[]{100},
        t -> 1d * Math.sin(-2d * Math.PI * t * 0.5d)
    );
    controller.setParams(weights);
    Robot<SensingVoxel> robot = new Robot(controller, voxels);
    // set task
    Settings settings = new Settings();
    settings.setStepFrequency(1d / 30d);
    Locomotion locomotion = new Locomotion(
        60,
        Locomotion.createTerrain("flat"),
        Lists.newArrayList(Locomotion.Metric.TRAVEL_X_VELOCITY),
        settings
    );
    // do task
    List<Double> results = locomotion.apply(robot);
    return results.get(0);
}
```

## References
1. Hiller, Lipson. "[Automatic design and manufacture of soft robots.](https://ieeexplore.ieee.org/abstract/document/6096440)" IEEE Transactions on Robotics 28.2 (2011): 457-466 
2. Medvet, Bartoli, De Lorenzo, Seriani. "[Design, Validation, and Case Studies of 2D-VSR-Sim, an Optimization-friendly Simulator of 2-D Voxel-based Soft Robots](https://arxiv.org/abs/2001.08617)" arXiv cs.RO: 2001.08617
