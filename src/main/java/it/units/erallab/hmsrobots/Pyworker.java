package it.units.erallab.hmsrobots;

import it.units.erallab.hmsrobots.core.controllers.*;
import it.units.erallab.hmsrobots.core.objects.ControllableVoxel;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.tasks.locomotion.Locomotion;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.RobotUtils;
import it.units.erallab.hmsrobots.util.SerializationUtils;
import it.units.erallab.hmsrobots.util.Utils;
import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.C;
import org.dyn4j.dynamics.Settings;

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
    Function<List<Double>, Robot<?>> builder;
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
        return SerializationUtils.serialize(builder.apply(DoubleStream.of(genome).boxed().collect(Collectors.toList())));
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

    private CentralizedSensing buildController(CentralizedSensing centralizedSensing, List<Double> weights, String controller) {
        String mlp = "MLP-(?<ratio>\\d+(\\.\\d+)?)-(?<nLayers>\\d+)(-(?<actFun>(sin|tanh|sigmoid|relu)))?";
        Map<String, String> params;
        if ((params = params(mlp, controller)) != null) {
            //(MultiLayerPerceptron.ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput)

            MultiLayerPerceptron func = new MultiLayerPerceptron(
                    params.containsKey("actFun") ? MultiLayerPerceptron.ActivationFunction.valueOf(params.get("actFun").toUpperCase()) : MultiLayerPerceptron.ActivationFunction.TANH,
                    centralizedSensing.nOfInputs(),
                    innerNeurons(centralizedSensing.nOfInputs(), centralizedSensing.nOfOutputs(), Integer.parseInt(params.get("nLayers")), Double.parseDouble(params.get("ratio"))),
                    centralizedSensing.nOfOutputs(),
                    weights.stream().mapToDouble(Double::doubleValue).toArray()
            );
            centralizedSensing.setFunction(func);
            return centralizedSensing;
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

    public static double[] validatationSerialized(String serialized) {
        String[] terrains = {"flat", "hilly-3-10-0", "hilly-3-10-1", "hilly-3-10-2", "hilly-3-10-3", "hilly-3-10-4", "steppy-3-10-0", "steppy-3-10-1", "steppy-3-10-2", "steppy-3-10-3", "steppy-3-10-4"
        };
        double[] vels = new double[terrains.length];
        int c = 0;
        for (String terrain : terrains) {
            Locomotion locomotion = new Locomotion(60, Locomotion.createTerrain(terrain), new Settings());
            Robot robot = SerializationUtils.deserialize(serialized, Robot.class, SerializationUtils.Mode.GZIPPED_JSON);
            //System.out.println("pre apply");
            Outcome outcome = locomotion.apply(robot, null);
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
            results = map(this.genotypes, this.builder, this.fitness, executorService);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }
}
