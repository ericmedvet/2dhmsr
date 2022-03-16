# 2D-VSR-Sim

2D-VSR-Sim is a Java framework for experimenting with a 2-D version of the *voxel-based soft robots* (VSRs) [1].

If you use this software, please cite one or both of the following papers:

- Medvet, Bartoli, De Lorenzo, Seriani; [2D-VSR-Sim: a Simulation Tool for the Optimization of 2-D Voxel-based Soft Robots](https://doi.org/10.1016/j.softx.2020.100573); SoftwareX, 12:100573, 2020

```bibtex
@article{medvet20202d,
    title = {{2D-VSR-Sim: A simulation tool for the optimization of 2-D voxel-based soft robots}},
    author={Medvet, Eric and Bartoli, Alberto and De Lorenzo, Andrea and Seriani, Stefano},
    journal = {{SoftwareX}},
    volume = {12},
    year = {2020},
    doi = {https://doi.org/10.1016/j.softx.2020.100573},
}
```

- Medvet, Bartoli, De Lorenzo, Seriani; [Design, Validation, and Case Studies of 2D-VSR-Sim, an Optimization-friendly Simulator of 2-D Voxel-based Soft Robots](https://arxiv.org/abs/2001.08617); arXiv cs.RO: 2001.08617, 2020

```bibtex
@article{medvet2020design,
    title = {{Design, Validation, and Case Studies of 2D-VSR-Sim, an Optimization-friendly Simulator of 2-D Voxel-based Soft Robots}},
    author = {Medvet, Eric and Bartoli, Alberto and De Lorenzo, Andrea and Seriani, Stefano},
    journal = {{arXiv preprint arXiv:2001.08617}},
    year = {2020}
}
```

VSRs are composed of many simple soft blocks (called *voxels*) that can change their volumes: the way voxels are assembled defines the *body* of the VSR, whereas the law according to which voxels change their volume over the time defines the *brain* of the VSR. Design of VSRs body and brain can be automatized by means of optimization techniques.

**2D-VSR-Sim is an optimization-friendly VSR simulator** that focuses on two key steps of optimization: what to optimize and towards which goal. It offers a consistent interface to the different components (e.g., body, brain, sensors, specific mechanisms for control signal propagation) of a VSR which are suitable for optimization and to the task the VSR is requested to perform (e.g., locomotion, grasping of moving objects). 2D-VSR-Sim **is not** a software for doing the actual optimization. As a consequence, it leaves users (i.e., researchers and practitioners) great freedom on how to optimize: different techniques, e.g., evolutionary computation or reinforcement learning, can be used.

## VSR model in brief

All the details of the model can be found in [2,3]. In brief, a voxel is a soft 2-D block, i.e., a deformable square modeled with four rigid bodies (square masses), a number of spring-damper systems that constitute a scaffolding, and a number of ropes. A VSR is modeled as a collection of voxels organized in a 2-D grid, each voxel in the grid being rigidly connected with the voxel above, below, on the left, and on the right. The way a VSR behaves is determined by a *controller* that may exploit the readings of a number of *sensors* that each voxel may be equipped with. Most of the properties of the VSR model are **configurable by the user**. 2D-VSR-Sim exploits an existing physics engine, [dyn4j](http://www.dyn4j.org/), for solving the mechanical model defined by a VSR subjected to the forces caused by the actuation determined by its controller and by the interaction with other bodies (typically, the ground).

A graphical representation of a moving VSR:
![A graphical representation of a moving VSR](/assets/frames.png)

## Using the sofware

2D-VSR-Sim is meant to be used within or together with another software that performs the actual optimization. This software is organized as a Java package containing the classes and the interfaces that represent the VSR model and related concepts. The voxel is modeled by the `Voxel` class. A `Voxel` can be equipped with zero or more sensors, modeled by the `Sensor` class. The VSR is modeled by the `Robot` class. A controller is modeled by the interface `Controller`. A task, i.e., some activity whose degree of accomplishment can be evaluated quantitatively according to one or more indexes, is described by the interface `Task`.

2D-VSR-Sim provides a mechanism for keeping track of an ongoing simulation based on the observer pattern. A `SnapshotListener` interface represents the observer that is notified of progresses in the simulation, each in the form of a `Snapshot`: the latter is an immutable representation of the state of all the objects (e.g., positions of voxels, values of their sensor readings) in the simulation at a given time. There are two listeners implementing (indirectly) this interface:

- `GridOnlineViewer` renders a visualization of the simulated world within a GUI;
- `GridFileWriter` produces a video file.

Both can process multiple simulations together, organized in a grid. The possibility of visualizing many simulations together can be useful, for example, for comparing different stages of an optimization.

### Sample code

A brief fragment of code using for setting up a VSR, testing it in the task of locomotion and saving an image with a few frames of the resulting behavior. This VSR is controlled with a periodic sinusoidal signal whose phase changes along the x-direction of the robot.

```java
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
```

#### Optimization examples: optimize phases

This piece of code shows a method for assessing a VSR whose voxels are actuated with a sinusoidal signal with different phases given the vector of phases. This method might be the one being called by an external optimization software. In this example, the VSR is a 10x4 worm that is assessed on locomotion on a flat terrain: the single objective is the traveled distance.

```java
public class Example {
  public static double assessOnLocomotion(double[] phases) {
    // set robot shape and sensors
    Grid<Boolean> shape = RobotUtils.buildShape("worm-10x4");
    Grid<Voxel> body = RobotUtils.buildSensorizingFunction("uniform-ax+t-0").apply(shape);
    // set controller
    double f = 1d;
    Controller controller = new TimeFunctions(Grid.create(
        body.getW(),
        body.getH(),
        (x, y) -> (t) -> Math.sin(-2 * Math.PI * f * t + Math.PI * phases[(x + (int) Math.floor(y / body.getH()))])
    ));
    Robot robot = new Robot(controller, body);
    // set task
    Locomotion locomotion = new Locomotion(
        60,
        Locomotion.createTerrain("flat"),
        new Settings()
    );
    // do task
    Outcome outcome = locomotion.apply(robot);
    return outcome.getVelocity();
  }
}
``` 

#### Optimization examples: optimize neural network weights

This example is similar to the one above, but here the controller of the robot is a *neural network* whose wights are subjected to optimization. Here the robot is a 7x4 biped with a 7x2 trunk and two 2x2 legs. The voxels have different sensors depending on their position:

- "feet" have touch sensors (whose signal is averaged in a 1 second time window)
- "spine", i.e., the top-row of trunk have velocity (along the 2-axes) and average velocity sensors
- the remaining voxels have an area ratio sensor

```java
public class Example {
  public static double assessOnLocomotion(double[] weights) {
    // set robot shape and sensors
    Grid<Boolean> shape = RobotUtils.buildShape("biped-7x4");
    Grid<Voxel> body = RobotUtils.buildSensorizingFunction("spinedTouch-t-f-0").apply(shape);
    // set controller
    CentralizedSensing controller = new CentralizedSensing(body);
    MultiLayerPerceptron mlp = new MultiLayerPerceptron(
        MultiLayerPerceptron.ActivationFunction.TANH,
        centralizedSensing.nOfInputs(),
        new int[0],
        centralizedSensing.nOfOutputs()
    );
    mlp.setParams(weights);
    centralizedSensing.setFunction(mlp);
    // build robot
    Robot robot = new Robot(controller, body);
    // set task
    Locomotion locomotion = new Locomotion(
        60,
        Locomotion.createTerrain("flat"),
        new Settings()
    );
    // do task
    Outcome outcome = locomotion.apply(robot);
    return outcome.getVelocity();
  }
}
```

## References

1. Hiller, Lipson; [Automatic design and manufacture of soft robots.](https://ieeexplore.ieee.org/abstract/document/6096440); IEEE Transactions on Robotics 28.2 (2011): 457-466
2. Medvet, Bartoli, De Lorenzo, Seriani; [Design, Validation, and Case Studies of 2D-VSR-Sim, an Optimization-friendly Simulator of 2-D Voxel-based Soft Robots](https://arxiv.org/abs/2001.08617); arXiv cs.RO: 2001.08617
3. Medvet, Bartoli, De Lorenzo, Seriani; [2D-VSR-Sim: a Simulation Tool for the Optimization of 2-D Voxel-based Soft Robots](https://medvet.inginf.units.it/publications/2020-j-mbds-vsr/); SoftwareX; 2020

## Other research papers based on 2D-VSR-Sim

- Nadizar, Medvet, Miras; [On the Schedule for Morphological Development of Evolved Modular Soft Robots](https://medvet.inginf.units.it/publications/2022-c-nmm-schedule/); 25th European Conference on Genetic Programming (EuroGP); 2022
- Ferigo, Medvet, Iacca; [Optimizing the Sensory Apparatus of Voxel-based Soft Robots through Evolution and Babbling](https://medvet.inginf.units.it/publications/2021-j-fmi-optimizing/); Springer Nature Computer Science; 2022
- Nadizar, Medvet, Nichele, Huse Ramstad, Pellegrino, Zullich; [Merging Pruning and Neuroevolution: towards Robust and Efficient Controllers for Modular Soft Robots](https://medvet.inginf.units.it/publications/2021-j-nmnhpz-merging/); Knowledge Engineering Review (KER); 2021
- Ferigo, Iacca, Medvet, Pigozzi; [Evolving Hebbian Learning Rules in Voxel-based Soft Robots](https://medvet.inginf.units.it/publications/2021-p-fimp-evolving/); IEEE TechRxiv; 2021
- Nadizar, Medvet, Pellegrino, Zullich, Nichele; [On the Effects of Pruning on Evolved Neural Controllers for Soft Robots](https://medvet.inginf.units.it/publications/2021-c-nmpzn-effects/); Workshop on Neuroevolution at Work (NEWK@GECCO); 2021
- Talamini, Medvet, Nichele; [Criticality-driven Evolution of Adaptable Morphologies of Voxel-Based Soft-Robots](https://medvet.inginf.units.it/publications/2021-j-tmn-criticality/); Frontiers in Robotics and AI; 2021
- Medvet, Bartoli, Pigozzi, Rochelli; [Biodiversity in Evolved Voxel-based Soft Robots](https://medvet.inginf.units.it/publications/2021-c-mbpr-biodiversity/); ACM Genetic and Evolutionary Computation Conference (GECCO); 2021
- Medvet, Bartoli; [Evolutionary Optimization of Graphs with GraphEA](https://medvet.inginf.units.it/publications/2020-c-mb-evolutionary/); 19th International Conference of the Italian Association for Artificial Intelligence (AIxIA); 2020
- Ferigo, Iacca, Medvet; [Beyond Body Shape and Brain: Evolving the Sensory Apparatus of Voxel-based Soft Robots](https://medvet.inginf.units.it/publications/2021-c-fim-beyond/); 24th European Conference on the Applications of Evolutionary Computation (EvoAPPS); 2021
- Medvet, Bartoli; [GraphEA: a Versatile Representation and Evolutionary Algorithm for Graphs](https://medvet.inginf.units.it/publications/2020-c-mb-graphea/); Workshop on Evolutionary and Population-based Optimization (WEPO@AIxIA); 2020
- Medvet, Bartoli, De Lorenzo, Fidel; [Evolution of Distributed Neural Controllers for Voxel-based Soft Robots](https://medvet.inginf.units.it/publications/2020-c-mbdf-evolution/); ACM Genetic and Evolutionary Computation Conference (GECCO); 2020
