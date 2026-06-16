/*
 * STARK: Software Tool for the Analysis of Robustness in the unKnown environment
 *
 *                Copyright (C) 2023.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package schlogl;

import stark.*;
import stark.controller.Controller;
import stark.controller.NilController;
import stark.distance.*;
import stark.distance.AtomicDistanceExpression;
import stark.distance.DistanceExpression;
import stark.distance.MaxIntervalDistanceExpression;
import stark.ds.DataState;
import stark.ds.DataStateExpression;
import stark.ds.DataStateUpdate;
import stark.ds.RelationOperator;
import stark.perturbation.AtomicPerturbation;
import stark.perturbation.Perturbation;
import stark.perturbation.NonePerturbation;
import stark.ds.DataStateFunction;
import stark.robtl.*;
import stark.robtl.AtomicRobustnessFormula;
import stark.robtl.ImplicationRobustnessFormula;
import stark.robtl.RobustnessFormula;
import stark.robtl.ThreeValuedSemanticsVisitor;
import stark.robtl.TruthValues;
import org.apache.commons.math3.random.RandomGenerator;

import java.io.IOException;
import java.util.*;

public class Main {

    /*
     * The Schlogl model is a one-species stochastic reaction network with bistable behaviour.
     * X is the only dynamic species; N1 and N2 are buffered constants.
     *
     *   R1: N1 + 2X -> 3X    a1 = k1·N1·x(x-1)/2,    x >= 2
     *   R2: 3X -> N1 + 2X    a2 = k2·x(x-1)(x-2)/6,  x >= 3
     *   R3: N2 -> X          a3 = k3·N2
     *   R4: X -> N2          a4 = k4·x,                x >= 1
     *
     * Bistable attractors: X ~ 50 (low) and X ~ 600 (high), barrier near X = 250.
     * k1 and k3 live in the DataState so perturbations can scale them persistently.
     */

    // Stoichiometry for X (index 0); K1 and K3 are additional DataState slots, not stoichiometric.
    public static final int[] r1_input  = {2};
    public static final int[] r1_output = {3};
    public static final int[] r2_input  = {3};
    public static final int[] r2_output = {2};
    public static final int[] r3_input  = {0};
    public static final int[] r3_output = {1};
    public static final int[] r4_input  = {1};
    public static final int[] r4_output = {0};

    public static final int[][] r_input  = {r1_input,  r2_input,  r3_input,  r4_input};
    public static final int[][] r_output = {r1_output, r2_output, r3_output, r4_output};

    public static final int X  = 0;
    public static final int K1 = 1;
    public static final int K3 = 2;

    private static final int NUMBER_OF_VARIABLES = 3;
    private static final int NUMBER_OF_REACTIONS  = 4;

    public static final double N1 = 1.0e5;
    public static final double N2 = 2.0e5;

    public static final double K1_NOMINAL = 3.0e-7;
    public static final double K3_NOMINAL = 1.0e-3;

    public static final double k2 = 1.0e-4;
    public static final double k4 = 3.5;

    public static final double LOW_INIT_X  = 50.0;
    public static final double MID_INIT_X  = 250.0;
    public static final double HIGH_INIT_X = 600.0;

    public static final double LOW_THRESHOLD  = 150.0;
    public static final double HIGH_THRESHOLD = 400.0;

    public static final double FINAL_TIME  = 50.0;
    public static final double GRANULARITY = 0.01;

    public static final int  SAMPLES     = 500;
    public static final long RANDOM_SEED = 123456789L;

    private static final int OBSERVATION_STEPS =
            (int) Math.round(FINAL_TIME / GRANULARITY) + 1;   // 5001

    // Perturbation fires at the midpoint of the simulation horizon.
    public static final double PERTURBATION_START_TIME = FINAL_TIME / 2.0;
    private static final int PERTURBATION_START_STEP =
            (int) Math.round(PERTURBATION_START_TIME / GRANULARITY);   // 2500

    private record BasinInit(String label, double initialX, long seedOffset) {}

    private static final BasinInit[] BASIN_INITS = new BasinInit[]{
            new BasinInit("low",  LOW_INIT_X,  0L),
            new BasinInit("mid",  MID_INIT_X,  1L),
            new BasinInit("high", HIGH_INIT_X, 2L)
    };

    private enum ScaledParameter { K1, K3 }

    private record Scenario(String tag, ScaledParameter parameter, double[] alphas) {}

    // Scenario 1: amplify high attractor by scaling k1 up.
    // Scenario 2: amplify low attractor by scaling k3 down.
    private static final Scenario SCENARIO_1 =
            new Scenario("scenario1_k1", ScaledParameter.K1, new double[]{1.05, 1.10, 1.20});
    private static final Scenario SCENARIO_2 =
            new Scenario("scenario2_k3", ScaledParameter.K3, new double[]{0.95, 0.90, 0.80});


    public static void main(String[] args) throws IOException {
        try {
            ArrayList<DataStateExpression> F = new ArrayList<>();
            F.add(ds -> ds.get(X));
            F.add(ds -> ds.get(X) < LOW_THRESHOLD ? 1.0 : 0.0);
            F.add(ds -> (LOW_THRESHOLD <= ds.get(X) && ds.get(X) < HIGH_THRESHOLD) ? 1.0 : 0.0);
            F.add(ds -> ds.get(X) >= HIGH_THRESHOLD ? 1.0 : 0.0);

            ArrayList<String> L = new ArrayList<>();
            L.add("X       ");
            L.add("p_low   ");
            L.add("p_mid   ");
            L.add("p_high  ");

            int leftRBound  = 2300;
            int rightRBound = 3800;

            Controller controller = new NilController();

            // --- Nominal simulations ---
            System.out.println("\nNominal simulation of the Schlogl bistability model\n");

            for (BasinInit basin : BASIN_INITS) {
                DataState state = getInitialState(GRANULARITY, 0.0, 0.0, 0.0, basin.initialX());
                RandomGenerator rand = new DefaultRandomGenerator();
                rand.setSeed(RANDOM_SEED + basin.seedOffset());
                TimedSystem system = new TimedSystem(controller,
                        (rg, ds) -> ds.apply(selectAndApplyReaction(rg, ds)),
                        state,
                        ds -> selectReactionTime(rand, ds));

                System.out.println("  Basin: " + basin.label());
                double[][] data = SystemState.sample(rand, F, system, OBSERVATION_STEPS, SAMPLES);
                Util.writeToCSV("./schlogl_nominal_" + basin.label() + ".csv", data);

                double[][] finalX = sampleFinalX(rand, new NonePerturbation(), system, OBSERVATION_STEPS, SAMPLES);
                Util.writeToCSV("./schlogl_finalx_nominal_" + basin.label() + ".csv", finalX);

                if ("mid".equals(basin.label())) {
                    double[][] traces = sampleTrajectoriesX(rand, new NonePerturbation(),
                            system, OBSERVATION_STEPS, SAMPLES);
                    Util.writeToCSV("./schlogl_traces_nominal_mid.csv", traces);
                }

                System.out.println("\nNominal system - average values of X and basin occupancies:\n");
                printAvgData(rand, L, F, system, OBSERVATION_STEPS, SAMPLES, leftRBound, rightRBound);
            }


            // --- Perturbed simulations, behavioural distances, and model checking ---
            System.out.println("\nPerturbed simulation + distance + model checking\n");

            for (Scenario scenario : new Scenario[]{SCENARIO_1, SCENARIO_2}) {
                for (double alpha : scenario.alphas()) {
                    Perturbation pert = (scenario.parameter() == ScaledParameter.K1)
                            ? pertK1(alpha) : pertK3(alpha);

                    // Mid basin is processed first so that atomicX and eta_1 are available
                    // when the low and high basins construct their adaptability formulae.
                    AtomicDistanceExpression atomicX = null;
                    double eta_1 = 0.0;

                    for (BasinInit basin : new BasinInit[]{BASIN_INITS[1], BASIN_INITS[0], BASIN_INITS[2]}) {
                        String tag = "schlogl_" + scenario.tag()
                                + "_alpha" + formatAlpha(alpha)
                                + "_" + basin.label();
                        System.out.println("  " + tag);

                        DataState state = getInitialState(GRANULARITY, 0.0, 0.0, 0.0, basin.initialX());
                        RandomGenerator rand = new DefaultRandomGenerator();
                        rand.setSeed(RANDOM_SEED + basin.seedOffset());
                        TimedSystem system = new TimedSystem(controller,
                                (rg, ds) -> ds.apply(selectAndApplyReaction(rg, ds)),
                                state,
                                ds -> selectReactionTime(rand, ds));

                        // Phase 1: perturbed simulation
                        double[][] data = SystemState.sample(rand, F, pert, system, OBSERVATION_STEPS, SAMPLES);
                        Util.writeToCSV("./" + tag + ".csv", data);

                        System.out.println("\nPerturbed system - average values of X and basin occupancies:\n");
                        printAvgDataPerturbed(rand, L, F, system, OBSERVATION_STEPS, SAMPLES,
                                leftRBound, rightRBound, pert);

                        double[][] finalX = sampleFinalX(rand, pert, system, OBSERVATION_STEPS, SAMPLES);
                        Util.writeToCSV("./schlogl_finalx_" + scenario.tag()
                                + "_alpha" + formatAlpha(alpha)
                                + "_" + basin.label() + ".csv", finalX);

                        if ("mid".equals(basin.label())) {
                            double[][] traces = sampleTrajectoriesX(rand, pert, system, OBSERVATION_STEPS, SAMPLES);
                            Util.writeToCSV("./schlogl_traces_" + scenario.tag()
                                    + "_alpha" + formatAlpha(alpha)
                                    + "_mid.csv", traces);
                        }

                        if ("mid".equals(basin.label())) {

                            // Phase 2: behavioural distance (mid basin only)
                            // Normaliser = 110% of the observed maximum X under either dynamics.
                            double[] dataMax   = printMaxData(rand, L, F, system, OBSERVATION_STEPS,
                                    SAMPLES, leftRBound, rightRBound);
                            double[] dataMax_p = printMaxDataPerturbed(rand, L, F, system, OBSERVATION_STEPS,
                                    SAMPLES, leftRBound, rightRBound, pert);
                            double normX = Math.max(dataMax[0], dataMax_p[0]) * 1.1;

                            EvolutionSequence sequence   = new EvolutionSequence(rand, rg -> system, SAMPLES);
                            EvolutionSequence sequence_p = sequence.apply(pert, 0, 1);

                            atomicX = new AtomicDistanceExpression(
                                    ds -> ds.get(X) / normX,
                                    (v1, v2) -> Math.abs(v2 - v1));

                            double[][] perStep = new double[rightRBound - leftRBound][1];
                            for (int i = 0; i < rightRBound - leftRBound; i++) {
                                perStep[i][0] = atomicX.compute(i + leftRBound, sequence, sequence_p);
                            }
                            Util.writeToCSV("./schlogl_atomic_" + scenario.tag()
                                    + "_alpha" + formatAlpha(alpha)
                                    + "_mid.csv", perStep);

                            eta_1 = Arrays.stream(perStep).mapToDouble(row -> row[0]).max().orElse(0.0);

                            // Phase 3: robustness model checking (mid basin)
                            // phi_mid := sup_{t in [leftR, rightR]} d(X)(t) <= eta
                            DistanceExpression intdMax = new MaxIntervalDistanceExpression(
                                    atomicX, leftRBound, rightRBound);

                            double[][] robEvaluations = new double[20][2];
                            for (int i = 0; i < 20; i++) {
                                double threshold = (1 + i) / 100.0;
                                // AtomicRobustnessFormula: d_max([leftR, rightR]) <= eta
                                RobustnessFormula robustF = new AtomicRobustnessFormula(
                                        pert, intdMax, RelationOperator.LESS_OR_EQUAL_THAN, threshold);
                                TruthValues value = new ThreeValuedSemanticsVisitor(rand, 50, 1.96)
                                        .eval(robustF).eval(5, 0, sequence);
                                System.out.printf("%n robustF @ %.2f: %s%n", threshold, value);
                                robEvaluations[i][0] = threshold;
                                robEvaluations[i][1] = value.valueOf();
                            }
                            Util.writeToCSV("./schlogl_evalR_" + scenario.tag()
                                    + "_alpha" + formatAlpha(alpha)
                                    + "_mid.csv", robEvaluations);

                        } else {

                            // Adaptability evaluation (low and high basins).
                            // atomicX and eta_1 carry over from the mid basin processed above.
                            EvolutionSequence seqBasin = new EvolutionSequence(rand, rg -> system, SAMPLES);

                            // Recovery window: [2700, 3800] (500 steps after perturbation onset at 2500).
                            DistanceExpression intdMax     = new MaxIntervalDistanceExpression(
                                    atomicX, leftRBound, rightRBound);
                            DistanceExpression recoveryDist = new MaxIntervalDistanceExpression(
                                    atomicX, 2700, 3800);

                            RobustnessFormula lhs, rhs;

                            if ("low".equals(basin.label())) {
                                // rho^low(d) = 1[d(X) >= theta_l]
                                // Clear low-basin baseline: tests whether Scenario 1 overcomes the autocatalytic barrier.
                                lhs = new AtomicRobustnessFormula(
                                        pert, intdMax, RelationOperator.GREATER_OR_EQUAL_THAN, eta_1);
                                // Recovery condition: sup_{t in [2700,3800]} d(X)(t) <= eta_1
                                rhs = new AtomicRobustnessFormula(
                                        pert, recoveryDist, RelationOperator.LESS_OR_EQUAL_THAN, eta_1);
                            } else {
                                // rho^high(d) = 1[d(X) < theta_h]
                                // Clear high-basin baseline: tests whether Scenario 2 can displace the strongly self-reinforcing high attractor.
                                lhs = new AtomicRobustnessFormula(
                                        pert, intdMax, RelationOperator.LESS_THAN, eta_1);
                                // Recovery condition: sup_{t in [2700,3800]} d(X)(t) <= eta_1
                                rhs = new AtomicRobustnessFormula(
                                        pert, recoveryDist, RelationOperator.LESS_OR_EQUAL_THAN, eta_1);
                            }

                            // phi^basin: rho^basin ==> sup_{t in [2700,3800]} d(X)(t) <= eta_1
                            RobustnessFormula adaptFormula = new ImplicationRobustnessFormula(lhs, rhs);
                            TruthValues adaptResult = new ThreeValuedSemanticsVisitor(rand, 50, 1.96)
                                    .eval(adaptFormula).eval(5, 0, seqBasin);
                            System.out.printf("%n adaptability (%s): %s%n", basin.label(), adaptResult);

                            Util.writeToCSV("./schlogl_adapt_" + scenario.tag()
                                    + "_alpha" + formatAlpha(alpha)
                                    + "_" + basin.label() + ".csv",
                                    new double[][]{{eta_1, adaptResult.valueOf()}});
                        }
                    }
                }
            }

            System.out.println("Schlogl bistability analysis complete.");

        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }


    // Full per-sample X trajectories: result[step][sample].
    // Stream order is intentional — SampleSet.evalPenaltyFunction sorts its output and would
    // scramble per-sample identity across steps.
    private static double[][] sampleTrajectoriesX(RandomGenerator rg, Perturbation p,
            SystemState system, int steps, int size) {
        SampleSet<SystemState> current = SampleSet.generate(rg, r -> system, size);
        double[][] result = new double[steps][size];
        for (int i = 0; i < steps; i++) {
            Optional<DataStateFunction> effect = p.effect();
            if (effect.isPresent()) {
                current = current.apply(s -> s.apply(rg, effect.get()));
            }
            List<SystemState> members = current.stream().toList();
            for (int k = 0; k < members.size(); k++) {
                result[i][k] = members.get(k).getDataState().get(X);
            }
            current = current.apply(s -> s.sampleNext(rg));
            p = p.step();
        }
        return result;
    }

    // Final-step X of every individual sample: result[sample][0].
    private static double[][] sampleFinalX(RandomGenerator rg, Perturbation p,
            SystemState system, int steps, int size) {
        SampleSet<SystemState> current = SampleSet.generate(rg, r -> system, size);
        double[][] result = new double[size][1];
        for (int i = 0; i < steps; i++) {
            Optional<DataStateFunction> effect = p.effect();
            if (effect.isPresent()) {
                current = current.apply(s -> s.apply(rg, effect.get()));
            }
            if (i == steps - 1) {
                List<SystemState> members = current.stream().toList();
                for (int k = 0; k < members.size(); k++) {
                    result[k][0] = members.get(k).getDataState().get(X);
                }
            }
            current = current.apply(s -> s.sampleNext(rg));
            p = p.step();
        }
        return result;
    }

    private static double[] printAvgData(RandomGenerator rg, ArrayList<String> label,
            ArrayList<DataStateExpression> F, SystemState s, int steps, int size,
            int leftbound, int rightbound) {
        System.out.println(label);
        double[][] data_avg = SystemState.sample(rg, F, s, steps, size);
        double[] tot = new double[F.size()];
        Arrays.fill(tot, 0);
        for (int i = 0; i < data_avg.length; i++) {
            System.out.printf("%d>   ", i);
            for (int j = 0; j < data_avg[i].length - 1; j++) {
                System.out.printf("%f   ", data_avg[i][j]);
                if (leftbound <= i & i <= rightbound) {
                    tot[j] += data_avg[i][j];
                }
            }
            System.out.printf("%f\n", data_avg[i][data_avg[i].length - 1]);
            if (leftbound <= i & i <= rightbound) {
                tot[data_avg[i].length - 1] += data_avg[i][data_avg[i].length - 1];
            }
        }
        System.out.println();
        System.out.println("Avg over window of per-step means (X, basin occupancies):");
        for (int j = 0; j < tot.length - 1; j++) {
            System.out.printf("%f   ", tot[j] / (rightbound - leftbound));
        }
        System.out.printf("%f\n\n", tot[tot.length - 1] / (rightbound - leftbound));
        return tot;
    }

    private static double[] printAvgDataPerturbed(RandomGenerator rg, ArrayList<String> label,
            ArrayList<DataStateExpression> F, SystemState s, int steps, int size,
            int leftbound, int rightbound, Perturbation perturbation) {
        System.out.println(label);
        double[] tot = new double[F.size()];
        double[][] data_avg = SystemState.sample(rg, F, perturbation, s, steps, size);
        Arrays.fill(tot, 0);
        for (int i = 0; i < data_avg.length; i++) {
            System.out.printf("%d>   ", i);
            for (int j = 0; j < data_avg[i].length - 1; j++) {
                System.out.printf("%f   ", data_avg[i][j]);
                if (leftbound <= i & i <= rightbound) {
                    tot[j] += data_avg[i][j];
                }
            }
            System.out.printf("%f\n", data_avg[i][data_avg[i].length - 1]);
            if (leftbound <= i & i <= rightbound) {
                tot[data_avg[i].length - 1] += data_avg[i][data_avg[i].length - 1];
            }
        }
        System.out.println();
        System.out.println("Avg over window of per-step means (X, basin occupancies):");
        for (int j = 0; j < tot.length - 1; j++) {
            System.out.printf("%f   ", tot[j] / (rightbound - leftbound));
        }
        System.out.printf("%f\n\n", tot[tot.length - 1] / (rightbound - leftbound));
        return tot;
    }

    private static double[] printMaxData(RandomGenerator rg, ArrayList<String> label,
            ArrayList<DataStateExpression> F, SystemState s, int steps, int size,
            int leftbound, int rightbound) {
        double[][] data_max = SystemState.sample_max(rg, F, s, steps, size);
        double[] max = new double[F.size()];
        Arrays.fill(max, Double.NEGATIVE_INFINITY);
        for (int i = 0; i < data_max.length; i++) {
            for (int j = 0; j < data_max[i].length - 1; j++) {
                if (leftbound <= i & i <= rightbound) {
                    if (max[j] < data_max[i][j]) max[j] = data_max[i][j];
                }
            }
            if (leftbound <= i & i <= rightbound) {
                if (max[data_max[i].length - 1] < data_max[i][data_max[i].length - 1])
                    max[data_max[i].length - 1] = data_max[i][data_max[i].length - 1];
            }
        }
        System.out.println();
        System.out.println(label);
        for (int j = 0; j < max.length - 1; j++) System.out.printf("%f ", max[j]);
        System.out.printf("%f\n\n", max[max.length - 1]);
        return max;
    }

    private static double[] printMaxDataPerturbed(RandomGenerator rg, ArrayList<String> label,
            ArrayList<DataStateExpression> F, SystemState s, int steps, int size,
            int leftbound, int rightbound, Perturbation perturbation) {
        double[] max = new double[F.size()];
        double[][] data_max = SystemState.sample_max(rg, F, perturbation, s, steps, size);
        Arrays.fill(max, Double.NEGATIVE_INFINITY);
        for (int i = 0; i < data_max.length; i++) {
            for (int j = 0; j < data_max[i].length - 1; j++) {
                if (leftbound <= i & i <= rightbound) {
                    if (max[j] < data_max[i][j]) max[j] = data_max[i][j];
                }
            }
            if (leftbound <= i & i <= rightbound) {
                if (max[data_max[i].length - 1] < data_max[i][data_max[i].length - 1])
                    max[data_max[i].length - 1] = data_max[i][data_max[i].length - 1];
            }
        }
        System.out.println(label);
        for (int j = 0; j < max.length - 1; j++) System.out.printf("%f ", max[j]);
        System.out.printf("%f\n\n", max[max.length - 1]);
        return max;
    }

    private static List<DataStateUpdate> upd_k1(RandomGenerator rg, DataState state, double alpha) {
        return List.of(new DataStateUpdate(K1, Math.max(state.get(K1) * alpha, 0.0)));
    }

    private static List<DataStateUpdate> upd_k3(RandomGenerator rg, DataState state, double alpha) {
        return List.of(new DataStateUpdate(K3, Math.max(state.get(K3) * alpha, 0.0)));
    }

    public static Perturbation pertK1(double alpha) {
        return new AtomicPerturbation(PERTURBATION_START_STEP,
                (rg, ds) -> ds.apply(upd_k1(rg, ds, alpha)));
    }

    public static Perturbation pertK3(double alpha) {
        return new AtomicPerturbation(PERTURBATION_START_STEP,
                (rg, ds) -> ds.apply(upd_k3(rg, ds, alpha)));
    }

    public static double selectReactionTime(RandomGenerator rg, DataState state) {
        double rate = Arrays.stream(getReactionPropensities(state)).sum();
        return -Math.log(rg.nextDouble()) / rate;
    }

    public static List<DataStateUpdate> selectAndApplyReaction(RandomGenerator rg, DataState state) {
        List<DataStateUpdate> updates = new LinkedList<>();

        double[] lambda = getReactionPropensities(state);
        double[] lambdaParSum = new double[NUMBER_OF_REACTIONS];
        double lambdaSum = 0.0;

        for (int j = 0; j < NUMBER_OF_REACTIONS; j++) {
            lambdaSum += lambda[j];
            lambdaParSum[j] = lambdaSum;
        }

        if (lambdaSum > 0.0) {
            double token = 1 - rg.nextDouble();
            int selReaction = 0;
            while (selReaction < NUMBER_OF_REACTIONS - 1
                    && lambdaParSum[selReaction] < token * lambdaSum) {
                selReaction++;
            }
            selReaction++;

            switch (selReaction) {
                case 1 -> updates.add(new DataStateUpdate(X,
                        Math.max(state.get(X) + r1_output[0] - r1_input[0], 0.0)));
                case 2 -> updates.add(new DataStateUpdate(X,
                        Math.max(state.get(X) + r2_output[0] - r2_input[0], 0.0)));
                case 3 -> updates.add(new DataStateUpdate(X,
                        Math.max(state.get(X) + r3_output[0] - r3_input[0], 0.0)));
                case 4 -> updates.add(new DataStateUpdate(X,
                        Math.max(state.get(X) + r4_output[0] - r4_input[0], 0.0)));
                default -> throw new IllegalStateException("Unknown reaction index: " + selReaction);
            }
        } else {
            System.out.println("Missing reagents");
        }

        return updates;
    }

    private static double[] getReactionPropensities(DataState state) {
        double x  = Math.max(state.get(X),  0.0);
        // k1 and k3 come from DataState so that perturbations applied to those slots take effect immediately
        double k1 = Math.max(state.get(K1), 0.0);
        double k3 = Math.max(state.get(K3), 0.0);

        double a1 = (x >= 2.0) ? (k1 * N1 * x * (x - 1.0)) / 2.0 : 0.0;
        double a2 = (x >= 3.0) ? (k2 * x * (x - 1.0) * (x - 2.0)) / 6.0 : 0.0;
        double a3 = k3 * N2;
        double a4 = (x >= 1.0) ? k4 * x : 0.0;

        return new double[]{
                Math.max(a1, 0.0),
                Math.max(a2, 0.0),
                Math.max(a3, 0.0),
                Math.max(a4, 0.0)
        };
    }

    public static DataState getInitialState(double gran, double Tstep, double Treal,
                                            double Tdelta, double initialX) {
        Map<Integer, Double> values = new HashMap<>();
        values.put(X,  Math.max(initialX, 0.0));
        values.put(K1, K1_NOMINAL);
        values.put(K3, K3_NOMINAL);
        return new DataState(NUMBER_OF_VARIABLES,
                i -> values.getOrDefault(i, Double.NaN), gran, Tstep, Treal, Tdelta);
    }

    private static String formatAlpha(double alpha) {
        return String.format(Locale.ROOT, "%.2f", alpha).replace('.', 'p');
    }
}
