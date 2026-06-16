package reacsys;

import org.apache.commons.math3.random.RandomGenerator;
import stark.*;
import stark.controller.Controller;
import stark.controller.ControllerRegistry;
import stark.controller.NilController;
import stark.controller.ParallelController;
import stark.distl.*;
import stark.ds.DataState;
import stark.ds.DataStateExpression;
import stark.ds.DataStateFunction;
import stark.ds.DataStateUpdate;

import java.io.IOException;
import java.util.*;

public class MainLO {

    /*
    The LAC OPERON was modeled in Reaction System in the following paper:
    Luca Corolli, Carlo Maja, Fabrizio Marini, Daniela Besozzi, Giancarlo Mauri:
    An excursion in reaction systems: From computer science to biology.
    Theoretical Computer Science 454(2012) 95-108.

    We start with the list of variables that are used to track the status of the system.
     */

    public static final int lac = 0;
    public static final int Z = 1; // enzyme
    public static final int Y = 2; // transporter
    public static final int A = 3; // enzyme
    public static final int lacI = 4; // gene encoding repressor protein
    public static final int I = 5; // represson protein
    public static final int IOP = 6; // repressor bounded to operator
    public static final int cya = 7; // gene encoding protein CAP
    public static final int cAMP = 8; // signal molecule
    public static final int crp = 9; // gene encoding signal molecule cAMP
    public static final int CAP = 10; // protein
    public static final int cAMPCAP= 11; // complex CAP - cAMP
    public static final int lactose = 12; // lactose
    public static final int glucose = 13; // glucose

    public static final int lactose_N = 14; // lactose provided by context
    public static final int glucose_N = 15; // glucose provided by context

    private static final int NUMBER_OF_VARIABLES = 16;

    public static void main(String[] args){

        try {

        /*
            INITIAL CONFIGURATION
            In order to perform simulations/analysis/model checking for a particular system, we need to create its
            initial configuration, which is an instance of <code>ControlledSystem>/code>
            */

            /*
            One of the elements of a system configuration is the "controller", i.e. an instance of <code>Controller</code>.
            In this example we use the controller named <code>context</code>, which is returned by static method
            <code>getController</code>. Essentially, this controller implements contexts of the Reaction System.
            Controller <code>context</code> supplies what is called the "default context" in [Corolli et al.], which
            mimics the real biological system in which the genomic elements plus their encoded proteins are normally
            present, and decides, step-by-step, when to supply lactose and glucose.

            */
            Controller context = getController();


        /*
            Another element of a system configuration is the "data state", i.e. an instance of <code>DataState</code>,
            which models the state of the data. Instances of <code>DataState</code> contains values for variables
            representing the quantities of the system.
            The initial state <code>initialState</code> is constructed by exploiting the static method
            <code>getInitialState</code>, which will be defined later and assigns the initial value to all
            variables defined above.
             */
            DataState initialState = getInitialState();



        /*
            In order to model probabilistic evolution, a system configuration needs a random generator.
             */
            RandomGenerator rand = new DefaultRandomGenerator();

            /*
            We define the <code>ControlledSystem</code> <code>system</code>, which will be the starting configuration from
            which the evolution sequences will be constructed.
            This configuration consists of 3 elements:
            - the controller <code>context</code> defined above,
            - a random function over data states, which implements interface <code>DataStateFunction</code> and maps a
            random generator <code>rg</code> and a data state <code>ds</code> to the data state obtained by updating
            <code>ds</code> with the list of changes given by method <code>applyReactions</code>. Essentially,
            this static method, defined later, applies the reactions that are promoted/inhibited by the entities in
            <code>ds</code> and produces new entities, which will be available at next instant. In this example we don't
            use randomness.
            - the data state <code>initialState</state> defined above,
             */

            SystemState system = new ControlledSystem(context, (rg, ds) -> ds.apply(applyReactions(rg, ds)), initialState);


            // N is the length of the evolution sequence (i.e. the number of steps) we want to analyse.

            int N = 100;

        /*
        Below the code needed to simulate N steps of the system.
        The value of variables is printed out on the screen and saved in .csv files for plotting.
         */
            ArrayList<DataStateExpression> F = new ArrayList<>();
            ArrayList<String> L = new ArrayList<>();
            L.add("       lac ");
            L.add("      Z ");
            L.add("      Y ");
            L.add("      A ");
            L.add("      lacI ");
            L.add("      I ");
            L.add("       IOP ");
            L.add("     cya ");
            L.add("    cAMP ");
            L.add("    crp ");
            L.add("     CAP ");
            L.add("   cAMPCAP ");
            L.add("  lac ");
            L.add("     glu    ");
            F.add(ds -> ds.get(lac));
            F.add(ds -> ds.get(Z));
            F.add(ds -> ds.get(Y));
            F.add(ds -> ds.get(A));
            F.add(ds -> ds.get(lacI));
            F.add(ds -> ds.get(I));
            F.add(ds -> ds.get(IOP));
            F.add(ds -> ds.get(cya));
            F.add(ds -> ds.get(cAMP));
            F.add(ds -> ds.get(crp));
            F.add(ds -> ds.get(CAP));
            F.add(ds -> ds.get(cAMPCAP));
            F.add(ds -> ds.get(lactose_N));
            F.add(ds -> ds.get(glucose_N));


            printAvgData(rand, L, F, system, N, 1, 0, 41);

            int obs_steps=40;

            double[][] plot_Z = new double[obs_steps][1];
            double[][] plot_Y = new double[obs_steps][1];
            double[][] plot_A = new double[obs_steps][1];
            double[][] plot_lactose_N = new double[obs_steps][1];
            double[][] plot_glucose_N = new double[obs_steps][1];



            double[][] data = SystemState.sample(rand, F, system, obs_steps, 1);

            for (int i = 0; i < obs_steps; i++) {
                plot_glucose_N[i][0] = data[i][13];
                plot_lactose_N[i][0] = data[i][12];
                plot_Z[i][0] = data[i][1];
                plot_Y[i][0] = data[i][2];
                plot_A[i][0] = data[i][3];

            }
            Util.writeToCSV("./LOGlucoseN.csv", plot_glucose_N);
            Util.writeToCSV("./LOLactoseN.csv", plot_lactose_N);
            Util.writeToCSV("./LOZ.csv", plot_Z);
            Util.writeToCSV("./LOY.csv", plot_Y);
            Util.writeToCSV("./LOA.csv", plot_A);


            EvolutionSequence sequence = new EvolutionSequence(rand, rg -> system, 1);

        /*
        The DisTLformula <code>lacExpressionAfterOnlyLactose</code> defined below expresses that it is always true
        that lac operon is expressed at step n+2 (proteins <code>Z</code>, <code>Y</code>, <code>A</code> are present)
        if and only if at step n lactose is present and glucose is absent.
         */

            DataStateFunction mu_lacExpressed = (rg, ds) -> ds.apply(getDiracLacExpressed(rg, ds));

            DataStateFunction mu_onlyLactose = (rg, ds) -> ds.apply(getDiracOnlyLactose(rg, ds));

            DisTLFormula target_lacExpressed = new TargetDisTLFormula(
                    mu_lacExpressed,
                    ds->1.0-Math.min(Math.min(ds.get(Z), ds.get(Y)), ds.get(A)),
                    //ds -> 1-ds.get(Z),
                            0.2
            );
            DisTLFormula target_onlyLactose = new TargetDisTLFormula(
                    mu_onlyLactose,
                    ds -> 1-Math.min(ds.get(lactose), 1 - ds.get(glucose)),
                    0.3
            );
            DisTLFormula lacExpressionAfterOnlyLactose = new ImplicationDisTLFormula(
                    target_onlyLactose,
                    new EventuallyDisTLFormula(target_lacExpressed, 2, 2)
            );

            DisTLFormula lacExpression = new AlwaysDisTLFormula(
                    lacExpressionAfterOnlyLactose,
                    0,
                    39
            );



            //System.out.println("''Always'' lactose = 1 and glucose = 0 implies Z=Y=A=1 after 2 steps: " + bValue);


            System.out.println("");
            System.out.println("Robustness at several time steps of formula expressing the presence of lactose and absence of glucose:");
            System.out.println("");



            for(int i = 0; i < 40 ; i=i+1){
              double value1 = new DoubleSemanticsVisitor().eval(target_onlyLactose).eval(1, i, sequence);
              System.out.println(i+":"+ " " + value1);
            }

            System.out.println("");
            System.out.println("Robustness at several time steps of formula expressing that lac operon is expressed: ");
            System.out.println("");

            for(int i = 0; i < 40 ; i=i+1){
              double value1 = new DoubleSemanticsVisitor().eval(target_lacExpressed).eval(1, i, sequence);
              System.out.println(i+":"+ " " + value1);
            }

            System.out.println("");
            System.out.println("Robustness at several time steps of formula expressing the presence of lactose and absence of glucose is followed by lac operon expression after 2 steps:");
            System.out.println("");


            for(int i = 0; i < 40 ; i=i+1){
                double value1 = new DoubleSemanticsVisitor().eval(lacExpressionAfterOnlyLactose).eval(1, i, sequence);
                System.out.println(i+":"+ " " + value1);
            }

            double value = new DoubleSemanticsVisitor().eval(lacExpression).eval(1, 0, sequence);

            boolean bValue = (value >0);
            System.out.println("");
            System.out.println("");
            System.out.println("Robustness of the DisTL formula stating that in all time points of the interval");
            System.out.println("if there is lactose and not glucose then lac operon is expressed after two steps: ");
            System.out.println(value);


        }

        catch (RuntimeException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }




    private static List<DataStateUpdate> applyReactions(RandomGenerator rg, DataState state) {
        List<DataStateUpdate> updates = new LinkedList<>();

        // reaction r1 - lac operon duplication
        if(state.get(lac)==1){
            updates.add(new DataStateUpdate(lac,1));
        }
        else{
            updates.add(new DataStateUpdate(lac,0));
        }

        // reaction r2 - repressor gene duplication
        if(state.get(lacI)==1){
            updates.add(new DataStateUpdate(lacI,1));
        }
        else{
            updates.add(new DataStateUpdate(lacI,0));
        }

        // reaction r3 - repressor gene expression
        if(state.get(lacI)==1){
            updates.add(new DataStateUpdate(I,1));
        }
        else{
            updates.add(new DataStateUpdate(I,0));
        }

        // reaction r4 - regulation mediated by lactose
        if(state.get(I)==1 & state.get(lactose)==0){
            updates.add(new DataStateUpdate(IOP,1));
        }
        else{
            updates.add(new DataStateUpdate(IOP,0));
        }

        // reaction r5 - gene encoding signal molecule cAMP duplication
        if(state.get(cya)==1){
            updates.add(new DataStateUpdate(cya,1));
        }
        else{
            updates.add(new DataStateUpdate(cya,0));
        }

        // reaction r6 - gene encoding signal molecule cAMP expression
        if(state.get(cya)==1){
            updates.add(new DataStateUpdate(cAMP,1));
        }
        else{
            updates.add(new DataStateUpdate(cAMP,0));
        }

        // reaction r7 - gene encoding protein CAP duplication
        if(state.get(crp)==1){
            updates.add(new DataStateUpdate(crp,1));
        }
        else{
            updates.add(new DataStateUpdate(crp,0));
        }

        // reaction r8 - gene encoding protein CAP expression
        if(state.get(crp)==1){
            updates.add(new DataStateUpdate(CAP,1));
        }
        else{
            updates.add(new DataStateUpdate(CAP,0));
        }

        // reaction r9 - regulation mediated by lactose
        if(state.get(cAMP)==1 & state.get(CAP)==1 & state.get(glucose)==0){
            //System.out.println("yes");
            updates.add(new DataStateUpdate(cAMPCAP,1));
        }
        else{
            //System.out.println("no + cAMP= "+ state.get(cAMP)+ " CAP = " + state.get(CAP)+ " glu = " + state.get(glucose));
            updates.add(new DataStateUpdate(cAMPCAP,0));
        }

        // reaction r10 - lac operon expression
        if(state.get(cAMPCAP)==1 & state.get(lac)==1 & state.get(IOP)==0){
            updates.add(new DataStateUpdate(Z,1));
            updates.add(new DataStateUpdate(Y,1));
            updates.add(new DataStateUpdate(A,1));
        }
        else{
            updates.add(new DataStateUpdate(Z,0));
            updates.add(new DataStateUpdate(Y,0));
            updates.add(new DataStateUpdate(A,0));
        }


        updates.add(new DataStateUpdate(lactose,state.get(lactose_N)));
        updates.add(new DataStateUpdate(glucose,state.get(glucose_N)));

        return updates;
    }


     /*
     Method <code>getInitialState</code> assigns the initial value to all 14 variables.
     To reproduce the same results as in Corolli et al. we start with all variables having value 0.
     Technically, the metrod returns an instance of <code>DataState</code>
      */
    private static DataState getInitialState() {
        Map<Integer, Double> initialValues = new HashMap<>();
        initialValues.put(lac, 1.0); //
        initialValues.put(Z, 0.0);
        initialValues.put(Y, 0.0);
        initialValues.put(A, 0.0);
        initialValues.put(lacI, 1.0); //
        initialValues.put(I, 1.0); //
        initialValues.put(IOP, 0.0);
        initialValues.put(cya, 1.0); //
        initialValues.put(cAMP, 1.0); //
        initialValues.put(crp, 1.0); //
        initialValues.put(CAP, 1.0); //
        initialValues.put(cAMPCAP, 0.0);
        initialValues.put(lactose, 0.0);
        initialValues.put(glucose, 0.0);
        initialValues.put(lactose_N, 0.0);
        initialValues.put(glucose_N, 0.0);
        return new DataState(NUMBER_OF_VARIABLES, i -> initialValues.getOrDefault(i, Double.NaN));
    }

    /*
    Method <code>getDiracLacExpressed</code> returns the updates needed to set Z = Y = A = 1, which
    characterizes a data state in which the lac operon is expressed
     */

    public static List<DataStateUpdate> getDiracLacExpressed(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Z, 1.0));
        updates.add(new DataStateUpdate(Y, 1.0));
        updates.add(new DataStateUpdate(A, 1.0));
        return updates;
    }

    /*
    Method <code>getDiracOnlyLactose</code> returns the updates needed to set lactose = 1 and glucose = 0,
    which characterise the data state in which the context has provided lactose and not glucose
     */
    private static List<DataStateUpdate> getDiracOnlyLactose(RandomGenerator rg, DataState state) {
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(lactose, 1.0));
        updates.add(new DataStateUpdate(glucose, 0.0));
        return updates;
    }





    /*
    Method <code>getController()</code> returns an instance of <code>ParallelController()</code>,
    which implements two <code>Controller()</code> running in parallel.
    The two <code>Controller()</code> running in parallel are defined in method <code>getControllerRegistry()</code>, and
    are:
    - the <code>Controller()</code> named "DefaultCondition", which implements the "default context"
    used in [Corolli et al.], which mimics the real biological system in which the genomic elements plus their encoded
    proteins are normally present
    - the <code>Controller()</code> named <Glucose5>, which is a <code>Controller()</code> that
    produces the following sequence of 40 contexts:
    {glucose}; {glucose}; {glucose}; {glucose}; {glucose};
    {lactose, glucose}; {lactose, glucose}; {lactose, glucose}; {lactose, glucose}; {lactose,,glucose};
    {lactose}; {lactose}; {lactose}; {lactose}; {lactose};
    { } ; { } ; { } ; { } ; { } ;
    {lactose}; {lactose}; {lactose}; {lactose}; {lactose};
    {lactose, glucose}; {lactose, glucose}; {lactose, glucose}; {lactose, glucose}; {lactose,,glucose};
    {lactose}; {lactose}; {lactose}; {lactose}; {lactose};
    {glucose}; {glucose}; {glucose}; {glucose}; {glucose};
     */
    public static Controller getController() {
        ControllerRegistry registry = getControllerRegistry();
        return new ParallelController(registry.reference("DefaultCondition"), registry.reference("Glucose5"));
    }

    public static ControllerRegistry getControllerRegistry() {
        ControllerRegistry registry = new ControllerRegistry();
        registry.set("DefaultCondition",
                Controller.doAction(
                        (rg,ds)->List.of(
                                new DataStateUpdate(lac,1.0),
                                new DataStateUpdate(lacI,1.0),
                                new DataStateUpdate(I,1.0),
                                new DataStateUpdate(cya,1.0),
                                new DataStateUpdate(cAMP,1.0),
                                new DataStateUpdate(crp,1.0),
                                new DataStateUpdate(CAP,1.0)),
                        registry.reference("DefaultCondition")
                )
        );
        registry.set("Start",
                Controller.doAction((rg,ds)->List.of(
                                ),
                        registry.reference("Glucose5")
                )
        );
        registry.set("Glucose5",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,0.0)),
                        registry.reference("Glucose4")
                )
        );
        registry.set("Glucose4",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,0.0)),
                        registry.reference("Glucose3")
                )
        );
        registry.set("Glucose3",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,0.0)),
                        registry.reference("Glucose2")
                )
        );
        registry.set("Glucose2",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,0.0)),
                        registry.reference("Glucose1")
                )
        );
        registry.set("Glucose1",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,0.0)),
                        registry.reference("GlucoseLactose5")
                )
        );
        registry.set("GlucoseLactose5",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("GlucoseLactose4")
                )
        );
        registry.set("GlucoseLactose4",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("GlucoseLactose3")
                )
        );
        registry.set("GlucoseLactose3",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("GlucoseLactose2")
                )
        );
        registry.set("GlucoseLactose2",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("GlucoseLactose1")
                )
        );
        registry.set("GlucoseLactose1",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("Lactose5")
                )
        );
        registry.set("Lactose5",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("Lactose4")
                )
        );
        registry.set("Lactose4",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("Lactose3")
                )
        );
        registry.set("Lactose3",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("Lactose2")
                )
        );
        registry.set("Lactose2",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("Lactose1")
                )
        );
        registry.set("Lactose1",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("Tick5")
                )
        );
        registry.set("Tick5",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,0.0)),
                        registry.reference("Tick4")
                )
        );
        registry.set("Tick4",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,0.0)),
                        registry.reference("Tick3")
                )
        );
        registry.set("Tick3",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,0.0)),
                        registry.reference("Tick2")
                )
        );
        registry.set("Tick2",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,0.0)),
                        registry.reference("Tick1")
                )
        );
        registry.set("Tick1",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,0.0)),
                        registry.reference("Lact5")
                )
        );


        registry.set("Lact5",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("Lact4")
                )
        );
        registry.set("Lact4",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("Lact3")
                )
        );
        registry.set("Lact3",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("Lact2")
                )
        );
        registry.set("Lact2",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("Lact1")
                )
        );
        registry.set("Lact1",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("GlucLact5")
                )
        );
        registry.set("GlucLact5",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("GlucLact4")
                )
        );
        registry.set("GlucLact4",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("GlucLact3")
                )
        );
        registry.set("GlucLact3",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("GlucLact2")
                )
        );
        registry.set("GlucLact2",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("GlucLact1")
                )
        );
        registry.set("GlucLact1",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("Lac5")
                )
        );
        registry.set("Lac5",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("Lac4")
                )
        );
        registry.set("Lac4",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("Lac3")
                )
        );
        registry.set("Lac3",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("Lac2")
                )
        );
        registry.set("Lac2",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("Lac1")
                )
        );
        registry.set("Lac1",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,1.0)),
                        registry.reference("Glu5")
                )
        );
        registry.set("Glu5",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,0.0)),
                        registry.reference("Glu4")
                )
        );
        registry.set("Glu4",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,0.0)
                        ),
                        registry.reference("Glu3")
                )
        );
        registry.set("Glu3",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,0.0)),
                        registry.reference("Glu2")
                )
        );
        registry.set("Glu2",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,0.0)),
                        registry.reference("Glu1")
                )
        );
        registry.set("Glu1",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,1.0),
                                new DataStateUpdate(lactose_N,0.0)),
                        registry.reference("Glucose5")
                )
        );
        registry.set("Tick",
                Controller.doAction((rg,ds)->List.of(
                                new DataStateUpdate(glucose_N,0.0),
                                new DataStateUpdate(lactose_N,0.0)),
                        registry.reference("Tick")
                )
        );

        return registry;
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
        /*System.out.println("Avg over all steps of the average values taken in the single step by the variables:");
        for (int j = 0; j < tot.length - 1; j++) {
            System.out.printf("%f   ", tot[j] / (rightbound - leftbound));
        }
        System.out.printf("%f\n", tot[tot.length - 1] / (rightbound - leftbound));
        System.out.println("");
        System.out.println("");
        */
        return tot;


    }






}
