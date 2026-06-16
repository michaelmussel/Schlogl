package reacsys;

import stark.*;
import stark.controller.*;
import stark.ds.*;
import org.apache.commons.math3.random.RandomGenerator;
import stark.perturbation.*;

import javax.xml.crypto.Data;
import java.util.*;
import java.io.IOException;

public class runningEx {

    public static final int a = 0;
    public static final int b = 1;
    public static final int c = 2;
    public static final int d = 3;
    public static final int C5_count = 4;

    private static final int NUMBER_OF_VARIABLES_A = 4;
    private static final int NUMBER_OF_VARIABLES = 5;

    public static void main(String[] args) {

        try {

            Controller nil = new NilController();
            DataState initialState = getInitialState();

            Controller cont_seq = getContextSequence();
            DataState initialStateInteractive = getInitialStateInteractive();

            RandomGenerator rand = new DefaultRandomGenerator();

            SystemState plainSystem = new ControlledSystem(nil, (rg, ds) -> ds.apply(applyReactions(rg, ds)), initialState);

            SystemState agentSystem = new ControlledSystem(cont_seq, (rg, ds) -> ds.apply(applyReactions(rg, ds)),getInitialStateInteractive());
            SystemState pertSystem = new ControlledSystem(nil, (rg, ds) -> ds.apply(applyReactions(rg, ds)),getInitialStateInteractive());


            int N = 10; // number of steps simulated
            int size = 1; // size of evolution sequences, i.e. cardinality of sample sets (put to 1 as there is no randomness/nondeterminism in the running example)

            ArrayList<DataStateExpression> F = new ArrayList<>();
            ArrayList<String> L = new ArrayList<>();
            L.add("       a   ");
            L.add("     b     ");
            L.add("    c    ");
            L.add("    d  ");
            F.add(ds -> ds.get(a));
            F.add(ds -> ds.get(b));
            F.add(ds -> ds.get(c));
            F.add(ds -> ds.get(d));

            printData(rand, L, F, plainSystem, N, size);

            double[][] sample_a = new double[N][1];
            double[][] sample_b = new double[N][1];
            double[][] sample_c = new double[N][1];
            double[][] sample_d = new double[N][1];

            double[][] sample_run = SystemState.sample(rand, F, plainSystem, N, size);
            for (int i = 0; i<N; i++){
                sample_a[i][0] = sample_run[i][0];
                sample_b[i][0] = sample_run[i][1];
                sample_c[i][0] = sample_run[i][2];
                sample_d[i][0] = sample_run[i][3];
            }

            Util.writeToCSV("./rs_running_sample_a.csv",sample_a);
            Util.writeToCSV("./rs_running_sample_b.csv",sample_b);
            Util.writeToCSV("./rs_running_sample_c.csv",sample_c);
            Util.writeToCSV("./rs_running_sample_d.csv",sample_d);

            int M = 8;

            printData(rand,L,F,agentSystem,M,size);

            double[][] ag_a = new double[M][1];
            double[][] ag_b = new double[M][1];
            double[][] ag_c = new double[M][1];
            double[][] ag_d = new double[M][1];

            double[][] ag_run = SystemState.sample(rand, F, agentSystem, M, size);
            for (int i = 0; i<M; i++){
                ag_a[i][0] = ag_run[i][0];
                ag_b[i][0] = ag_run[i][1];
                ag_c[i][0] = ag_run[i][2];
                ag_d[i][0] = ag_run[i][3];
            }

            Util.writeToCSV("./rs_running_ag_a.csv",ag_a);
            Util.writeToCSV("./rs_running_ag_b.csv",ag_b);
            Util.writeToCSV("./rs_running_ag_c.csv",ag_c);
            Util.writeToCSV("./rs_running_ag_d.csv",ag_d);

            printPertData(rand,L,F,p_cont_seq(),pertSystem,M,size);

            double[][] pert_a = new double[M][1];
            double[][] pert_b = new double[M][1];
            double[][] pert_c = new double[M][1];
            double[][] pert_d = new double[M][1];

            double[][] pert_run = SystemState.sample(rand, F, p_cont_seq(), pertSystem, M, size);
            for (int i = 0; i<M; i++){
                pert_a[i][0] = pert_run[i][0];
                pert_b[i][0] = pert_run[i][1];
                pert_c[i][0] = pert_run[i][2];
                pert_d[i][0] = pert_run[i][3];
            }

            Util.writeToCSV("./rs_running_p_a.csv",pert_a);
            Util.writeToCSV("./rs_running_p_b.csv",pert_b);
            Util.writeToCSV("./rs_running_p_c.csv",pert_c);
            Util.writeToCSV("./rs_running_p_d.csv",pert_d);



        }

        catch (RuntimeException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    // INITIAL DATA STATE

    private static DataState getInitialState() {
        Map<Integer, Double> initialValues = new HashMap<>();

        initialValues.put(a, 1.0);
        initialValues.put(b, 0.0);
        initialValues.put(c, 1.0);
        initialValues.put(d, 1.0);

        return new DataState(NUMBER_OF_VARIABLES_A, i -> initialValues.getOrDefault(i, Double.NaN));
    }

    private static DataState getInitialStateInteractive() {
        Map<Integer, Double> initialValues = new HashMap<>();

        initialValues.put(a, 0.0);
        initialValues.put(b, 0.0);
        initialValues.put(c, 0.0);
        initialValues.put(d, 0.0);
        initialValues.put(C5_count, 0.0);

        return new DataState(NUMBER_OF_VARIABLES, i -> initialValues.getOrDefault(i, Double.NaN));
    }

    public static Controller getContextSequence() {

        ControllerRegistry registry = new ControllerRegistry();

        registry.set("Ag0",
                Controller.doAction(
                        (rg, ds) -> List.of(
                                new DataStateUpdate(a,1),
                                new DataStateUpdate(c,1),
                                new DataStateUpdate(d,1)
                                ),
                        registry.reference("Ag1")
                )
        );

        registry.set("Ag1",
                Controller.doAction(
                        (rg, ds) -> List.of(
                                new DataStateUpdate(b,1)
                        ),
                        registry.reference("Ag2")
                )
        );

        registry.set("Ag2",
                Controller.doAction(
                        (rg, ds) -> List.of(
                                new DataStateUpdate(b,1),
                                new DataStateUpdate(c,1)
                        ),
                        registry.reference("Ag3")
                )
        );

        registry.set("Ag3",
                Controller.doAction(
                        (rg, ds) -> List.of(
                                new DataStateUpdate(b,1)
                        ),
                        registry.reference("Ag4")
                )
        );

        registry.set("Ag4",
                Controller.doTick(
                        registry.reference("Ag5")
                )
        );

        registry.set("Ag5",
                Controller.doAction(
                        (rg, ds) -> List.of(
                                new DataStateUpdate(a,1),
                                new DataStateUpdate(d,1),
                                new DataStateUpdate(C5_count,1)
                        ),
                        registry.reference("Ag5rep")
                )
        );

        registry.set("Ag5rep",
                Controller.ifThenElse(
                        DataState.greaterThan(C5_count, 0),
                        Controller.doAction(
                                (rg, ds) -> List.of(
                                        new DataStateUpdate(a,1),
                                        new DataStateUpdate(d,1),
                                        new DataStateUpdate(C5_count,ds.get(C5_count)-1)
                                ),
                                registry.reference("Ag5rep")
                        ),
                        registry.reference("Ag7")
                )
        );

        registry.set("Ag7",
                Controller.doAction(
                        (rg, ds) -> List.of(
                            new DataStateUpdate(d,1)
                        ),
                        registry.reference("nil")
                )
        );

        registry.set("nil",
                Controller.doTick(registry.reference("nil"))
        );

        return registry.reference("Ag0");
    }


    /*- The method <code>applyReactions</code> maps a random generator <code>rg</code> and a data state <code>ds</code>
    to a list of "updates", which are instances of class <code>DataStateUpdate</code>. Essentially, these updates
    will be used to update the data state <ds> so that the new data state is the one available at the next instant
    after the reactions are applied on <code>ds</code> at present instant.
    */
    private static List<DataStateUpdate> applyReactions(RandomGenerator rg, DataState state) {
        List<DataStateUpdate> updates = new LinkedList<>();

        // auxiliary variables used to ensure the no permanency principle

        double new_a = 0.0;
        double new_b = 0.0;
        double new_c = 0.0;
        double new_d = 0.0;

        // modelling reaction a_1
        if (state.get(a) * state.get(d)==1.0 && state.get(b)==0.0) { // enabling predicate of a_1
            new_a = 1.0;
            new_b = 1.0; // products of a_1
        }
        // modelling reaction a_2
        if (state.get(b)==1.0 & state.get(c)==0.0) { // enabling predicate of a_2
            new_a = 1.0;
            new_d = 1.0; // products of a_2
        }
        updates.add(new DataStateUpdate(a,new_a));
        updates.add(new DataStateUpdate(b,new_b));
        updates.add(new DataStateUpdate(c,new_c));
        updates.add(new DataStateUpdate(d,new_d)); // the updates assign the result of the evaluation of res_A on the current data state to the next data state

        return (updates);

    }

    private static Perturbation p0(){
        return new AtomicPerturbation(0,
                (rg, ds)->ds.apply(List.of(
                        new DataStateUpdate(a,1),
                        new DataStateUpdate(c,1),
                        new DataStateUpdate(d,1)
                ))
        );
    }

    private static Perturbation p1(){
        return new AtomicPerturbation(0,
                (rg, ds)->ds.apply(List.of(
                        new DataStateUpdate(b,1)
                ))
        );
    }

    private static Perturbation p2(){
        return new AtomicPerturbation(0,
                (rg, ds)->ds.apply(List.of(
                        new DataStateUpdate(b,1),
                        new DataStateUpdate(c,1)
                ))
        );
    }

    private static Perturbation p5(){
        return new AtomicPerturbation(0,
                (rg, ds)->ds.apply(List.of(
                        new DataStateUpdate(a,1),
                        new DataStateUpdate(d,1)
                ))
        );
    }

    private static Perturbation p7(){
        return new AtomicPerturbation(0,
                (rg, ds)->ds.apply(List.of(
                        new DataStateUpdate(d,1)
                ))
        );
    }

    private static Perturbation p_cont_seq() {
        return new SequentialPerturbation(p0(),
                    new SequentialPerturbation(p1(),
                            new SequentialPerturbation(p2(),
                                    new SequentialPerturbation(p1(),
                                            new SequentialPerturbation(new NonePerturbation(),
                                                    new SequentialPerturbation(new IterativePerturbation(1,p5()),
                                                            p7())))))
                );
    }


    private static void printData(RandomGenerator rg, ArrayList<String> label, ArrayList<DataStateExpression> F, SystemState s, int steps, int size) {
        System.out.println(label);
        double[][] data = SystemState.sample(rg, F, s, steps, size);
        for (int i = 0; i < data.length; i++) {
            System.out.printf("%d>   ", i);
            for (int j = 0; j < data[i].length -1; j++) {
                System.out.printf("%f   ", data[i][j]);
            }
            System.out.printf("%f\n", data[i][data[i].length -1]);
        }
    }

    private static void printPertData(RandomGenerator rg, ArrayList<String> label, ArrayList<DataStateExpression> F, Perturbation pert, SystemState s, int steps, int size) {
        System.out.println(label);
        double[][] data = SystemState.sample(rg, F, pert, s, steps, size);
        for (int i = 0; i < data.length; i++) {
            System.out.printf("%d>   ", i);
            for (int j = 0; j < data[i].length -1; j++) {
                System.out.printf("%f   ", data[i][j]);
            }
            System.out.printf("%f\n", data[i][data[i].length -1]);
        }
    }







}
