package reacsys;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import org.apache.commons.math3.random.RandomGenerator;
import stark.*;
import stark.controller.Controller;
import stark.controller.NilController;
import stark.distance.*;
import stark.ds.DataState;
import stark.ds.DataStateExpression;
import stark.ds.DataStateUpdate;
import stark.ds.RelationOperator;
import stark.perturbation.*;
import stark.perturbation.AtomicPerturbation;
import stark.perturbation.IterativePerturbation;
import stark.perturbation.Perturbation;
import stark.distance.DistanceExpression;

import stark.Util;
import stark.robtl.AtomicRobustnessFormula;
import stark.robtl.RobustnessFormula;
import stark.robtl.ThreeValuedSemanticsVisitor;
import stark.robtl.TruthValues;

import java.io.IOException;
import java.util.*;

public class Main {

    /*
    SYNAPTIC TRANSMISSION was modeled using REACTION SYSTEMS WITH CONCENTRATION LEVELS in:
    L. Brodo, R. Bruni, M. Falaschi, R. Gori, F. Levi, P. Milazzo:
    "Quantitative extensions of reaction systems based on SOS semantics"
    Neural Computing and Applications 35(9) - 2023.
    Here, we show how in STARK we can model the reaction system in that paper and how STARK can
    be employed to analyze robustness property.
    In particular, we consider the "three neuron model" with the "Y-shape communication" studied by
    Brodo et al. and we analyze robustness with respect to uneffectiveness of neureceptor of neuron #3.
     */

    /*
    The STATE MODEL of the THREE NEURON SYSTEM.
    The three neuron system with Y-shape communication studied in Brodo et al. is a network of 3 neurons,
    where:
    - the neurotransmitters of neuron #1 and neuron #2 interact with the neuroreceptor of neuron #3;
    - the neurotransmitter of neuron #3 interact with the neuroreceptors of both neuron #1 and neuron #2.
    We start with the list of variables that are used to track the status of the system.
     */


    // quantities in neuron #1
    public static final int Ca1 = 0; // amount of calcium
    public static final int X1 = 1; // calcium ligand
    public static final int XStar1 = 2; // compound calcium-ligand
    public static final int Ve1 = 3; // vesicles before exocytosis
    public static final int VeStar1 = 4; // vesicles after exocytosis
    public static final int T1 = 5; // neurotransmitter
    public static final int c1 = 6; // closure status of receptor from neuron #3 (1 = closed, 0 = not closed)
    public static final int o1 = 7; // opening status of receptor from neuron #3 (1 = open, 0 = not open)

    // quantities in neuron #2
    public static final int Ca2 = 8; // amount of calcium
    public static final int X2 = 9; // calcium ligand
    public static final int XStar2 = 10; // compound calcium-ligand
    public static final int Ve2 = 11; // vesicles before exocytosis
    public static final int VeStar2 = 12; // vesicles after exocytosis
    public static final int T2 = 13; // neurotransmitter
    public static final int c2 = 14; // closure status of receptor from neuron #3 (1 = closed, 0 = not closed)
    public static final int o2 = 15; // opening status of receptor from neuron #3 (1 = open, 0 = not open)

    // quantities in neuron #3
    public static final int Ca3 = 16; // amount of calcium
    public static final int X3 = 17; // calcium ligand
    public static final int XStar3 = 18; // compound calcium-ligand
    public static final int Ve3 = 19; // vesicles before exocytosis
    public static final int VeStar3 = 20; // vesicles after exocytosis
    public static final int T3 = 21; // neurotransmitter
    public static final int c31 = 22; // closure status of receptor from neuron #1 (1 = closed, 0 = not closed)
    public static final int o31 = 23; // opening status of receptor from neuron #1 (1 = open, 0 = not open)
    public static final int c32 = 24; // closure status of receptor from neuron #2 (1 = closed, 0 = not closed)
    public static final int o32 = 25; // opening status of receptor from neuron #2 (1 = open, 0 = not open)

    public static final int e31 = 26; // effectiveness of neuroreceptor of neuron #3 from neuron #1 (value in [0,1])
    public static final int e32 = 27; // effectiveness of neuroreceptor of neuron #3 from neuron #2 (value in [0,1])

    private static final int NUMBER_OF_VARIABLES = 28;


    public static void main(String[] args) {

        try {

            /*
            INITIAL CONFIGURATION
            In order to perform simulations/analysis/model checking for a particular system, we need to create its
            initial configuration, which is an instance of <code>ControlledSystem>/code>
            */


            /*
            One of the elements of a system configuration is the "controller", i.e. an instance of <code>Controller</code>.
            In this example we do not need controllers, therefore we use a controller that does nothing, i.e. an instance
            of <code>NilController</code>.
            In other case studies, controllers may be used to control the activity of a system or to implement contexts.
            */

            Controller controller = new NilController();

            /*
            Another element of a system configuration is the "data state", i.e. an instance of <code>DataState</code>,
            which models the state of the data. Instances of <code>DataState</code> contains values for variables
            representing the quantities of the system.
            The initial state <code>initialState</code> is constructed by exploiting the static method
            <code>getInitialState</code>, which will be defined later and assigns the initial value to all 28
            variables defined above.
             */
            DataState initialState = getInitialState();


            /*
            In order to model probabilistic evolution, a system configuration needs a random generator.
             */
            RandomGenerator rand = new DefaultRandomGenerator();

            /*
            We define the <code>ControlledSystem</code> <code>system</code>, which will be the starting configuration from
            which the evolution sequence will be constructed.
            This configuration consists of 3 elements:
            - the controller <code>controller</code> defined above,
            - a random function over data states, which implements interface <code>DataStateFunction</code> and maps a
            random generator <code>rg</code> and a data state <code>ds</code> to the data state obtained by updating
            <code>ds</code> with the list of changes given by method <code>applyReactions</code>. Essentially,
            this static method, defined later, applies the reactions that are promoted/inhibited by the entities in
            <code>ds</code> and produces new entities, which will be available at next instant.
            - the data state <code>initialState</state> defined above,
             */
            SystemState system = new ControlledSystem(controller, (rg, ds) -> ds.apply(applyReactions(rg, ds)), initialState);


            /*

            USING THE SIMULATOR

            We start with generating two evolution sequences from configuration <code>system</system>.
            Both evolution sequences are sequences of length <code>N</code> of sample sets of cardinality
            <code>size</code> of configurations, with the first sample set consisting in <code>size</code> copies of
            <code>system</code>.
            The second evolution sequence is perturbed by applying the perturbation returned by the static method
            <code>itNeuroreceptorComp</code> defined later. Essentially, the method returns a cyclic perturbation that
            affects the effectiveness of neuroreceptor in neuron #3: for <code>replica</code> times, the perturbation
            has no effect for the first <code>w1</code> time points, i.e., the system behaves regularly, then in the
            subsequent <code>w2</code> time points, the effectiveness of neuroreceptor is decremented, meaning that,
            with a parametric probability, the neuroreceptor does not work.

            For both evolution sequences, we store in .csv files some information allowing us to observe the dynamics of
            both the nominal and the perturbed system: for each time unit in [0,N-1] and for each variable, we store
            the average value that the variable assumes in the <code>size</code> configurations in the sample set
            obtained at that time unit. These values are also printed out on the screen.

            */


            System.out.println("");
            System.out.println("Simulation of nominal and perturbed system");
            System.out.println("");

            int N = 1000; // lenght of evolution sequences
            int size = 10000; // size of evolution sequences, i.e. cardinality of sample sets
            int w1=50; // size of time window in which the perturbed neurotransmitter has less effectiveness
            int w2=50; // size of time window in which the perturbed neurotransmitter works regurarly
            int replica=5; // iterations of cyclic perturbation
            double ed=0.01; // probability that the neurtransmitter does not work in perturbed system

            ArrayList<DataStateExpression> F = new ArrayList<>();
            ArrayList<String> L = new ArrayList<>();
            L.add("    Ca1    ");
            L.add("  Ca2       ");
            L.add("Ca3       ");
            L.add("T1        ");
            L.add("T2        ");
            L.add("T3        ");
            L.add("c1        ");
            L.add("c2      ");
            L.add("c31      ");
            L.add("c32       ");
            L.add("o1       ");
            L.add("o2       ");
            L.add("o31      ");
            L.add("o32       ");
            //L.add("e31       ");
            //L.add("e32       ");
            F.add(ds -> ds.get(Ca1));
            F.add(ds -> ds.get(Ca2));
            F.add(ds -> ds.get(Ca3));
            F.add(ds -> ds.get(T1));
            F.add(ds -> ds.get(T2));
            F.add(ds -> ds.get(T3));
            F.add(ds -> ds.get(c1));
            F.add(ds -> ds.get(c2));
            F.add(ds -> ds.get(c31));
            F.add(ds -> ds.get(c32));
            F.add(ds -> ds.get(o1));
            F.add(ds -> ds.get(o2));
            F.add(ds -> ds.get(o31));
            F.add(ds -> ds.get(o32));

            /*
            Static methods <code>printAvgData</code> and <code>printAvgDataPerturbed</code> defined later generate an evolution
            sequence of length <code>N</code> and size <code>size<code> and, at each step in interval
            [<code>lb</code>,<code>ub</code>] print out the average values assumed by variables in list <code>F</code>.
            The sequence generated by <code>printAvgDataPerturbed</code> is affected by a perturbation.
             */
            int lb=0;
            int rb=1000;
            printAvgData(rand, L, F, system, N, size, lb, rb);
            printAvgDataPerturbed(rand, L, F, system, N, size, lb, rb, itNeureceptorComp(ed,w1,w2,replica));


            /*
            In order to plot the results of the simulations, we apply method <code>sample</code> to <code>system</code>.
            The method generate a possibly perturbed sequence, stores the average values assumed by variables at each
            time instant and store them in an array.
             */
            double[][] plot_Ca1 = new double[N][1];
            double[][] plot_Ca2 = new double[N][1];
            double[][] plot_Ca3 = new double[N][1];
            double[][] plot_T1 = new double[N][1];
            double[][] plot_T2 = new double[N][1];
            double[][] plot_T3 = new double[N][1];
            double[][] plot_c1 = new double[N][1];
            double[][] plot_c2 = new double[N][1];
            double[][] plot_c31 = new double[N][1];
            double[][] plot_c32 = new double[N][1];
            double[][] plot_o1 = new double[N][1];
            double[][] plot_o2 = new double[N][1];
            double[][] plot_o31 = new double[N][1];
            double[][] plot_o32 = new double[N][1];

            double[][] plot_pertCa1 = new double[N][1];
            double[][] plot_pertCa2 = new double[N][1];
            double[][] plot_pertCa3 = new double[N][1];
            double[][] plot_pertT1 = new double[N][1];
            double[][] plot_pertT2 = new double[N][1];
            double[][] plot_pertT3 = new double[N][1];
            double[][] plot_pertc1 = new double[N][1];
            double[][] plot_pertc2 = new double[N][1];
            double[][] plot_pertc31 = new double[N][1];
            double[][] plot_pertc32 = new double[N][1];
            double[][] plot_perto1 = new double[N][1];
            double[][] plot_perto2 = new double[N][1];
            double[][] plot_perto31 = new double[N][1];
            double[][] plot_perto32 = new double[N][1];

            double[][] data = SystemState.sample(rand, F, system, N, size);
            double[][] pertData = SystemState.sample(rand, F, itNeureceptorComp(ed, w1, w2, replica), system, N, size);

            for (int i = 0; i < N; i++) {
                plot_Ca1[i][0] = data[i][0];
                plot_Ca2[i][0] = data[i][1];
                plot_Ca3[i][0] = data[i][2];
                plot_T1[i][0] = data[i][3];
                plot_T2[i][0] = data[i][4];
                plot_T3[i][0] = data[i][5];
                plot_c1[i][0] = data[i][6];
                plot_c2[i][0] = data[i][7];
                plot_c31[i][0] = data[i][8];
                plot_c32[i][0] = data[i][9];
                plot_o1[i][0] = data[i][10];
                plot_o2[i][0] = data[i][11];
                plot_o31[i][0] = data[i][12];
                plot_o32[i][0] = data[i][13];
            }
            Util.writeToCSV("./plotRSCa1.csv", plot_Ca1);
            Util.writeToCSV("./plotRSCa2.csv", plot_Ca2);
            Util.writeToCSV("./plotRSCa3.csv", plot_Ca3);
            Util.writeToCSV("./plotRST1.csv", plot_T1);
            Util.writeToCSV("./plotRST2.csv", plot_T2);
            Util.writeToCSV("./plotRST3.csv", plot_T3);
            Util.writeToCSV("./plotRSc1.csv", plot_c1);
            Util.writeToCSV("./plotRSc2.csv", plot_c2);
            Util.writeToCSV("./plotRSc31.csv", plot_c31);
            Util.writeToCSV("./plotRSc32.csv", plot_c32);
            Util.writeToCSV("./plotRSo1.csv", plot_o1);
            Util.writeToCSV("./plotRSo2.csv", plot_o2);
            Util.writeToCSV("./plotRSo31.csv", plot_o31);
            Util.writeToCSV("./plotRSo32.csv", plot_o32);

            for (int i = 0; i < N; i++) {
                plot_pertCa1[i][0] = pertData[i][0];
                plot_pertCa2[i][0] = pertData[i][1];
                plot_pertCa3[i][0] = pertData[i][2];
                plot_pertT1[i][0] = pertData[i][3];
                plot_pertT2[i][0] = pertData[i][4];
                plot_pertT3[i][0] = pertData[i][5];
                plot_pertc1[i][0] = pertData[i][6];
                plot_pertc2[i][0] = pertData[i][7];
                plot_pertc31[i][0] =pertData[i][8];
                plot_pertc32[i][0] =pertData[i][9];
                plot_perto1[i][0] = pertData[i][10];
                plot_perto2[i][0] = pertData[i][11];
                plot_perto31[i][0] = pertData[i][12];
                plot_perto32[i][0] = pertData[i][13];
            }
            Util.writeToCSV("./plotRSpertCa1.csv", plot_pertCa1);
            Util.writeToCSV("./plotRSpertCa2.csv", plot_pertCa2);
            Util.writeToCSV("./plotRSpertCa3.csv", plot_pertCa3);
            Util.writeToCSV("./plotRSpertT1.csv", plot_pertT1);
            Util.writeToCSV("./plotRSpertT2.csv", plot_pertT2);
            Util.writeToCSV("./plotRSpertT3.csv", plot_pertT3);
            Util.writeToCSV("./plotRSpertc1.csv", plot_pertc1);
            Util.writeToCSV("./plotRSpertc2.csv", plot_pertc2);
            Util.writeToCSV("./plotRSpertc31.csv", plot_pertc31);
            Util.writeToCSV("./plotRSpertc32.csv", plot_pertc32);
            Util.writeToCSV("./plotRSperto1.csv", plot_perto1);
            Util.writeToCSV("./plotRSperto2.csv", plot_perto2);
            Util.writeToCSV("./plotRSperto31.csv", plot_perto31);
            Util.writeToCSV("./plotRSperto32.csv", plot_perto32);


            EvolutionSequence sequence = new EvolutionSequence(rand, rg -> system, 10);

             /*
            The following instruction allows us to create the evolution sequence <code>sequence_pert</code>, which is
            obtained from the evolution sequence <code>sequence</code> by applying a perturbation, where:
            - as above, the perturbation is returned by the static method <code>itNeureceptorComp()</code> defined later
            - the perturbation is applied at step 0
            - the sample sets of configurations in <code>sequence_pert</code> have a cardinality which corresponds to that
            of <code>sequence</code> multiplied by <code>scale>/code>
            */


            int scale=5;
            EvolutionSequence sequence_pert = sequence.apply(itNeureceptorComp(ed, w1, w2, replica),0,scale);

            /*
            The following lines of code define three atomic distances between evolution sequences, named
            <code>atomicCai</code> for i=1,2,3. Then, these distances are evaluated, time-point by time-point, over
            evolution sequence <code>sequence</code> and its perturbed version <code>sequence_pert</code> defined above.
            Finally, the time-point to time-point values of the distances are stored in .csv files.
            Technically, <code>distanceCai</code> is an atomic distance in the sense that it is an instance of
            class <code>AtomicDistanceExpression</code>, which consists in a data state expression,
            which maps a data state to a number, or rank, and a binary operator. Given two configurations, the data
            state expression allows us to get the normalised value of <code>Cai</code>, which is a value in [0,1], from
            both configurations, and the binary operator gives us their difference, which, intuitively, is the difference
            with respect to the level of Cai between the two configurations.
            This distance will be lifted to two sample sets of configurations, those obtained from <code>sequence</code> and
            <code>sequence_p</code> at the same step.
            Then, we define an instance of <code>MaxDistanceExpression</code>, which is a class realizes the max between
            two argument distance expressions. Intuitively, <code>maxAtomicCa123</code> is the max distance among
            <code>atomicCa1</code>, <code>atomicCa2</code> and <code>atomicCa3</code>.
            */

            DistanceExpression atomicCa1 = new AtomicDistanceExpression(ds->ds.get(Ca1), (a,b)->Math.abs(a-b)/20);
            DistanceExpression atomicCa2 = new AtomicDistanceExpression(ds->ds.get(Ca2), (a,b)->Math.abs(a-b)/20);
            DistanceExpression atomicCa3 = new AtomicDistanceExpression(ds->ds.get(Ca3), (a,b)->Math.abs(a-b)/20);

            DistanceExpression maxAtomicCa123 = new MaxDistanceExpression(
                    atomicCa1,
                    new MaxDistanceExpression(atomicCa2,atomicCa3)
            );

            /*
            Now we compute the step-by-step distances between <code>sequence</code> and <code>sequence_pert</code>
             */
            double[][] direct_evaluation_atomic_Ca1 = new double[rb-lb][1];
            double[][] direct_evaluation_atomic_Ca2 = new double[rb-lb][1];
            double[][] direct_evaluation_atomic_Ca3 = new double[rb-lb][1];
            double[][] direct_evaluation_max_atomic_Ca123 = new double[rb-lb][1];

            for (int i = 0; i<(rb-lb); i++){
                direct_evaluation_atomic_Ca1[i][0] = atomicCa1.compute(i+lb, sequence, sequence_pert);
                direct_evaluation_atomic_Ca2[i][0] = atomicCa2.compute(i+lb, sequence, sequence_pert);
                direct_evaluation_atomic_Ca3[i][0] = atomicCa3.compute(i+lb, sequence, sequence_pert);
                direct_evaluation_max_atomic_Ca123[i][0] = maxAtomicCa123.compute(i+lb,sequence,sequence_pert);
            }
            for (int i = 0; i<(rb-lb); i++){
                System.out.println(direct_evaluation_max_atomic_Ca123[i][0]);
            }
            Util.writeToCSV("./plotRSatomic_Ca1.csv",direct_evaluation_atomic_Ca1);
            Util.writeToCSV("./plotRSatomic_Ca2.csv",direct_evaluation_atomic_Ca2);
            Util.writeToCSV("./plotRSatomic_Ca3.csv",direct_evaluation_atomic_Ca3);
            Util.writeToCSV("./plotRSmax_atomic_Ca123.csv",direct_evaluation_max_atomic_Ca123);



            /*
            Now we define an instance of <code>MaxIntervalDistanceExpression</code>, which is a class that implements a
            distance that is the max of the argument distance in the argument interval.
             */

            DistanceExpression maxIntCa123 = new MaxIntervalDistanceExpression(maxAtomicCa123,lb,rb);
            double maxD = maxIntCa123.compute(0,sequence,sequence_pert);
            System.out.println("The max atomic distance is: "+ maxD);


            /*
            Then, we define a robustness formula, in particular an atomic formula, namely an instance of
            <code>AtomicRobustnessFormula</code>.
            This formula will be evaluated on the evolution sequence <code>sequence</code> and expresses that the
            distance, expressed by expression distance <code>maxIntCa123</code> between that evolution
            sequence and the evolution sequence obtained from it by applying the perturbation returned by method
            <code>itNeureceptorComp</code>, is below a given threshold.
            We evaluate the formula for several thresholds and for several values for uneffectiveness of neuroreceptor
            of neuron #3.

             */

            double[][] robEvaluationsVaryingThreshold = new double[12][2];
            RobustnessFormula robustF;
            int index=0;
            double thresholdB = 1;
            for(int i = 14; i < 26 ; i=i+1){
                double threshold = thresholdB + i;
                threshold = threshold / 100;
                robustF = new AtomicRobustnessFormula(itNeureceptorComp(ed,w1,w2,replica),
                        maxIntCa123,
                        RelationOperator.LESS_OR_EQUAL_THAN,
                        threshold);
                TruthValues value = new ThreeValuedSemanticsVisitor(rand,50,1.96).eval(robustF).eval(300, 0, sequence);
                System.out.println(" ");
                System.out.println("\n robustF evaluation at " + threshold + ": " + value);
                robEvaluationsVaryingThreshold[index][1]=value.valueOf();
                robEvaluationsVaryingThreshold[index][0]=threshold;
                index++;
            }
            Util.writeToCSV("./plotRSevalRVaryingThreshold.csv",robEvaluationsVaryingThreshold);

            double[][] robEvaluationsVaryingEd = new double[9][2];
            double threshold = 0.15;
            double[] eda = new double[ ] {0.010,0.009,0.008,0.007,0.006,0.005,0.004,0.003,0.002};
            index=0;
            for(int i = 0; i < 9 ; i=i+1){
                ed=eda[i];
                robustF = new AtomicRobustnessFormula(itNeureceptorComp(ed,w1,w2,replica),
                        maxIntCa123,
                        RelationOperator.LESS_OR_EQUAL_THAN,
                        threshold);
                TruthValues value = new ThreeValuedSemanticsVisitor(rand,50,1.96).eval(robustF).eval(300, 0, sequence);
                System.out.println(" ");
                System.out.println("\n robustF evaluation with ed=" + ed + ": " + value);
                robEvaluationsVaryingEd[index][1]=value.valueOf();
                robEvaluationsVaryingEd[index][0]=ed;
                index++;
            }
            Util.writeToCSV("./plotRSevalRVaryingEd.csv",robEvaluationsVaryingEd);




        }





        catch (RuntimeException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    /*- The method <code>applyReactions</code> maps a random generator <code>rg</code> and a data state <code>ds</code>
    to a list of "updates", which are instances of class <code>DataStateUpdate</code>. Essentially, these updates
    will be used to update the data state <ds> so that the new data state is the one available at the next instant
    after the reactions are applied on <code>ds</code> at present instant.

     */
    private static List<DataStateUpdate> applyReactions(RandomGenerator rg, DataState state) {
        List<DataStateUpdate> updates = new LinkedList<>();

        // Updates on calcium - variables Ca1, Ca2, Ca3

        // new value for calcium in first neuron, Ca1 - reactions r11, r19 can produce it
        if (state.get(o1) == 1) { // postsynaptic activity, neural receptor open - reaction r91
            updates.add(new DataStateUpdate(Ca1, 1.0)); //
        } else {
            if (state.get(Ca1) > 0 & state.get(Ca1) < 10) { // presynaptic activity: Ca doubles until it reaches threshold 10 - reaction r11
                updates.add(new DataStateUpdate(Ca1, state.get(Ca1) * 2));
            } else {
                updates.add(new DataStateUpdate(Ca1, 0.0));
            }
        }

        // new value for calcium in second neuron, Ca2 - reactions r21, r29 can produce it
        if (state.get(o2) == 1) { // postsynaptic activity, neural receptor open - reaction r29
            updates.add(new DataStateUpdate(Ca2, 1.0));
        } else {
            if (state.get(Ca2) > 0 & state.get(Ca2) < 10) { //presynaptic activity: Ca doubles until it reaches threshold 10 - reaction r21
                updates.add(new DataStateUpdate(Ca2, state.get(Ca2) * 2));
            } else {
                updates.add(new DataStateUpdate(Ca2, 0.0));
            }
        }

        // new value for calcium in third neuron, Ca3 - reactions r31, r39a, r39b, r39c can produce it
        if (state.get(o31) == 1 & state.get(o32) == 1) { // postisynaptic activity, both neural receptors open - reaction r39c
            updates.add(new DataStateUpdate(Ca3, 4.0));
        } else {
            if (state.get(o31) == 1 || state.get(o32) == 1) { // postisynaptic activity, one neural receptor open - reaction r39a or r39b
                updates.add(new DataStateUpdate(Ca3, 1.0));
            } else {
                if (state.get(Ca3) > 0 & state.get(Ca3) < 10) { //presynaptic activity: Ca doubles until it reaches threshold 10 - r31
                    updates.add(new DataStateUpdate(Ca3, state.get(Ca3) * 2)); //presynaptic activity: Ca doubles until it reaches threshold 10 - r31
                } else {
                    updates.add(new DataStateUpdate(Ca3, 0.0));
                }
            }
        }


        // Updates on calcium ligand - variables X1, X2, X3

        // new value for calcium ligand in first neuron, X1 - reactions r12, r15 can produce it
        if (state.get(XStar1) == 10 & state.get(Ve1) > 0) { // formation of vesicles - reaction r15
            updates.add(new DataStateUpdate(X1, 10.0));
        } else {
            if (state.get(X1) > 0 & state.get(XStar1) == 0) { // permanency of calcium ligand - reaction r12
                updates.add(new DataStateUpdate(X1, state.get(X1)));
            } else {
                updates.add(new DataStateUpdate(X1, 0.0));
            }
        }

        // new value for calcium ligand in second neuron, X2 - reactions r22, r25 can produce it
        if (state.get(XStar2) == 10 & state.get(Ve2) > 0) { // formation of vesicles - reaction r25
            updates.add(new DataStateUpdate(X2, 10.0));
        } else {
            if (state.get(X2) > 0 & state.get(XStar2) == 0) {
                updates.add(new DataStateUpdate(X2, state.get(X2))); // permanency of calcium ligand - reaction r22
            } else {
                updates.add(new DataStateUpdate(X2, 0.0));
            }
        }

        // new value for calcium ligand in third neuron, X3 - reactions r32, r35 can produce it
        if (state.get(XStar3) == 10 & state.get(Ve3) > 0) { // formation of vesicles - reaction r35
            updates.add(new DataStateUpdate(X3, 10.0));
        } else {
            if (state.get(X3) > 0 & state.get(XStar3) == 0) {
                updates.add(new DataStateUpdate(X3, state.get(X3))); // permanency of calcium ligand - reaction r32
            } else {
                updates.add(new DataStateUpdate(X3, 0.0));
            }
        }


        // Updates on vesicles before exocytosis - variables Ve1, Ve2, Ve3

        // new values for vesicles in neuron 1, Ve1 - reactions r13 and r16 can produce it
        if (state.get(Ve1) > 0 & state.get(VeStar1) == 0) { // permanency of vesicles - reaction r13
            updates.add(new DataStateUpdate(Ve1, state.get(Ve1)));
        } else {
            if (state.get(VeStar1) > 0) { // neurotransmitter released - reaction r16
                updates.add(new DataStateUpdate(Ve1, state.get(VeStar1)));
            } else {
                updates.add(new DataStateUpdate(Ve1, 0.0));
            }
        }

        // new values for vesicles in neuron 2, Ve2 - reactions r23 and r26 can produce it
        if (state.get(Ve2) > 0 & state.get(VeStar2) == 0) { // permanency of vesicles - reaction r23
            updates.add(new DataStateUpdate(Ve2, state.get(Ve2)));
        } else {
            if (state.get(VeStar2) > 0) { // neurotransmitter released - reaction r26
                updates.add(new DataStateUpdate(Ve2, state.get(VeStar2)));
            } else {
                updates.add(new DataStateUpdate(Ve2, 0.0));
            }
        }

        // new values for vesicles in neuron 3, Ve3 - reactions r33 and r36 can produce it
        if (state.get(Ve3) > 0 & state.get(VeStar3) == 0) { // permanency of vesicles - reaction r33
            updates.add(new DataStateUpdate(Ve3, state.get(Ve3)));
        } else {
            if (state.get(VeStar3) > 0) { // neurotransmitter released - reaction r36
                updates.add(new DataStateUpdate(Ve3, state.get(VeStar3)));
            } else {
                updates.add(new DataStateUpdate(Ve3, 0.0));
            }
        }


        // Updates on complex calcium-ligand - variables XStar1, XStar2, XStar3

        // new values for complex calcium-ligand in neuron 1, XStar1 - reaction r14 can produce it
        if (state.get(Ca1) >= 10 & state.get(X1) >= 10) { // enough calcium to form the complex - reaction r14
            updates.add(new DataStateUpdate(XStar1, state.get(X1)));
        } else {
            updates.add(new DataStateUpdate(XStar1, 0.0));
        }

        // new values for complex calcium-ligand in neuron 2, XStar2 - reaction r24 can produce it
        if (state.get(Ca2) >= 10 & state.get(X2) >= 10) { // enough calcium to form the complex - reaction r24
            updates.add(new DataStateUpdate(XStar2, state.get(X2)));
        } else {
            updates.add(new DataStateUpdate(XStar2, 0.0));
        }

        // new values for complex calcium-ligand in neuron 3, XStar3 - reaction r34 can produce it
        if (state.get(Ca3) >= 10 & state.get(X3) >= 10) { // enough calcium to form the complex - reaction r34
            updates.add(new DataStateUpdate(XStar3, state.get(X3)));
        } else {
            updates.add(new DataStateUpdate(XStar3, 0.0));
        }


        // Updates on vesicles with neurotransmitter - variables VeStar1, VeStar2, VeStar3

        // new values for vesicles with neurotransmitter in first neuron, VeStar1 - reactons r15 can produce it
        if (state.get(XStar1) == 10 & state.get(Ve1) > 0) { // enough calcium ligand to release the neurotransmitter - reaction r15
            updates.add(new DataStateUpdate(VeStar1, state.get(Ve1)));
        } else {
            updates.add(new DataStateUpdate(VeStar1, 0.0));
        }

        // new values for vesicles with neurotransmitter in second neuron, VeStar2 - reactons r25 can produce it
        if (state.get(XStar2) == 10 & state.get(Ve2) > 0) {// enough calcium ligand to release the neurotransmitter - reaction r25
            updates.add(new DataStateUpdate(VeStar2, state.get(Ve2)));
        } else {
            updates.add(new DataStateUpdate(VeStar2, 0.0));
        }

        // new values for vesicles with neurotransmitter in third neuron, VeStar3 - reactions r35 can produce it
        if (state.get(XStar3) == 10 & state.get(Ve3) > 0) {// enough calcium ligand to release the neurotransmitter - reaction r35
            updates.add(new DataStateUpdate(VeStar3, state.get(Ve3)));
        } else {
            updates.add(new DataStateUpdate(VeStar3, 0.0));
        }


        // Updates on neurotransmitter - variables T1, T2, T3

        // new values for neurotransmitter from first neuron, T1 - reaction r16 can produce it
        if (state.get(VeStar1) > 0) { // neurotransmitter released - reaction r16
            updates.add(new DataStateUpdate(T1, 1.0));
        } else {
            updates.add(new DataStateUpdate(T1, 0.0));
        }

        // new values for neurotransmitter from second neuron, T2 - reaction r36 can produce it
        if (state.get(VeStar2) > 0) { // neurotransmitter released - reaction r26
            updates.add(new DataStateUpdate(T2, 1.0));
        } else {
            updates.add(new DataStateUpdate(T2, 0.0));
        }

        // new values for neurotransmitter from third neuron, T3 - reaction r36 can produce it
        if (state.get(VeStar3) > 0) { // neurotransmitter released - reaction r36
            updates.add(new DataStateUpdate(T3, 1.0));
        } else {
            updates.add(new DataStateUpdate(T3, 0.0));
        }


        // New values for closure/opening states of neuroreceptors - variables c1, o1, c2, o2, c31, o31, c32, o32

        // new values for closing state of neuroreceptor of neuron 1, c1 - reactions r17 and r19 can modify it
        if ((state.get(c1) > 0 & state.get(T3) == 0) || state.get(o1) > 0) {
            // neuroreceptor remains closed if there is no neurotransmitter or becomes closed if it is open
            // - reaction r17 or r19
            updates.add(new DataStateUpdate(c1, 1.0));
        } else {
            updates.add(new DataStateUpdate(c1, 0.0));
        }

        // new values for closing state of neuroreceptor of neuron 2, c2 - reactions r27 and r29 can modify it
        if ((state.get(c2) > 0 & state.get(T3) == 0) || state.get(o2) > 0) {
            // neuroreceptor remains closed if there is no neurotransmitter or becomes closed if it is open
            // - reaction r27 or r29
            updates.add(new DataStateUpdate(c2, 1.0));
        } else {
            updates.add(new DataStateUpdate(c2, 0.0));
        }

        // new values for opening state of neuroreceptor of neuron 1, o1 -  reactions r18  can modify it
        if (state.get(T3) > 0) {
            // neuroreceptor becomes open if there is the neurotransmitter - reaction r18
            updates.add(new DataStateUpdate(o1, 1.0));
        } else {
            updates.add(new DataStateUpdate(o1, 0.0));
        }


        // new values for opening state of neuroreceptor of neuron 2, o2 -  reactions r18  can modify it
        if (state.get(T3) > 0) {
            // neuroreceptor becomes open if there is the neurotransmitter
            updates.add(new DataStateUpdate(o2, 1.0));
        } else {
            updates.add(new DataStateUpdate(o2, 0.0));
        }


        double r31 = rg.nextDouble();

        double r32 = rg.nextDouble();

        boolean w31 = (r31 < state.get(e31));
        boolean w32 = (r32 < state.get(e32));


        // new values for closure state of first neuroreceptor of neuron 3, o31 -  reactions r37a,r39a,r39c  can modify it
        if ((state.get(c31) > 0 & state.get(T1) == 0) || (state.get(c31) > 0 & state.get(T1) == 1 & !w31)) {
            // neuroreceptor from neuron #1 remains closed in absence of the neurotransmitter - r37a
            updates.add(new DataStateUpdate(c31, state.get(c31)));
        } else {
            if (state.get(o31) == 1) {
                // neuroreceptor from neuron #1 closes if it is open - r39a or r39a
                updates.add(new DataStateUpdate(c31, 1.0));
            } else {
                updates.add(new DataStateUpdate(c31, 0.0));
            }
        }

        if ((state.get(c32) > 0 & state.get(T2) == 0) || (state.get(c32) > 0 & state.get(T2) == 1 & !w32)) {
            // neuroreceptor from neuron #2 remains closed in absence of the neurotransmitter - r37b
            updates.add(new DataStateUpdate(c32, state.get(c32)));
        } else {
            if (state.get(o32) == 1) {
                // neuroreceptor from neuron #2 closes if it is open - r39b or r39c
                updates.add(new DataStateUpdate(c32, 1.0));
            } else {
                updates.add(new DataStateUpdate(c32, 0.0));
            }
        }

        // new values for opening state of first neuroreceptor of neuron 3, o31 -  reactions r38a  can modify it

        if (state.get(T1) > 0 & w31) { // neuroreceptor opens if there is the neurotransmitter
            updates.add(new DataStateUpdate(o31, 1.0));
        } else {
            updates.add(new DataStateUpdate(o31, 0.0));
        }

        // new values for opening state of second neuroreceptor of neuron 3, o32 -  reactions r38b  can modify it
        if (state.get(T2) > 0 & w32) {// neuroreceptor opens if there is the neurotransmitter
            updates.add(new DataStateUpdate(o32, 1.0));
        } else {
            updates.add(new DataStateUpdate(o32, 0.0));
        }

        return (updates);

    }


    // Method <code>getInitialState</code> assigns the initial value to all 28 variables.
    // To reproduce the same results as in Brodo et al. we start with the following levels for calcium:
    // Ca1=1; Ca2=0; Ca3=0.
    // Moreover, there are no neurotransmitters (T1=T2=T3=0), and all neuroreceptors are closed
    // (c1,c2,c31,c32=1 and o1=o2=o31=o32=0).
    // Technically, the metrod returns an instance of <code>DataState</code>

    private static DataState getInitialState() {
        Map<Integer, Double> initialValues = new HashMap<>();

        // first neuron
        initialValues.put(Ca1, 1.0);
        initialValues.put(X1, 10.0);
        initialValues.put(XStar1, 0.0);
        initialValues.put(Ve1, 5.0);
        initialValues.put(VeStar1, 0.0);
        initialValues.put(T1, 0.0);
        initialValues.put(c1, 1.0);
        initialValues.put(o1, 0.0);

        // second neuron
        initialValues.put(Ca2, 0.0);
        initialValues.put(X2, 10.0);
        initialValues.put(XStar2, 0.0);
        initialValues.put(Ve2, 5.0);
        initialValues.put(VeStar2, 0.0);
        initialValues.put(T2, 0.0);
        initialValues.put(c2, 1.0);
        initialValues.put(o2, 0.0);

        // third neuron
        initialValues.put(Ca3, 0.0);
        initialValues.put(X3, 10.0);
        initialValues.put(XStar3, 0.0);
        initialValues.put(Ve3, 5.0);
        initialValues.put(VeStar3, 0.0);
        initialValues.put(T3, 0.0);
        initialValues.put(c31, 1.0);
        initialValues.put(o31, 0.0);
        initialValues.put(c32, 1.0);
        initialValues.put(o32, 0.0);

        // neuroreceptor effectiveness
        initialValues.put(e31, 1.0);
        initialValues.put(e32, 1.0);


        return new DataState(NUMBER_OF_VARIABLES, i -> initialValues.getOrDefault(i, Double.NaN));
    }


    private static double[] printAvgData(RandomGenerator rg, ArrayList<String> label, ArrayList<DataStateExpression> F, SystemState s, int steps, int size, int leftbound, int rightbound) {
        System.out.println(label);
        /*
        The following instruction creates an evolution sequence consisting in a sequence of <code>steps</code> sample
        sets of cardinality <size>.
        The first sample set contains <code>size</code> copies of configuration <code>s</code>.
        The subsequent sample sets are derived by simulating the dynamics.
        For each step from 1 to <code>steps</code> and for each variable, the average value taken by the
        variables in the elements of the sample set at each step are printed out.
         */
        double[][] data_avg = SystemState.sample(rg, F, s, steps, size);
        double[] tot = new double[F.size()];
        Arrays.fill(tot, 0);
        for (int i = 0; i < data_avg.length; i++) {
            System.out.printf("%d>   ", i);
            for (int j = 0; j < data_avg[i].length - 1; j++) {
                System.out.printf("%f   ", data_avg[i][j]);
                if (leftbound <= i & i <= rightbound) {
                    tot[j] = tot[j] + data_avg[i][j];
                }
            }
            System.out.printf("%f\n", data_avg[i][data_avg[i].length - 1]);
            if (leftbound <= i & i <= rightbound) {
                tot[data_avg[i].length - 1] = tot[data_avg[i].length - 1] + data_avg[i][data_avg[i].length - 1];
            }
        }
        System.out.println(" ");
        System.out.println("Avg over all steps of the average values taken in the single step by the variables:");
        for (int j = 0; j < tot.length - 1; j++) {
            System.out.printf("%f   ", tot[j] / (rightbound - leftbound));
        }
        System.out.printf("%f\n", tot[tot.length - 1] / (rightbound - leftbound));
        System.out.println("");
        System.out.println("");
        return tot;
    }




    private static double[] printAvgDataPerturbed(RandomGenerator rg, ArrayList<String> label, ArrayList<DataStateExpression> F, SystemState s, int steps, int size, int leftbound, int rightbound, Perturbation perturbation) {
        System.out.println(label);

        double[] tot = new double[F.size()];
        double[][] data_avg = SystemState.sample(rg, F, perturbation, s, steps, size);
        Arrays.fill(tot, 0);
        for (int i = 0; i < data_avg.length; i++) {
            System.out.printf("%d>   ", i);
            for (int j = 0; j < data_avg[i].length -1 ; j++) {
                System.out.printf("%f   ", data_avg[i][j]);
                if (leftbound <= i & i <= rightbound) {
                    tot[j]=tot[j]+data_avg[i][j];
                }
            }
            System.out.printf("%f\n", data_avg[i][data_avg[i].length -1]);
            if (leftbound <= i & i <= rightbound) {
                tot[data_avg[i].length -1]=tot[data_avg[i].length -1]+data_avg[i][data_avg[i].length -1];
            }
        }
        System.out.println("");
        System.out.println("Avg over all steps of the average values taken in the single step by the variables:");
        for(int j=0; j<tot.length-1; j++){
            System.out.printf("%f   ", tot[j] / (rightbound-leftbound));
        }
        System.out.printf("%f\n", tot[tot.length-1]/ (rightbound-leftbound));
        System.out.println("");
        return tot;
    }













    private static List<DataStateUpdate> upd_e31_e32(RandomGenerator rg, DataState state, double x) {
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(e31, Math.max(state.get(e31) + x, 0)));
        updates.add(new DataStateUpdate(e32, Math.max(state.get(e31) + x, 0)));
        return updates;
    }




    /*
    public static Perturbation itNeureceptorComp(double x, int w1, int w2, int w3, int replica){
        return new IterativePerturbation(replica,
                new SequentialPerturbation(
                        new AtomicPerturbation(w1, (rg,ds)->ds.apply(upd_e31_e32(rg,ds,-x))),
                        new SequentialPerturbation(
                               new AtomicPerturbation(w2,(rg,ds)->ds.apply(upd_e31_e32(rg,ds,+x))),
                               new AtomicPerturbation(w3,(rg,ds)->ds.apply(upd_e31_e32(rg,ds,0)))
                        )
                )
        );
    }


     */



    /*
    The following method returns a perturbation. In particular this is an iterative perturbation, i.e. an instance of
    <code>IterativePerturbation</code>. It consists of two elements, an integer and a body perturbation, the idea being
    that the integer expresses how many times the body perturbation is applied.
    In this case, the body perturbation is a sequential perturbation, namely an instance of
    <code>SequentialPerturbation</code>, which, essentially, consists in two perturbations where the second is applied
    after the first has terminated its effect. The first perturbation is applied after <code>w1</code> steps
    and . The second perturbation is applied after <code>w2</code>
    steps and reverts the effects of the first one.

     */
    public static Perturbation itNeureceptorComp(double x, int w1, int w2, int replica){
        return new IterativePerturbation(replica,
                new SequentialPerturbation(
                        new AtomicPerturbation(w1,(rg,ds)->ds.apply(upd_e31_e32(rg,ds,-x))),
                        new AtomicPerturbation(w2,(rg,ds)->ds.apply(upd_e31_e32(rg,ds,+x)))
                )
        );
    }



}