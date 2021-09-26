package it.units.erallab.hmsrobots;

import it.units.erallab.hmsrobots.core.controllers.*;
import it.units.erallab.hmsrobots.core.geometry.BoundingBox;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.core.sensors.Lidar;
import it.units.erallab.hmsrobots.core.sensors.Normalization;
import it.units.erallab.hmsrobots.core.sensors.Sensor;
import it.units.erallab.hmsrobots.core.snapshots.MLPState;
import it.units.erallab.hmsrobots.tasks.locomotion.Locomotion;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.RobotUtils;
import it.units.erallab.hmsrobots.util.SerializationUtils;
import it.units.erallab.hmsrobots.viewers.GridFileWriter;
import it.units.erallab.hmsrobots.viewers.VideoUtils;
import it.units.erallab.hmsrobots.viewers.drawers.Drawer;
import it.units.erallab.hmsrobots.viewers.drawers.Drawers;
import it.units.erallab.hmsrobots.viewers.drawers.MLPDrawer;
import it.units.erallab.hmsrobots.viewers.drawers.SubtreeDrawer;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.units.qual.A;
import org.dyn4j.dynamics.Settings;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import static it.units.erallab.hmsrobots.util.Utils.params;
import static java.util.Arrays.stream;

public class Pyworker implements Runnable {
    double[][] terrain;
    double duration;
    Function<double[], Robot<?>> builder;
    Function<List<Double>, Robot<?>> builder1;
    Function<Robot<?>, List<Double>> fitness;
    List<List<Double>> genotypes = new ArrayList<>();
    List<List<Double>> results = new ArrayList<>();
    protected final ExecutorService executorService;

    public Pyworker(String terrain, String shape, String sensors, String controller, double duration) {
        Grid<? extends SensingVoxel> body = RobotUtils.buildSensorizingFunction(sensors).apply(RobotUtils.buildShape(shape));
        CentralizedSensing centralizedSensing = new CentralizedSensing(body);
        System.out.println("process " + Runtime.getRuntime().availableProcessors());
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.duration = duration;
        this.terrain = Locomotion.createTerrain(terrain);
        this.builder = i -> new Robot<>(
                SerializationUtils.clone(buildController(centralizedSensing, i, controller)),
                SerializationUtils.clone(body));
        this.builder1 = i -> new Robot<>(
                SerializationUtils.clone(buildController(centralizedSensing, i.stream().mapToDouble(Double::doubleValue).toArray(), controller)),
                SerializationUtils.clone(body));
        this.fitness = robot -> {
            Locomotion locomotion = new Locomotion(duration, this.terrain, new Settings());
            Outcome outcome = locomotion.apply(robot, null);
            double vel = outcome.getVelocity();
            Outcome.Gait gait = outcome.getMainGait();
            double[] desc;
            if (gait == null) {
                desc = new double[4];
            }else {
                desc = new double[]{
                        gait.getAvgTouchArea(),
                        gait.getCoverage(),
                        gait.getPurity(),
                        gait.getModeInterval()
                };
            }

            ArrayList<Double> fitness = new ArrayList<>();
            fitness.add(vel);
            for (int i = 0; i < desc.length; i++) {
                fitness.add(desc[i]);
            }
            return fitness;
        };


    }

    /*public Outcome locomote() {
        Locomotion locomotion = new Locomotion(duration, terrain, new Settings());
        //System.out.println("pre apply");
        Outcome outcome = locomotion.apply(robot, null);
        //System.out.println("post apply");
        return outcome;
    }
*/
    public String getRobotSerialized(double[] genome) {
        return SerializationUtils.serialize(builder.apply(genome));
    }

    public double[] getHebbCoeff(String serialized){
        Robot robot = SerializationUtils.deserialize(serialized, Robot.class, SerializationUtils.Mode.GZIPPED_JSON);

        double[] hc;
        if (((CentralizedSensing) robot.getController()).getFunction() instanceof HebbianPerceptronOutputModel){
            hc = ((HebbianPerceptronOutputModel)((CentralizedSensing) robot.getController()).getFunction()).getParams();
        }else{
            hc = ((HebbianPerceptronFullModel)((CentralizedSensing) robot.getController()).getFunction()).getParams();
        }
        return hc;
    }



    private int[] innerNeurons(int nOfInputs, int nOfOutputs, int nOfInnerLayers, double innerLayerRatio) {
        int[] innerNeurons = new int[nOfInnerLayers];
        int centerSize = (int) Math.max(2, Math.round(nOfInputs * innerLayerRatio));
        if (nOfInnerLayers > 1) {
            for (int i = 0; i < nOfInnerLayers / 2; i++) {
                innerNeurons[i] = nOfInputs + (centerSize - nOfInputs) / (nOfInnerLayers / 2 + 1) * (i + 1);
            }
            for (int i = nOfInnerLayers / 2; i < nOfInnerLayers; i++) {
                innerNeurons[i] = centerSize + (nOfOutputs - centerSize) / (nOfInnerLayers / 2 + 1) * (i - nOfInnerLayers / 2);
            }
        } else if (nOfInnerLayers > 0) {
            innerNeurons[0] = centerSize;
        }
        return innerNeurons;
    }

    private CentralizedSensing buildController(CentralizedSensing centralizedSensing, double[] weights, String controller) {
        String mlp = "MLP-(?<ratio>\\d+(\\.\\d+)?)-(?<nLayers>\\d+)(-(?<actFun>(sin|tanh|sigmoid|relu)))?";
        String hlp = "HLP-(?<type>(full|output))-(?<eta>\\d+(\\.\\d+)?)(-(?<actFun>(tanh|sigmoid|relu)))-(?<ratio>\\d+(\\.\\d+)?)-(?<nLayers>\\d+)-(?<seed>-?\\d+)(-(?<min>-?\\d+(\\.\\d+)?))?(-(?<max>\\d+(\\.\\d+)?))?";

        Map<String, String> params;
        if ((params = params(mlp, controller)) != null) {
            //(MultiLayerPerceptron.ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput)

            MultiLayerPerceptron func = new MultiLayerPerceptron(
                    params.containsKey("actFun") ? MultiLayerPerceptron.ActivationFunction.valueOf(params.get("actFun").toUpperCase()) : MultiLayerPerceptron.ActivationFunction.TANH,
                    centralizedSensing.nOfInputs(),
                    innerNeurons(centralizedSensing.nOfInputs(), centralizedSensing.nOfOutputs(), Integer.parseInt(params.get("nLayers")), Double.parseDouble(params.get("ratio"))),
                    centralizedSensing.nOfOutputs(),
                    weights
            );

            centralizedSensing.setFunction(func);
            return centralizedSensing;
        }
        if ((params = params(hlp, controller)) != null) {

            if (params.get("type").equals("full")) {

                HebbianPerceptronFullModel full = new HebbianPerceptronFullModel(
                        params.containsKey("actFun") ? HebbianPerceptronFullModel.ActivationFunction.valueOf(params.get("actFun").toUpperCase()) : HebbianPerceptronFullModel.ActivationFunction.TANH,
                        centralizedSensing.nOfInputs(),
                        innerNeurons(centralizedSensing.nOfInputs(), centralizedSensing.nOfOutputs(), Integer.parseInt(params.get("nLayers")), Double.parseDouble(params.get("ratio"))),
                        centralizedSensing.nOfOutputs(),
                        Double.parseDouble(params.get("eta")),
                        new HashSet<>(),
                        new HashMap<>(),
                        params.containsKey("min") && params.containsKey("max") ? new double[]{Double.parseDouble(params.get("min").replace("n","-")),Double.parseDouble(params.get("max")) } : null
                        );
                //System.out.println(Arrays.toString(weights));

                full.setInitWeights(weights);
                full.setWeights(weights);
                centralizedSensing.setFunction(full);
                return centralizedSensing;

            } else {
                HebbianPerceptronOutputModel incoming = new HebbianPerceptronOutputModel(
                        params.containsKey("actFun") ? HebbianPerceptronOutputModel.ActivationFunction.valueOf(params.get("actFun").toUpperCase()) : HebbianPerceptronOutputModel.ActivationFunction.TANH,
                        centralizedSensing.nOfInputs(),
                        innerNeurons(centralizedSensing.nOfInputs(), centralizedSensing.nOfOutputs(), Integer.parseInt(params.get("nLayers")), Double.parseDouble(params.get("ratio"))),
                        centralizedSensing.nOfOutputs(),
                        Double.parseDouble(params.get("eta")),
                        new HashSet<>(),
                        new HashMap<>(),
                        params.containsKey("min") && params.containsKey("max") ? new double[]{Double.parseDouble(params.get("min").replace("n","-")),Double.parseDouble(params.get("max")) } : null
                );
                incoming.setWeights(weights);
                centralizedSensing.setFunction(incoming);
                return centralizedSensing;

            }

        }
        throw new IllegalArgumentException(String.format("Unknown mapper name: %s", controller));

    }

    public double[][] locomote(double[][] genos) {
        genotypes = new ArrayList<>();
        for (int i = 0; i < genos.length; i++) {
            genotypes.add(DoubleStream.of(genos[i]).boxed().collect(Collectors.toList()));
        }

        run();

        double[][] res = new double[results.size()][5];

        for (int i = 0; i < results.size(); i++) {
            res[i] = results.get(i).stream().mapToDouble(Double::doubleValue).toArray();
        }

        return res;
    }

    public Outcome locomoteSerialized(String serialized) {
        Locomotion locomotion = new Locomotion(duration, terrain, new Settings());
        System.out.println("here");
        Robot robot = SerializationUtils.deserialize(serialized, Robot.class, SerializationUtils.Mode.GZIPPED_JSON);
        robot = new Robot(robot.getController(),
                RobotUtils.buildSensorizingFunction("high_biped-0.01-f").apply(RobotUtils.buildShape("biped-4x3")));
        System.out.println("pre apply");
        Outcome outcome = locomotion.apply(robot, null);
        System.out.println("post apply");
        return outcome;

    }

    public double[][][] locomoteSerializedParallel(String[] serialized) throws ExecutionException, InterruptedException {
        List<double[][]> results = mapI(Arrays.asList(serialized.clone()), "identity", terrain, executorService);
        return results.stream().toArray(double[][][]::new);

    }

    public double[][][] locomoteSerializedParallelBreakable(String[] serialized, String transformation) throws ExecutionException, InterruptedException {
        List<double[][]> results = mapI(Arrays.asList(serialized.clone()), transformation, terrain, executorService);
        return results.stream().toArray(double[][][]::new);

    }
    public String makeSer(String serialized, double[] weights) {
        Robot robot = SerializationUtils.deserialize(serialized, Robot.class, SerializationUtils.Mode.GZIPPED_JSON);

        robot = new Robot(robot.getController(),
                RobotUtils.buildSensorizingFunction("high_biped-0.01-f").apply(RobotUtils.buildShape("biped-4x3")));

        if (weights.length > 0) {

            ((HebbianPerceptronFullModel)((CentralizedSensing)robot.getController()).getFunction()).setInitWeights(weights);
            ((HebbianPerceptronFullModel) ((CentralizedSensing) robot.getController()).getFunction()).setWeights(weights);
        }
        return SerializationUtils.serialize(robot, SerializationUtils.Mode.GZIPPED_JSON);
    }

    public void makeVideoT(String serialized, boolean convertLidar, String terrainName, String filename, double[] weights){
        Robot robot = SerializationUtils.deserialize(serialized, Robot.class, SerializationUtils.Mode.GZIPPED_JSON);

        Locomotion locomotion = new Locomotion(
                60,
                Locomotion.createTerrain(terrainName),
                new Settings()
        );
        System.out.println("ehi");
        Grid<Pair<String, Robot<?>>> namedSolutionGrid = Grid.create(1, 1);
        //namedSolutionGrid.set(0, 0, Pair.of("dist-hetero", distHetero));
        namedSolutionGrid.set(0, 0, Pair.of("centralized", robot));
        Function<String, Drawer> drawerSupplier = s -> Drawer.of(
                Drawer.clip(
                        BoundingBox.build(0d, 0d, 1d, 0.5d),
                        Drawers.basicWithMiniWorld(s)
                ),
                Drawer.clip(
                        BoundingBox.build(0d, 0.5d, 1d, 1d),
                        Drawer.of(
                                Drawer.clear(),
                                new MLPDrawer(SubtreeDrawer.Extractor.matches(MLPState.class, null, null), 15d, EnumSet.allOf(MLPDrawer.Part.class))
                        )
                )
        );
        //GridOnlineViewer.run(locomotion, namedSolutionGrid, drawerSupplier);
        //GridOnlineViewer.run(locomotion, Grid.create(1, 1, Pair.of("", centralized)), drawerSupplier);
        System.out.println("ohi");
        try {
            GridFileWriter.save(
                    locomotion,
                    Grid.create(1, 1, Pair.of("", robot)),
                    400, 400, 1, 24,
                    VideoUtils.EncoderFacility.JCODEC,
                    new File(filename),
                    drawerSupplier
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void makeVideo(String serialized, boolean convertLidar, String terrainName, String filename, double[] weights){
        Robot robot = SerializationUtils.deserialize(serialized, Robot.class, SerializationUtils.Mode.GZIPPED_JSON);
        System.out.println("333"+Arrays.toString(((HebbianPerceptronFullModel)((CentralizedSensing)robot.getController()).getFunction()).getWeights()));
        if (convertLidar){
            robot = new Robot(robot.getController(),
                    RobotUtils.buildSensorizingFunction("high_biped-0.01-f").apply(RobotUtils.buildShape("biped-4x3")));
        }
        if (weights.length>0){
            ((HebbianPerceptronFullModel)((CentralizedSensing)robot.getController()).getFunction()).setInitWeights(weights);
            ((HebbianPerceptronFullModel)((CentralizedSensing)robot.getController()).getFunction()).setWeights(weights);
            ((HebbianPerceptronFullModel)((CentralizedSensing)robot.getController()).getFunction()).invert();
        }
        //((HebbianPerceptronFullModel)((CentralizedSensing)robot.getController()).getFunction()).invert();

        Locomotion locomotion = new Locomotion(
                60,
                Locomotion.createTerrain(terrainName),
                new Settings()
        );
        System.out.println("ehi");
        Grid<Pair<String, Robot<?>>> namedSolutionGrid = Grid.create(1, 1);
        //namedSolutionGrid.set(0, 0, Pair.of("dist-hetero", distHetero));
        namedSolutionGrid.set(0, 0, Pair.of("centralized", robot));
        Function<String, Drawer> drawerSupplier = s -> Drawer.of(
                Drawer.clip(
                        BoundingBox.build(0d, 0d, 1d, 0.5d),
                        Drawers.basicWithMiniWorld(s)
                ),
                Drawer.clip(
                        BoundingBox.build(0d, 0.5d, 1d, 1d),
                        Drawer.of(
                                Drawer.clear(),
                                new MLPDrawer(SubtreeDrawer.Extractor.matches(MLPState.class, null, null), 15d, EnumSet.allOf(MLPDrawer.Part.class))
                        )
                )
        );
        //GridOnlineViewer.run(locomotion, namedSolutionGrid, drawerSupplier);
        //GridOnlineViewer.run(locomotion, Grid.create(1, 1, Pair.of("", centralized)), drawerSupplier);
        System.out.println("ohi");
        try {
            GridFileWriter.save(
                    locomotion,
                    Grid.create(1, 1, Pair.of("", robot)),
                    400, 400, 1, 24,
                    VideoUtils.EncoderFacility.JCODEC,
                    new File(filename),
                    drawerSupplier
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static double[] validatationSerialized(String serialized) {
        String[] terrains = {"hilly-3-30-0", "hilly-3-30-1", "hilly-3-30-2", "hilly-3-30-3", "hilly-3-30-4"};
        double[] vels = new double[terrains.length];
        int c = 0;
        Robot robot = SerializationUtils.deserialize(serialized, Robot.class, SerializationUtils.Mode.GZIPPED_JSON);
        for (String terrain : terrains) {
            Locomotion locomotion = new Locomotion(60, Locomotion.createTerrain(terrain), new Settings());
            robot = SerializationUtils.deserialize(serialized, Robot.class, SerializationUtils.Mode.GZIPPED_JSON);
            robot = new Robot(robot.getController(),
                    RobotUtils.buildSensorizingFunction("high_biped-0.01-f").apply(RobotUtils.buildShape("biped-4x3")));
            System.out.println(Arrays.toString(((HebbianPerceptronFullModel)((CentralizedSensing)robot.getController()).getFunction()).getWeights()));
            ((HebbianPerceptronFullModel)((CentralizedSensing)robot.getController()).getFunction()).invert();
            //System.out.println(Arrays.toString(((HebbianPerceptronFullModel)((CentralizedSensing)robot.getController()).getFunction()).getParams()));
            //System.out.println("pre apply");
            Outcome outcome = locomotion.apply(robot, null);
            System.out.println("----------");
            vels[c] = outcome.getVelocity();
            c++;
        }
        return vels;
    }

    public static double[] validatationSerializedTimed(String serialized, double t) {
        String[] terrains = {"hilly-3-30-0", "hilly-3-30-1", "hilly-3-30-2", "hilly-3-30-3", "hilly-3-30-4"};
        double[] vels = new double[terrains.length];
        int c = 0;
        Robot robot = SerializationUtils.deserialize(serialized, Robot.class, SerializationUtils.Mode.GZIPPED_JSON);
        for (String terrain : terrains) {
            Locomotion locomotion = new Locomotion(60, Locomotion.createTerrain(terrain), new Settings(), t);
            robot = SerializationUtils.deserialize(serialized, Robot.class, SerializationUtils.Mode.GZIPPED_JSON);
            robot = new Robot(robot.getController(),
                    RobotUtils.buildSensorizingFunction("high_biped-0.01-f").apply(RobotUtils.buildShape("biped-4x3")));
            //System.out.println(Arrays.toString(((HebbianPerceptronFullModel)((CentralizedSensing)robot.getController()).getFunction()).getWeights()));

            //System.out.println(Arrays.toString(((HebbianPerceptronFullModel)((CentralizedSensing)robot.getController()).getFunction()).getParams()));
            //System.out.println("pre apply");
            Outcome outcome = locomotion.apply(robot, null);
            System.out.println(outcome);
            System.out.println("----------  ");
            vels[c] = outcome.getVelocity();
            c++;
        }
        return vels;
    }

    public double[] validatationSerializedBreakable(String[] serialized, String transformation) throws ExecutionException, InterruptedException {
        List<Double> results = mapII(Arrays.asList(serialized.clone()), transformation, terrain, executorService);
        return results.stream().mapToDouble(d -> d).toArray();
    }

    protected static List<List<Double>> map(Collection<List<Double>> genotypes,
                                            Function<List<Double>, Robot<?>> solutionMapper,
                                            Function<Robot<?>, List<Double>> fitnessFunction,
                                            ExecutorService executor) throws InterruptedException, ExecutionException {
        List<Callable<List<Double>>> callables = new ArrayList<>(genotypes.size());
        callables.addAll(genotypes.stream()
                .map(genotype -> (Callable<List<Double>>) () -> {
                    return fitnessFunction.apply(solutionMapper.apply(genotype));
                }).collect(Collectors.toList()));
        return getAll(executor.invokeAll(callables));
    }

    protected static List<double[][]> mapI(List<String> ser, String transformation, double[][] terrain, ExecutorService executor) throws InterruptedException, ExecutionException {
        List<Callable<double[][]>> callables = new ArrayList<>(ser.size());
        callables.addAll(ser.stream()
                .map(ind -> (Callable<double[][]>) () -> {
                    Locomotion locomotion = new Locomotion(60, terrain, new Settings());
                    double[][] data = locomotion.apply(
                            RobotUtils.buildRobotTransformation(transformation, new Random(0)).apply(
                                    SerializationUtils.deserialize(ind, Robot.class, SerializationUtils.Mode.GZIPPED_JSON)
                            )
                    ).getDataObservation();
                    return data;
                }).collect(Collectors.toList()));
        return getAll(executor.invokeAll(callables));
    }

    protected static List<Double> mapII(List<String> ser, String transformation, double[][] terrain, ExecutorService executor) throws InterruptedException, ExecutionException {
        List<Callable<Double>> callables = new ArrayList<>(ser.size());
        callables.addAll(ser.stream()
                .map(ind -> (Callable<Double>) () -> {
                    Locomotion locomotion = new Locomotion(60, terrain, new Settings());
                    double data = locomotion.apply(
                            RobotUtils.buildRobotTransformation(transformation, new Random(0)).apply(
                                    SerializationUtils.deserialize(ind, Robot.class, SerializationUtils.Mode.GZIPPED_JSON)
                            )
                    ).getVelocity();
                    return data;
                }).collect(Collectors.toList()));
        return getAll(executor.invokeAll(callables));
    }


    private static <T> List<T> getAll(List<Future<T>> futures) throws InterruptedException, ExecutionException {
        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            results.add(future.get());
        }
        return results;
    }

    @Override
    public void run() {
        try {
            results = map(this.genotypes, this.builder1, this.fitness, executorService);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }
}
