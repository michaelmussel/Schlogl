/*
 * STARK: Software Tool for the Analysis of Robustness in the unKnown environment
 *
 *              Copyright (C) 2023.
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

package mechanicallungventilator;

import stark.*;
import stark.controller.Controller;
import stark.controller.ControllerRegistry;
import stark.controller.ParallelController;
import stark.distance.*;
import stark.ds.*;
import stark.perturbation.*;
import stark.robtl.*;
import stark.distl.*;
import org.apache.commons.math3.random.RandomGenerator;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.util.*;

public class Main {

    public final static String[] VARIABLES =
            new String[]{"p_GB_pressure", "s_GB_pressure", "p_PS_ins_pressure", "s_PS_ins_pressure",
                    "p_PS_exp_pressure", "s_PS_exp_pressure","p_OS", "s_OS", "p_Fl1_flow", "s_Fl1_flow",
                    "p_Fl2_flow", "s_Fl2_flow",  "p_temp", "s_temp","p_power_source", "s_power_source",
                    "p_fan", "s_fan", "s_battery_level", "a_IN_valve", "a_OUT_valve", "a_LED",
                    "RR_ms", "peak_P_insp", "V_tidal", "V_E", "t_RM_remaining", "Status", "IE_ms",
                    "b_powerOn", "conn_power_source", "conn_air_supply", "conn_patient", "conn_breathing",
                    "comm_sens_valves_ok", "comm_memory", "comm_cont_gui_ok", "init_succ", "conn_failToPowerOn",
                    "sys_out_of_service", "selfTest_fail", "gui_req_res_ven", "power_switch_ok",
                    "no_leaks_breathing_circuit", "out_valve_ok", "alarms_ok", "nr_of_retries",
                    "gui_req_change_mode_PCV", "gui_req_change_mode_PSV", "gui_req_stop_vent", "timer_PCV_insp",
                    "timer_PSV_insp", "timer_PCV_exp", "drop_PAW", "gui_req_IP", "gui_req_RM", "gui_req_EP",
                    "timer_IP", "timer_EP", "timer_RM", "timer_triggerDelay", "min_exp_time_psv", "b_powerOff",
                    "gui_param_psv_ok", "phase", "phase_changed", "IE_toolow_counter", "timer_insp",
                    "timer_exp", "cycle_done", "fs", "previous_PAW", "peak_flow", "nr_of_retries_p", "timer_PSV_exp",
                    "V_tidal_prev", "rr_pcv", "p_insp_pcv", "ie_pcv", "alarm_counter", "counter_cycles", "switch_ready",
                    "req_counter", "on_counter", "test_counter", "test_per", "p_drop_PAW", "p_peak_flow", "psv_param_counter"
            };
    //The order above does not matter for the order below. So it's okay that it's not the same anymore
    public final static int PRM = 20;
    public final static int RM_TIME = 10;
    public final static int RR_PCV = 12;
    public final static double IE_PCV = 0.5;
    public final static int P_INSP_PCV = 15;
    public final static int ITS_PCV = 3;
    public final static int P_INSP_PSV = 15;
    public final static int ITS_PSV = 3;
    public final static int ETS = 30;
    public final static int T_APNEALAG = 30;
    public final static int RR_AP = 12;
    public final static int P_INSP_AP = 12;
    public final static double IE_AP = 0.5;
    public final static double MAX_P_INSP = 40.0;
    public final static int MIN_P_INSP = 50; //note this means 50% of P_insp
    public final static int MAX_V_TIDAL_EXP = 990;
    public final static int MIN_V_TIDAL_EXP = 10;
    public final static int MAX_V_E = 80;
    public final static int MIN_V_E = 2;
    public final static int MIN_RR = 4;
    public final static int MAX_RR = 50;
    public final static int MIN_PEEP = 5;
    public final static int MAX_PEEP = 15;
    public final static int MAX_T_IP = 40;
    public final static int MAX_T_EP = 60;
    public final static double TRIGGER_WINDOW_DELAY = 0.7;
    public final static int MAX_INSP_TIME_PSV = 7;
    public final static int PM_A_GB_PRESSURE = 4500;
    public final static int PM_A_GB_FiO2 = 50;
    public final static int PM_A_PEEP_VALVE = 8;
    public final static int HIGH_FLOW = 60;
    public final static int H= 450;
    public final static int TIMER_INIT = 5;
    public final static int SIZE_DISTL = 50;

    private static final int p_GB_pressure = 0;//variableRegistry.getVariable("p_speed");
    private static final int s_GB_pressure = 1;//variableRegistry.getVariable("s_speed");
    private static final int p_PS_ins_pressure = 2;// variableRegistry.getVariable("p_distance");
    private static final int s_PS_ins_pressure = 3;// variableRegistry.getVariable("s_distance");
    private static final int p_PS_exp_pressure = 4;//variableRegistry.getVariable("accel");
    private static final int s_PS_exp_pressure = 5;//variableRegistry.getVariable("timer");
    private static final int p_OS = 6;//variableRegistry.getVariable("warning");
    private static final int s_OS = 7;//variableRegistry.getVariable("braking_distance");
    private static final int p_Fl1_flow = 8; //variableRegistry.getVariable("required_distance");
    private static final int s_Fl1_flow = 9;//variableRegistry.getVariable("safety_gap");
    private static final int p_Fl2_flow = 10;

    private static final int s_Fl2_flow = 11;//variableRegistry.getVariable("p_speed");
    private static final int p_temp = 12;//variableRegistry.getVariable("s_speed");
    private static final int s_temp = 13;// variableRegistry.getVariable("p_distance");
    private static final int p_power_source = 14;// variableRegistry.getVariable("s_distance");
    private static final int s_power_source = 15;// variableRegistry.getVariable("p_distance");
    private static final int p_fan = 16;
    private static final int s_fan = 17;//variableRegistry.getVariable("accel");
    private static final int s_battery_level = 18;//variableRegistry.getVariable("timer");
    private static final int a_IN_valve = 19;//variableRegistry.getVariable("warning");
    private static final int a_OUT_valve = 20;//variableRegistry.getVariable("braking_distance");
    private static final int a_LED = 21; //variableRegistry.getVariable("required_distance");
    private static final int RR_ms = 22;
    private static final int peak_P_insp = 23;//variableRegistry.getVariable("safety_gap");
    private static final int V_tidal = 24;
    private static final int V_E = 25;
    private static final int t_RM_remaining = 26;
    private static final int Status = 27;
    private static final int IE_ms = 28;
    private static final int b_powerOn = 29;
    private static final int conn_power_source = 30;
    private static final int conn_air_supply = 31;
    private static final int conn_patient = 32;
    private static final int conn_breathing = 33;
    private static final int comm_sens_valves_ok = 34;
    private static final int comm_memory = 35;
    private static final int comm_cont_gui_ok = 36;
    private static final int init_succ = 37;
    private static final int conn_failToPowerOn = 38;
    private static final int sys_out_of_service = 39;
    private static final int selfTest_fail = 40;
    private static final int gui_req_res_ven = 41;
    private static final int power_switch_ok = 42;
    private static final int no_leaks_breathing_circuit = 43;
    private static final int out_valve_ok = 44;
    private static final int alarms_ok = 45;
    private static final int nr_of_retries = 46;
    private static final int gui_req_change_mode_PCV = 47;
    private static final int gui_req_change_mode_PSV = 48;
    private static final int gui_req_stop_vent = 49;
    private static final int timer_PCV_insp = 50;
    private static final int timer_PSV_insp = 51;
    private static final int timer_PCV_exp = 52;
    private static final int drop_PAW = 53;
    private static final int gui_req_IP = 54;
    private static final int gui_req_RM = 55;
    private static final int gui_req_EP = 56;
    private static final int timer_IP = 57;
    private static final int timer_EP = 58;
    private static final int timer_RM = 59;
    private static final int timer_triggerDelay = 60;
    private static final int min_exp_time_psv = 61;
    private static final int b_powerOff = 62;
    private static final int gui_param_psv_ok = 63;
    private static final int phase = 64;
    private static final int phase_changed = 65;
    private static final int IE_toolow_counter = 67;
    private static final int timer_insp = 68;
    private static final int timer_exp = 69;
    private static final int cycle_done = 70;
    private static final int fs = 71;
    private static final int previous_PAW = 72;
    private static final int peak_flow = 73;
    private static final int nr_of_retries_p = 74;
    private static final int timer_PSV_exp = 66;
    private static final int V_tidal_prev = 75;
    private static final int rr_pcv = 76;
    private static final int p_insp_pcv = 77;
    private static final int ie_pcv = 78;
    private static final int ind_var = 79;
    private static final int alarm_counter = 80;
    private static final int counter_cycles = 81;
    private static final int switch_ready = 82;
    private static final int req_counter = 83;
    private static final int on_counter = 84;
    private static final int test_counter = 85;
    private static final int test_per = 86;
    private static final int p_drop_PAW = 87;
    private static final int p_peak_flow = 88;
    private static final int psv_param_counter = 89;

    private static final int NUMBER_OF_VARIABLES = 90;



    public static void main(String[] args) throws IOException {
        try {
            RandomGenerator rand = new DefaultRandomGenerator();
            int size = 100;
            Controller controller_MLV = getController_MLV();
            Controller controller_alarm = getController_alarm();
            Controller controller_switch = getController_switch();
            DataState state = getInitialState();
            ControlledSystem system = new ControlledSystem(new ParallelController(new ParallelController(controller_MLV, controller_alarm), controller_switch),
                    (rg, ds) -> ds.apply(getEnvironmentUpdates(rg, ds)), state);
            EvolutionSequence sequence = new EvolutionSequence(rand, rg -> system, size);

            DistanceExpression sav_6 = new AtomicDistanceExpressionLeq(Main::rho_sav_6);
            DistanceExpression sav_6_dist = new MinIntervalDistanceExpression(sav_6, 0, 2);
            DistanceExpression sav_6_penal_no_alarm = new AtomicDistanceExpressionLeq(Main::rho_sav_6_penal_no_alarm);
            DistanceExpression sav_6_dist_penal_no_alarm =  new MaxIntervalDistanceExpression(sav_6_penal_no_alarm, 0, 2);
            DistanceExpression sav_6_led = new AtomicDistanceExpressionLeq(Main::rho_sav_6_alarm);
            DistanceExpression sav_6_dist_led = new MaxIntervalDistanceExpression(sav_6_led, 0, 2);
            DistanceExpression sav_16 = new AtomicDistanceExpressionLeq(Main::rho_sav_16);
            DistanceExpression sav_16_dist = new MinIntervalDistanceExpression(sav_16, 0, 3);
            DistanceExpression sav_16_penal_no_alarm = new AtomicDistanceExpressionLeq(Main::rho_sav_16_penal_no_alarm);
            DistanceExpression sav_16_dist_penal_no_alarm = new MaxIntervalDistanceExpression(sav_16_penal_no_alarm, 0, 2);
            DistanceExpression basic_test = new AtomicDistanceExpressionLeq(Main::rho_basic_test);
            DistanceExpression cont_15 = new AtomicDistanceExpressionLeq(Main::rho_cont_15);
            DistanceExpression cont_15_dist = new MinIntervalDistanceExpression(cont_15, 13, 15   );
            DistanceExpression cont_15_penal_no_alarm = new AtomicDistanceExpressionLeq(Main::rho_cont_15_penal_no_alarm);
            DistanceExpression cont_15_dist_penal_no_alarm = new MaxIntervalDistanceExpression(cont_15_penal_no_alarm, 13, 14   );

            double eta_sav_6 = 0.1;
            double eta_sav_16 = 0.1;

            RobustnessFormula Phi_sav_6_t3_2 = new AtomicRobustnessFormula(get_Sav_6Perturbation_t3_2(),
                    sav_6_dist_penal_no_alarm,
                    RelationOperator.GREATER_OR_EQUAL_THAN,
                    1.0
            );

            RobustnessFormula Phi_sav_6 = new AlwaysRobustnessFormula(
                    new AtomicRobustnessFormula(
                            get_Sav_6Perturbation_t3_2(),
                            sav_6_dist,
                            RelationOperator.LESS_OR_EQUAL_THAN,
                            eta_sav_6
                    ),
                    0,
                    H
            );
            RobustnessFormula Phi_sav_6_alarm = new AlwaysRobustnessFormula(
                    new AtomicRobustnessFormula(
                            get_Sav_6Perturbation_t3_2(),
                            sav_6_dist_led,
                            RelationOperator.GREATER_OR_EQUAL_THAN,
                            1
                    ),
                    0,
                    0
            );


            RobustnessFormula Phi_sav_16_per = new AtomicRobustnessFormula(
                    get_Sav_16Perturbation(),
                    sav_16_dist_penal_no_alarm,
                    RelationOperator.GREATER_OR_EQUAL_THAN,
                    1.0
            );

            //TODO: ensure that sav_16 is according to Excel formula again
            RobustnessFormula Phi_sav_16 = new AlwaysRobustnessFormula(
                    new AtomicRobustnessFormula(
                            get_Sav_16Perturbation(),
                            sav_16_dist,
                            RelationOperator.LESS_OR_EQUAL_THAN,
                            eta_sav_16
                    ),
                    0,
                    9
            );

            RobustnessFormula Phi_cont_15 =
                    new AtomicRobustnessFormula(
                            get_cont_15Perturbation(),
                            cont_15_dist,
                            RelationOperator.LESS_OR_EQUAL_THAN,
                            0.1);

            RobustnessFormula Phi_cont_15_per = new AtomicRobustnessFormula(
                    get_cont_15Perturbation(),
                    cont_15_dist_penal_no_alarm,
                    RelationOperator.GREATER_OR_EQUAL_THAN,
                    1.0
            );


            RobustnessFormula Phi_basic_test = new AtomicRobustnessFormula(
                    get_test_Perturbation(),
                    basic_test,
                    RelationOperator.GREATER_OR_EQUAL_THAN,
                    1.0
            );

            /*
            USING THE SIMULATOR
             */

            ArrayList<String> L = new ArrayList<>();
            //L.add("init_succ");
            //L.add("conn_patient");
            L.add("fs");
            L.add("a_LED");
            //L.add("timer_PCV_insp");
            //L.add("timer_insp");
            L.add("a_in");
            L.add("a_out");
            L.add("Status");
            L.add("phase");
            L.add("phase_changed");
            L.add("PAW/s_PS_insp");
            L.add("p_PS_insp");
            L.add("s_PS_exp");
            L.add("p_PS_exp");
            L.add("timer_insp");
            L.add("timer_pcv_insp");
            L.add("timer_pcv_exp");
            L.add("timer_exp");
            L.add("timer_psv_insp");
            L.add("timer_psv_exp");
            L.add("drop_PAW");
            L.add("Timer_trigger_Delay");
            L.add("cycle_done");
            L.add("counter_cycles");
            L.add("V_tidal");
            L.add("V_tidal_prev");
            L.add("V_E");
            L.add("RR_ms");
            //L.add("timer_insp");
            L.add("s_Fl1_flow");
            L.add("p_Fl1_flow");
            L.add("peak_flow");
            L.add("s_Fl2_flow");
            L.add("p_Fl2_flow");
            L.add("switch_ready");
            L.add("b_powerOff");
            L.add("b_powerOn");
            L.add("on_counter");
            L.add("conn_failToPowerOn");
            L.add("comm_sens_valves_ok");
            L.add("conn_power_source");
            L.add("comm_memory");
            L.add("comm_cont_gui_ok");
            L.add("conn_patient");
            L.add("gui_req_change_mode_PCV");

            ArrayList<DataStateExpression> F = new ArrayList<>();
            //F.add(ds->ds.get(init_succ));
            //F.add(ds->ds.get(conn_patient));
            F.add(ds->ds.get(fs));
            F.add(ds->ds.get(a_LED));
            //F.add(ds->ds.get(timer_PCV_insp));
            //F.add(ds->ds.get(timer_insp));
            F.add(ds->ds.get(a_IN_valve));
            F.add(ds->ds.get(a_OUT_valve));
            F.add(ds->ds.get(Status));
            F.add(ds->ds.get(phase));
            F.add(ds -> ds.get(phase_changed));
            F.add(ds->ds.get(s_PS_ins_pressure));
            F.add(ds->ds.get(p_PS_ins_pressure));
            F.add(ds->ds.get(s_PS_exp_pressure));
            F.add(ds->ds.get(p_PS_exp_pressure));
            //F.add(ds->ds.get(timer_PCV_exp));
            F.add(ds->ds.get(timer_insp));
            F.add(ds-> ds.get(timer_PCV_insp));
            F.add(ds->ds.get(timer_PCV_exp));
            F.add(ds->ds.get(timer_exp));
            F.add(ds->ds.get(timer_PSV_insp));
            F.add(ds->ds.get(timer_PSV_exp));
            F.add(ds->ds.get(drop_PAW));
            F.add(ds->ds.get(timer_triggerDelay));
            F.add(ds->ds.get(cycle_done));
            F.add(ds->ds.get(counter_cycles));
            //F.add(ds->ds.get(comm_sens_valves_ok));
            //F.add(ds->ds.get(comm_cont_gui_ok));
            F.add(ds->ds.get(V_tidal));
            F.add(ds->ds.get(V_tidal_prev));
            F.add(ds->ds.get(V_E));
            F.add(ds->ds.get(RR_ms));
            //F.add(ds->ds.get(timer_insp));
            F.add(ds -> ds.get(s_Fl1_flow));
            F.add(ds -> ds.get(p_Fl1_flow));
            F.add(ds -> ds.get(peak_flow));
            F.add(ds -> ds.get(s_Fl2_flow));
            F.add(ds -> ds.get(p_Fl2_flow));
            F.add(ds -> ds.get(switch_ready));
            F.add(ds -> ds.get(b_powerOff));
            F.add(ds -> ds.get(b_powerOn));
            F.add(ds -> ds.get(on_counter));
            F.add(ds -> ds.get(conn_failToPowerOn));
            F.add(ds -> ds.get(comm_sens_valves_ok));
            F.add(ds -> ds.get(conn_power_source));
            F.add(ds -> ds.get(comm_memory));
            F.add(ds -> ds.get(comm_cont_gui_ok));
            F.add(ds -> ds.get(conn_patient));
            F.add(ds -> ds.get(gui_req_change_mode_PCV));

            printLData(rand,L,F,system,150,1);
            System.out.println("Here the perturbed system with perturbation get_Sav6_t2");
            printLDataP(rand, L, F, get_Sav_16Perturbation_sim(), system, 150, 1);

            //Trying to simulate a perturbed sequence
            EvolutionSequence sequence_pert = sequence.apply(get_Sav_16Perturbation_sim(),0, 100);

            System.out.println("Starting tests on perturbed behaiour");
            Util.writeToCSV("./testPerturbed.csv", Util.evalDistanceExpression(sequence, sequence_pert, 0, 100, sav_6_dist_penal_no_alarm));

            double[][] val_test_t3_2 = new double[15][1];
            for(int i = 0; i<15; i++) {
                int step = i*10;
                TruthValues value1_t3_2 = new ThreeValuedSemanticsVisitor(rand,50,1.96).eval(Phi_sav_6_t3_2).eval(60, step, sequence);
                System.out.println("Phi_sav_6_t3_2 evaluation at step "+step+": " + value1_t3_2);
                if (value1_t3_2 == TruthValues.TRUE) {
                    val_test_t3_2[i][0] = 1;
                } else {
                    if (value1_t3_2 == TruthValues.UNKNOWN) {
                        val_test_t3_2[i][0] = 0;
                    } else {
                        val_test_t3_2[i][0] = -1;
                    }
                }
            }

            System.out.println();
            double[][] val_test = new double[15][1];
            for(int i = 0; i<15; i++) {
                int step = i*10;
                TruthValues value1 = new ThreeValuedSemanticsVisitor(rand,50,1.96).eval(Phi_sav_6).eval(60, step, sequence);
                System.out.println("Phi_sav_6 evaluation at step "+step+": " + value1);
                if (value1 == TruthValues.TRUE) {
                    val_test[i][0] = 1;
                } else {
                    if (value1 == TruthValues.UNKNOWN) {
                        val_test[i][0] = 0;
                    } else {
                        val_test[i][0] = -1;
                    }
                }
            }

            System.out.println();

            double[][] val_test_alarm  = new double[15][1];
            for(int i = 0; i<15; i++) {
                int step = i*5;
                TruthValues value1_alarm  = new ThreeValuedSemanticsVisitor(rand,50,1.96).eval(Phi_sav_6_alarm).eval(60, step, sequence);
                System.out.println("Phi_sav_6_alarm evaluation at step "+step+": " + value1_alarm );
                if (value1_alarm  == TruthValues.TRUE) {
                    val_test_alarm [i][0] = 1;
                } else {
                    if (value1_alarm  == TruthValues.UNKNOWN) {
                        val_test_alarm [i][0] = 0;
                    } else {
                        val_test_alarm [i][0] = -1;
                    }
                }
            }

            System.out.println();

            double[][] val_sav_16 = new double[10][1];
            for(int i = 0; i<10; i++) {
                int step = i*30;
                TruthValues value_sav_16 = new ThreeValuedSemanticsVisitor(rand,50,1.96).eval(Phi_sav_16).eval(60, step, sequence);
                System.out.println("Phi_sav_16 evaluation at step "+step+": " + value_sav_16);
                if (value_sav_16 == TruthValues.TRUE) {
                    val_sav_16[i][0] = 1;
                } else {
                    if (value_sav_16 == TruthValues.UNKNOWN) {
                        val_sav_16[i][0] = 0;
                    } else {
                        val_sav_16[i][0] = -1;
                    }
                }
            }
            System.out.println();

            double[][] val_sav_16_per = new double[10][1];
            for(int i = 0; i<10; i++) {
                int step = i*30;
                TruthValues value_sav_16_per = new ThreeValuedSemanticsVisitor(rand,50,1.96).eval(Phi_sav_16_per).eval(60, step, sequence);
                System.out.println("Phi_sav_16_per evaluation at step "+step+": " + value_sav_16_per);
                if (value_sav_16_per == TruthValues.TRUE) {
                    val_sav_16_per[i][0] = 1;
                } else {
                    if (value_sav_16_per == TruthValues.UNKNOWN) {
                        val_sav_16_per[i][0] = 0;
                    } else {
                        val_sav_16_per[i][0] = -1;
                    }
                }
            }

            System.out.println();

            double[][] val_cont_15 = new double[10][1];
            for(int i = 0; i<10; i++) {
                int step = i*1;
                TruthValues value_cont_15 = new ThreeValuedSemanticsVisitor(rand,50,1.96).eval(Phi_cont_15).eval(60, step, sequence);
                System.out.println("Phi_cont_15 evaluation at step "+step+": " + value_cont_15);
                if (value_cont_15 == TruthValues.TRUE) {
                    val_cont_15[i][0] = 1;
                } else {
                    if (value_cont_15 == TruthValues.UNKNOWN) {
                        val_cont_15[i][0] = 0;
                    } else {
                        val_cont_15[i][0] = -1;
                    }
                }
            }

            System.out.println();
            double[][] val_cont_15_per = new double[10][1];
            for(int i = 0; i<10; i++) {
                int step = i;
                TruthValues value_cont_15_per = new ThreeValuedSemanticsVisitor(rand,50,1.96).eval(Phi_cont_15_per).eval(60, step, sequence);
                System.out.println("Phi_cont_15_per evaluation at step "+step+": " + value_cont_15_per);
                if (value_cont_15_per == TruthValues.TRUE) {
                    val_cont_15_per[i][0] = 1;
                } else {
                    if (value_cont_15_per == TruthValues.UNKNOWN) {
                        val_cont_15_per[i][0] = 0;
                    } else {
                        val_cont_15_per[i][0] = -1;
                    }
                }
            }

            System.out.println();

            double[][] val_basic_test = new double[10][1];
            for(int i = 0; i<10; i++) {
                int step = i*30;
                TruthValues value_basic_test = new ThreeValuedSemanticsVisitor(rand,50,1.96).eval(Phi_basic_test).eval(60, step, sequence);
                System.out.println("Phi_basic_test evaluation at step "+step+": " + value_basic_test);
                if (value_basic_test == TruthValues.TRUE) {
                    val_basic_test[i][0] = 1;
                } else {
                    if (value_basic_test == TruthValues.UNKNOWN) {
                        val_basic_test[i][0] = 0;
                    } else {
                        val_basic_test[i][0] = -1;
                    }
                }
            }


            //ANALYSIS WITH DisTL
            DataStateFunction mu_cont19 = (rg, ds) -> ds.apply(getDiracCont19(rg, ds));
            DataStateFunction mu_cont19_t2_p2 = (rg, ds) -> ds.apply(getDiracCont19_t2_p2(rg, ds));
            DataStateFunction mu_test0 = (rg, ds) -> ds.apply(getDiracTest0(rg, ds));
            DataStateFunction mu_test1 = (rg, ds) -> ds.apply(getDiracTest1(rg, ds));
            DataStateFunction mu_test2 = (rg, ds) -> ds.apply(getDiracTest2(rg, ds));
            DataStateFunction mu_test3 = (rg, ds) -> ds.apply(getDiracTest3(rg, ds));
            DataStateFunction mu_test4 = (rg, ds) -> ds.apply(getDiracTest4(rg, ds));
            DataStateFunction mu_test5 = (rg, ds) -> ds.apply(getDiracTest5(rg, ds));
            DataStateFunction mu_test6 = (rg, ds) -> ds.apply(getDiracTest6(rg, ds));
            DataStateFunction mu_test7 = (rg, ds) -> ds.apply(getDiracTest7(rg, ds));

            DataStateFunction mu_cont36_3 = (rg, ds) -> ds.apply(getDiracCont36_3(rg, ds));
            DataStateFunction mu_cont36_3_part1 = (rg, ds) -> ds.apply(getDiracCont36_3_part1(rg, ds));
            DataStateFunction mu_cont1_3 = (rg, ds) -> ds.apply(getDiracCont1_3(rg, ds));
            DataStateFunction mu_cont1_3_t2_p1 = (rg, ds) -> ds.apply(getDiracCont1_3_t2_p1(rg, ds));
            DataStateFunction mu_cont1_3_t2_p2 = (rg, ds) -> ds.apply(getDiracCont1_3_t2_p2(rg, ds));
            DataStateFunction mu_cont38_part1 = (rg, ds) -> ds.apply(getDiracCont38_part1(rg, ds));
            DataStateFunction mu_cont38_part2 = (rg, ds) -> ds.apply(getDiracCont1_3(rg, ds));
            DataStateFunction mu_cont38_part3 = (rg, ds) -> ds.apply(getDiracCont38_part3(rg, ds));
            DataStateFunction mu_cont38_t2_p1 = (rg, ds) -> ds.apply(getDiracCont38_t2_p1(rg, ds));
            DataStateFunction mu_cont38_t2_p2 = (rg, ds) -> ds.apply(getDiracCont38_t2_p2(rg, ds));
            DataStateFunction mu_cont38_t2_p3 = (rg, ds) -> ds.apply(getDiracCont38_t2_p3(rg, ds));
            DataStateFunction mu_sav22 = (rg, ds) -> ds.apply(getDiracSav22(rg, ds));
            DataStateFunction mu_sav22_2 = (rg, ds) -> ds.apply(getDiracSav22_2(rg, ds));
            DataStateFunction mu_sav22_p2 = (rg, ds) -> ds.apply(getDiracSav22_p2(rg, ds));
            DataStateFunction mu_sav1_al23 = (rg, ds) -> ds.apply(getDiracSav1_al23(rg, ds));
            DataStateFunction mu_sav1_al24 = (rg, ds) -> ds.apply(getDiracSav1_al24(rg, ds));
            DataStateFunction mu_sav1_al23_p = (rg, ds) -> ds.apply(getDiracSav1_al23_p(rg, ds));
            DataStateFunction mu_sav1_al24_p = (rg, ds) -> ds.apply(getDiracSav1_al24_p(rg, ds));
            DataStateFunction mu_sav17_1 = (rg, ds) -> ds.apply(getDiracSav17_1(rg, ds));
            DataStateFunction mu_cont5_part1 = (rg, ds) -> ds.apply(getDiracCont5_part1(rg, ds));
            DataStateFunction mu_cont5_part2 = (rg, ds) -> ds.apply(getDiracCont5_part2(rg, ds));
            DataStateFunction mu_cont6_t1 = (rg, ds) -> ds.apply(getDiracCont6_t1(rg, ds));
            DataStateFunction mu_cont6_t2_part3 = (rg, ds) -> ds.apply(getDiracCont6_t2_part3(rg, ds));
            DataStateFunction mu_cont8_1 = (rg, ds) -> ds.apply(getDiracCont8_1(rg, ds));
            DataStateFunction mu_cont8_2= (rg, ds) -> ds.apply(getDiracCont8_2(rg, ds));
            DataStateFunction mu_cont10_1 = (rg, ds) -> ds.apply(getDiracCont10_1(rg, ds));
            DataStateFunction mu_cont3_1 = (rg, ds) -> ds.apply(getDiracCont3_1(rg, ds));
            DataStateFunction mu_cont3_2 = (rg, ds) -> ds.apply(getDiracCont3_2(rg, ds));
            DataStateFunction mu_cont4_part1_1 = (rg, ds) -> ds.apply(getDiracCont4_part1_1(rg, ds));
            DataStateFunction mu_cont4_part1_2 = (rg, ds) -> ds.apply(getDiracCont4_part1_2(rg, ds));
            DataStateFunction mu_cont4_part2_1 = (rg, ds) -> ds.apply(getDiracCont4_part2_1(rg, ds));
            DataStateFunction mu_cont21_part1_1 = (rg, ds) -> ds.apply(getDiracCont21_part1_1(rg, ds));
            DataStateFunction mu_cont21_part1_2 = (rg, ds) -> ds.apply(getDiracCont21_part1_2(rg, ds));
            DataStateFunction mu_cont21_part2_1 = (rg, ds) -> ds.apply(getDiracCont21_part2_1(rg, ds));
            DataStateFunction mu_cont21_part2_2 = (rg, ds) -> ds.apply(getDiracCont21_part2_2(rg, ds));
            DataStateFunction mu_cont7_part1_1 = (rg, ds) -> ds.apply(getDiracCont7_part1_1(rg, ds));
            DataStateFunction mu_cont7_part1_2 = (rg, ds) -> ds.apply(getDiracCont7_part1_2(rg, ds));
            DataStateFunction mu_cont7_part2_1 = (rg, ds) -> ds.apply(getDiracCont7_part2_1(rg, ds));
            DataStateFunction mu_cont7_part3_1 = (rg, ds) -> ds.apply(getDiracCont7_part3_1(rg, ds));
            DataStateFunction mu_cont7_part4_1 = (rg, ds) -> ds.apply(getDiracCont7_part4_1(rg, ds));
            DataStateFunction mu_cont7_part5_1 = (rg, ds) -> ds.apply(getDiracCont7_part5_1(rg, ds));
            DataStateFunction mu_cont9_part1_1 = (rg, ds) -> ds.apply(getDiracCont9_part1_1(rg, ds));
            DataStateFunction mu_cont9_part1_2 = (rg, ds) -> ds.apply(getDiracCont9_part1_2(rg, ds));
            DataStateFunction mu_cont9_part2_1 = (rg, ds) -> ds.apply(getDiracCont9_part2_1(rg, ds));
            DataStateFunction mu_cont25_part2_1 = (rg, ds) -> ds.apply(getDiracCont25_part2_1(rg, ds));
            DataStateFunction mu_cont25_part2_2 = (rg, ds) -> ds.apply(getDiracCont25_part2_2(rg, ds));
            DataStateFunction mu_cont25_part2_3 = (rg, ds) -> ds.apply(getDiracCont25_part2_3(rg, ds));
            DataStateFunction mu_cont25_part1_p1_1 = (rg, ds) -> ds.apply(getDiracCont25_part1_p1_1(rg, ds));
            DataStateFunction mu_cont25_part1_p1_2 = (rg, ds) -> ds.apply(getDiracCont25_part1_p1_2(rg, ds));
            DataStateFunction mu_cont25_part1_p1_3 = (rg, ds) -> ds.apply(getDiracCont25_part1_p1_3(rg, ds));
            DataStateFunction mu_cont25_part1_p2_1 = (rg, ds) -> ds.apply(getDiracCont25_part1_p2_1(rg, ds));
            DataStateFunction mu_cont25_part1_p3_1 = (rg, ds) -> ds.apply(getDiracCont25_part1_p3_1(rg, ds));
            DataStateFunction mu_cont15_1 = (rg, ds) -> ds.apply(getDiracCont15_1(rg, ds));
            DataStateFunction mu_cont15_2 = (rg, ds) -> ds.apply(getDiracCont15_2(rg, ds));
            DataStateFunction mu_sav6_1 = (rg, ds) -> ds.apply(getDiracSav6_1(rg, ds));
            DataStateFunction mu_sav6_2 = (rg, ds) -> ds.apply(getDiracSav6_2(rg, ds));
            DataStateFunction mu_sav16_1 = (rg, ds) -> ds.apply(getDiracSav16_1(rg, ds));
            DataStateFunction mu_cont46_status_1 = (rg, ds) -> ds.apply(getDiracCont46_status_1(rg, ds));
            DataStateFunction mu_cont46_status_2 = (rg, ds) -> ds.apply(getDiracCont46_status_2(rg, ds));
            DataStateFunction mu_cont46_status_3 = (rg, ds) -> ds.apply(getDiracCont46_status_3(rg, ds));
            DataStateFunction mu_cont46_status_4 = (rg, ds) -> ds.apply(getDiracCont46_status_4(rg, ds));
            DataStateFunction mu_cont46_status_5 = (rg, ds) -> ds.apply(getDiracCont46_status_5(rg, ds));
            DataStateFunction mu_cont46_status_6 = (rg, ds) -> ds.apply(getDiracCont46_status_6(rg, ds));
            DataStateFunction mu_cont46_status_7 = (rg, ds) -> ds.apply(getDiracCont46_status_7(rg, ds));
            DataStateFunction mu_cont46_pow_Off = (rg, ds) -> ds.apply(getDiracCont46_pow_Off(rg, ds));
            DataStateFunction mu_cont46_pow_On = (rg, ds) -> ds.apply(getDiracCont46_pow_On(rg, ds));
            DataStateFunction mu_cont26_1 = (rg, ds) -> ds.apply(getDiracCont26_1(rg, ds));
            DataStateFunction mu_cont26_2 = (rg, ds) -> ds.apply(getDiracCont26_2(rg, ds));
            DataStateFunction mu_cont36_1_1 = (rg, ds) -> ds.apply(getDiracCont36_1_1(rg, ds));
            DataStateFunction mu_cont33_1 = (rg, ds) -> ds.apply(getDiracCont33_1(rg, ds));
            DataStateFunction mu_cont33_2 = (rg, ds) -> ds.apply(getDiracCont33_2(rg, ds));
            DataStateFunction mu_cont39_1_1 = (rg, ds) -> ds.apply(getDiracCont39_1_1(rg, ds));
            DataStateFunction mu_cont39_1_2 = (rg, ds) -> ds.apply(getDiracCont39_1_2(rg, ds));
            DataStateFunction mu_cont39_1_t2_2 = (rg, ds) -> ds.apply(getDiracCont39_1_t2_2(rg, ds));
            DataStateFunction mu_cont39_2_1 = (rg, ds) -> ds.apply(getDiracCont39_2_1(rg, ds));
            DataStateFunction mu_cont39_2_2 = (rg, ds) -> ds.apply(getDiracCont39_2_2(rg, ds));
            DataStateFunction mu_cont44_1 = (rg, ds) -> ds.apply(getDiracCont44_1(rg, ds));
            DataStateFunction mu_cont44_2 = (rg, ds) -> ds.apply(getDiracCont44_2(rg, ds));

            double eta_cont19 = 0.0;
            double eta_test0 = 1.0;
            double eta_cont36_3 = 0.0;
            double eta_cont36_3_part1 = 0.0;
            double eta_cont1_3 = 0.0;
            double eta_cont38 = 0.0;
            double eta_sav22 = 0.0;
            double eta_sav1 = 0.0;
            double eta_sav17 = 0.0;
            double eta_cont5 = 0.0;
            double eta_cont6_t2 = 0.0;
            double eta_cont6_t3 = 0.0;
            double eta_cont8 = 0.0;
            double eta_cont10 = 0.0;
            double eta_cont3 = 0.0;
            double eta_cont4 = 0.0;
            double eta_cont21 = 0.0;
            double eta_cont7 = 0.0;
            double eta_cont9 = 0.0;
            double eta_cont25_part1 = 0.0;
            double eta_cont25_part2 = 0.0;
            double eta_cont15 = 0.0;
            double eta_sav6 = 0.0;
            double eta_sav16 = 0.0;
            double eta_cont46 = 0.0;
            double eta_cont26 = 0.0;
            double eta_cont36_1 = 0.0;
            double eta_cont33 = 0.0;
            double eta_cont39_1 = 0.0;
            double eta_cont44 = 0.0;

            DisTLFormula phi_cont19_1 = new AlwaysDisTLFormula(
                    new TargetDisTLFormula(
                            mu_cont19,
                            Main::rho_cont19,
                            eta_cont19
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont19_2 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont19,
                                            Main::rho_cont19_t2_p1,
                                            eta_cont19
                                    )
                            ),
                            new AlwaysDisTLFormula(
                                    new BrinkDisTLFormula(
                                            mu_cont19_t2_p2,
                                            Main::rho_cont19_t2_p2,
                                            eta_cont19
                                    ),
                                    1,
                                    1
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont36_3_part2 = new AlwaysDisTLFormula(
                    new TargetDisTLFormula(
                            mu_cont36_3,
                            Main::rho_cont36_3,
                            eta_cont36_3
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont36_3_part1 = new AlwaysDisTLFormula(
                    new TargetDisTLFormula(
                            mu_cont36_3_part1,
                            Main::rho_cont36_3_part1,
                            eta_cont36_3_part1
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont1_3 = new AlwaysDisTLFormula(
                    new TargetDisTLFormula(
                            mu_cont1_3,
                            Main::rho_cont1_3,
                            eta_cont1_3
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont1_3_t2 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont1_3_t2_p1,
                                            Main::rho_cont1_3_t2_p1,
                                            eta_cont1_3
                                    )
                            ),
                            new TargetDisTLFormula(
                                    mu_cont1_3_t2_p2,
                                    Main::rho_cont1_3_t2_p2,
                                    eta_cont1_3
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont38 = new AlwaysDisTLFormula(
                    new ConjunctionDisTLFormula(
                            new TargetDisTLFormula(
                                    mu_cont38_part1,
                                    Main::rho_cont38_part1,
                                    eta_cont38
                            ),
                            new ConjunctionDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont38_part2,
                                            Main::rho_cont1_3,
                                            eta_cont38
                                    ),
                                    new TargetDisTLFormula(
                                            mu_cont38_part3,
                                            Main::rho_cont38_part3,
                                            eta_cont38
                                    )
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont38_t2 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new ConjunctionDisTLFormula(
                                    new NegationDisTLFormula(
                                            new TargetDisTLFormula(
                                                    mu_cont38_t2_p1,
                                                    Main::rho_cont38_t2_p1,
                                                    eta_cont38
                                            )
                                    ),
                                    new ConjunctionDisTLFormula(
                                            new NegationDisTLFormula(
                                                    new TargetDisTLFormula(
                                                            mu_cont38_t2_p2,
                                                            Main::rho_cont38_t2_p2,
                                                            eta_cont38
                                                    )
                                            ),
                                            new NegationDisTLFormula(
                                                    new TargetDisTLFormula(
                                                            mu_cont38_t2_p3,
                                                            Main::rho_cont38_t2_p3,
                                                            eta_cont38
                                                    )
                                            )
                                    )
                            ),
                            new TargetDisTLFormula(
                                    mu_cont1_3_t2_p2,
                                    Main::rho_cont1_3_t2_p2,
                                    eta_cont38
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_sav22 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_sav22,
                                            Main::rho_sav22,
                                            eta_sav22
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_sav22_2,
                                            Main::rho_sav22_2,
                                            eta_sav22
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );
            DisTLFormula phi_sav22_p2 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_sav22_p2,
                                            Main::rho_sav22_p2,
                                            eta_sav22
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_sav22_2,
                                            Main::rho_sav22_2,
                                            eta_sav22
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_sav1_al23 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_sav1_al23,
                                            Main::rho_sav1_al23,
                                            eta_sav1
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_sav6_2,
                                            Main::rho_sav_6_alarm,
                                            eta_sav1
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );
            DisTLFormula phi_sav1_al24 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_sav1_al24,
                                            Main::rho_sav1_al24,
                                            eta_sav1
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_sav6_2,
                                            Main::rho_sav_6_alarm,
                                            eta_sav1
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );
            DisTLFormula phi_sav1 = new ConjunctionDisTLFormula(
                    phi_sav1_al23,
                    phi_sav1_al24
            );

            DisTLFormula phi_sav1_al23_p = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_sav1_al23_p,
                                            Main::rho_sav1_al23_p,
                                            eta_sav1
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_sav6_2,
                                            Main::rho_sav_6_alarm,
                                            eta_sav1
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );
            DisTLFormula phi_sav1_al24_p = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_sav1_al24_p,
                                            Main::rho_sav1_al24_p,
                                            eta_sav1
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_sav6_2,
                                            Main::rho_sav_6_alarm,
                                            eta_sav1
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );
            DisTLFormula phi_sav1_p = new ConjunctionDisTLFormula(
                    phi_sav1_al23_p,
                    phi_sav1_al24_p
            );

            DisTLFormula phi_sav17 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_sav17_1,
                                            Main::rho_sav17,
                                            eta_sav17
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_sav6_2,
                                            Main::rho_sav_6_alarm,
                                            eta_sav17
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont5 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont5_part1,
                                            Main::rho_cont5_part1,
                                            eta_cont5
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont5_part2,
                                            Main::rho_cont5_part2,
                                            eta_cont5
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont6_t2_v2 = new DisjunctionDisTLFormula(
                    new EventuallyDisTLFormula(
                            new UntilDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont6_t1,
                                            Main::rho_cont6_t2_part2,
                                            eta_cont6_t2
                                    ),
                                    0,
                                    2,
                                    new TargetDisTLFormula(
                                            mu_cont6_t2_part3,
                                            Main::rho_cont6_t2_part3,
                                            eta_cont6_t2
                                    )
                            ),
                            0,
                            H
                    ),
                    new AlwaysDisTLFormula(
                            new BrinkDisTLFormula(
                                    mu_cont6_t1,
                                    Main::rho_cont6_t1,
                                    eta_cont6_t2
                            ),
                            0,
                            H
                    )
            );

            DisTLFormula phi_cont6_t3 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont6_t1,
                                            Main::rho_cont6_t2_part2,
                                            eta_cont6_t3
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont6_t2_part3,
                                            Main::rho_cont6_t2_part3,
                                            eta_cont6_t3
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );
            DisTLFormula phi_cont8 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont8_1,
                                            Main::rho_cont8_1,
                                            eta_cont8
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont8_2,
                                            Main::rho_cont8_2,
                                            eta_cont8
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );
            DisTLFormula phi_cont10 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont10_1,
                                            Main::rho_cont10_1,
                                            eta_cont10
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont8_2,
                                            Main::rho_cont8_2,
                                            eta_cont10
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );


            DisTLFormula phi_cont3 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont3_1,
                                            Main::rho_cont3_1,
                                            eta_cont3
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont3_2,
                                            Main::rho_cont3_2,
                                            eta_cont3
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont4_part1 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont4_part1_1,
                                            Main::rho_cont4_part1_1,
                                            eta_cont4
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont4_part1_2,
                                            Main::rho_cont4_part1_2,
                                            eta_cont4
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont4_part2 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont4_part2_1,
                                            Main::rho_cont4_part2_1,
                                            eta_cont4
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont4_part1_2,
                                            Main::rho_cont4_part1_2,
                                            eta_cont4
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont4 = new ConjunctionDisTLFormula(
                    phi_cont4_part1,
                    phi_cont4_part2
            );

            DisTLFormula phi_cont21_part1 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont21_part1_1,
                                            Main::rho_cont21_part1_1,
                                            eta_cont21
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont21_part1_2,
                                            Main::rho_cont21_part1_2,
                                            eta_cont21
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont21_part2 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont21_part2_1,
                                            Main::rho_cont21_part2_1,
                                            eta_cont21
                                    )
                            ),
                            new BrinkDisTLFormula(
                                    mu_cont21_part2_2,
                                    Main::rho_cont21_part2_2,
                                    eta_cont21
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont21 = //new AlwaysDisTLFormula(
                    new ConjunctionDisTLFormula(
                            phi_cont21_part1,
                            phi_cont21_part2
                    //),
                    //0,
                    //%H
            );

            DisTLFormula phi_cont7_p1 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont7_part1_1,
                                            Main::rho_cont7_part1_1,
                                            eta_cont7
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont7_part1_2,
                                            Main::rho_cont7_part1_2,
                                            eta_cont7
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont7_p2 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont7_part2_1,
                                            Main::rho_cont7_part2_1,
                                            eta_cont7
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont7_part1_2,
                                            Main::rho_cont7_part1_2,
                                            eta_cont7
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );
            DisTLFormula phi_cont7_p3 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont7_part3_1,
                                            Main::rho_cont7_part3_1,
                                            eta_cont7
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont7_part1_2,
                                            Main::rho_cont7_part1_2,
                                            eta_cont7
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );
            DisTLFormula phi_cont7_p4 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont7_part4_1,
                                            Main::rho_cont7_part4_1,
                                            eta_cont7
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont7_part1_2,
                                            Main::rho_cont7_part1_2,
                                            eta_cont7
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont7_p5 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont7_part5_1,
                                            Main::rho_cont7_part5_1,
                                            eta_cont7
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont7_part1_2,
                                            Main::rho_cont7_part1_2,
                                            eta_cont7
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont7 = new ConjunctionDisTLFormula(
                    new ConjunctionDisTLFormula(
                            new ConjunctionDisTLFormula(
                                    new ConjunctionDisTLFormula(
                                            phi_cont7_p4,
                                            phi_cont7_p5
                                    ),
                                    phi_cont7_p3
                            ),
                            phi_cont7_p2
                    ),
                    phi_cont7_p1
            );

            DisTLFormula phi_cont9_p1 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont9_part1_1,
                                            Main::rho_cont9_part1_1,
                                            eta_cont9
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont9_part1_2,
                                            Main::rho_cont9_part1_2,
                                            eta_cont9
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );
            DisTLFormula phi_cont9_p2 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont9_part2_1,
                                            Main::rho_cont9_part2_1,
                                            eta_cont9
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont9_part1_2,
                                            Main::rho_cont9_part1_2,
                                            eta_cont9
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont9 = new ConjunctionDisTLFormula(
                    phi_cont9_p1,
                    phi_cont9_p2
            );

            DisTLFormula phi_cont25_part1_p1 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont25_part1_p1_1,
                                            Main::rho_cont25_part1_p1_1,
                                            eta_cont25_part1
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new DisjunctionDisTLFormula(
                                            new TargetDisTLFormula(
                                                    mu_cont25_part1_p1_2,
                                                    Main::rho_cont25_part1_p1_2,
                                                    eta_cont25_part1
                                            ),
                                            new TargetDisTLFormula(
                                                    mu_cont25_part1_p1_3,
                                                    Main::rho_cont25_part1_p1_3,
                                                    eta_cont25_part1
                                            )
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont25_part1_p2 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont25_part1_p2_1,
                                            Main::rho_cont25_part1_p2_1,
                                            eta_cont25_part1
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont25_part1_p1_2,
                                            Main::rho_cont25_part1_p1_2,
                                            eta_cont25_part1
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont25_part1_p3 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont25_part1_p3_1,
                                            Main::rho_cont25_part1_p3_1,
                                            eta_cont25_part1
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont25_part1_p1_2,
                                            Main::rho_cont25_part1_p1_2,
                                            eta_cont25_part1
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );
            DisTLFormula phi_cont25_part1 = new ConjunctionDisTLFormula(
                    new ConjunctionDisTLFormula(
                            phi_cont25_part1_p1,
                            phi_cont25_part1_p2
                    ),
                    phi_cont25_part1_p3
            );

            DisTLFormula phi_cont25_part2 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new DisjunctionDisTLFormula(
                                            new TargetDisTLFormula(
                                                    mu_cont25_part2_1,
                                                    Main::rho_cont25_part2_1,
                                                    eta_cont25_part2
                                            ),
                                            new TargetDisTLFormula(
                                                    mu_cont25_part2_2,
                                                    Main::rho_cont25_part2_2,
                                                    eta_cont25_part2
                                            )
                                    )
                            ),
                            new TargetDisTLFormula(
                                    mu_cont25_part2_3,
                                    Main::rho_cont25_part2_3,
                                    eta_cont25_part2)
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont15 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont15_1,
                                            Main::rho_cont_15_retries,
                                            eta_cont15
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont15_2,
                                            Main::rho_cont_15_status,
                                            eta_cont15
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_sav6 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_sav6_1,
                                            Main::rho_sav_6_dis_1,
                                            eta_sav6
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_sav6_2,
                                            Main::rho_sav_6_alarm,
                                            eta_sav6
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );
            DisTLFormula phi_sav16 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_sav16_1,
                                            Main::rho_sav_16_dis_1,
                                            eta_sav16
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_sav6_2,
                                            Main::rho_sav_6_alarm,
                                            eta_sav16
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont46_p1 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont46_status_6,
                                            Main::rho_cont_46_part1_1,
                                            eta_cont46
                                    )
                            ),
                            new AlwaysDisTLFormula(
                                    new DisjunctionDisTLFormula(
                                            new ConjunctionDisTLFormula(
                                                    new BrinkDisTLFormula(
                                                            mu_cont46_status_1,
                                                            Main::rho_cont_46_brink_1,
                                                            eta_cont46
                                                    ),
                                                    new ConjunctionDisTLFormula(
                                                            new BrinkDisTLFormula(
                                                                    mu_cont46_status_2,
                                                                    Main::rho_cont_46_brink_2,
                                                                    eta_cont46
                                                            ),
                                                            new ConjunctionDisTLFormula(
                                                                    new BrinkDisTLFormula(
                                                                            mu_cont46_status_3,
                                                                            Main::rho_cont_46_brink_3,
                                                                            eta_cont46
                                                                    ),
                                                                    new ConjunctionDisTLFormula(
                                                                            new BrinkDisTLFormula(
                                                                                    mu_cont46_status_4,
                                                                                    Main::rho_cont_46_brink_4,
                                                                                    eta_cont46
                                                                            ),
                                                                            new ConjunctionDisTLFormula(
                                                                                    new BrinkDisTLFormula(
                                                                                            mu_cont46_status_5,
                                                                                            Main::rho_cont_46_brink_5,
                                                                                            eta_cont46
                                                                                    ),
                                                                                    new BrinkDisTLFormula(
                                                                                            mu_cont46_status_7,
                                                                                            Main::rho_cont_46_brink_7,
                                                                                            eta_cont46
                                                                                    )
                                                                            )
                                                                    )
                                                            )
                                                    )
                                            ),
                                            new TargetDisTLFormula(
                                                    mu_cont46_pow_Off,
                                                    Main::rho_cont_46_tar_off,
                                                    eta_cont46
                                            )
                                    ),
                                    1,
                                    1
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont46_p2 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont46_status_7,
                                            Main::rho_cont_46_part2_1,
                                            eta_cont46
                                    )
                            ),
                            new AlwaysDisTLFormula(
                                    new DisjunctionDisTLFormula(
                                            new ConjunctionDisTLFormula(
                                                    new BrinkDisTLFormula(
                                                            mu_cont46_status_1,
                                                            Main::rho_cont_46_brink_1,
                                                            eta_cont46
                                                    ),
                                                    new ConjunctionDisTLFormula(
                                                            new BrinkDisTLFormula(
                                                                    mu_cont46_status_2,
                                                                    Main::rho_cont_46_brink_2,
                                                                    eta_cont46
                                                            ),
                                                            new ConjunctionDisTLFormula(
                                                                    new BrinkDisTLFormula(
                                                                            mu_cont46_status_3,
                                                                            Main::rho_cont_46_brink_3,
                                                                            eta_cont46
                                                                    ),
                                                                    new ConjunctionDisTLFormula(
                                                                            new BrinkDisTLFormula(
                                                                                    mu_cont46_status_4,
                                                                                    Main::rho_cont_46_brink_4,
                                                                                    eta_cont46
                                                                            ),
                                                                            new ConjunctionDisTLFormula(
                                                                                    new BrinkDisTLFormula(
                                                                                            mu_cont46_status_5,
                                                                                            Main::rho_cont_46_brink_5,
                                                                                            eta_cont46
                                                                                    ),
                                                                                    new BrinkDisTLFormula(
                                                                                            mu_cont46_status_6,
                                                                                            Main::rho_cont_46_brink_6,
                                                                                            eta_cont46
                                                                                    )
                                                                            )
                                                                    )
                                                            )
                                                    )
                                            ),
                                            new TargetDisTLFormula(
                                                    mu_cont46_pow_On,
                                                    Main::rho_cont_46_tar_on,
                                                    eta_cont46
                                            )
                                    ),
                                    1,
                                    1
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont46_r1 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont46_status_6,
                                            Main::rho_cont_46_part1_1,
                                            eta_cont46
                                    )
                            ),
                            new NegationDisTLFormula(
                                    new UntilDisTLFormula(
                                            new NegationDisTLFormula(
                                                    new TargetDisTLFormula(
                                                            mu_cont46_pow_Off,
                                                            Main::rho_cont_46_tar_off,
                                                            eta_cont46
                                                    )
                                            ),
                                            0,
                                            H,
                                            new NegationDisTLFormula(
                                                    new ConjunctionDisTLFormula(
                                                            new BrinkDisTLFormula(
                                                                    mu_cont46_status_1,
                                                                    Main::rho_cont_46_brink_1,
                                                                    eta_cont46
                                                            ),
                                                            new ConjunctionDisTLFormula(
                                                                    new BrinkDisTLFormula(
                                                                            mu_cont46_status_2,
                                                                            Main::rho_cont_46_brink_2,
                                                                            eta_cont46
                                                                    ),
                                                                    new ConjunctionDisTLFormula(
                                                                            new BrinkDisTLFormula(
                                                                                    mu_cont46_status_3,
                                                                                    Main::rho_cont_46_brink_3,
                                                                                    eta_cont46
                                                                            ),
                                                                            new ConjunctionDisTLFormula(
                                                                                    new BrinkDisTLFormula(
                                                                                            mu_cont46_status_4,
                                                                                            Main::rho_cont_46_brink_4,
                                                                                            eta_cont46
                                                                                    ),
                                                                                    new ConjunctionDisTLFormula(
                                                                                            new BrinkDisTLFormula(
                                                                                                    mu_cont46_status_5,
                                                                                                    Main::rho_cont_46_brink_5,
                                                                                                    eta_cont46
                                                                                            ),
                                                                                            new BrinkDisTLFormula(
                                                                                                    mu_cont46_status_7,
                                                                                                    Main::rho_cont_46_brink_7,
                                                                                                    eta_cont46
                                                                                            )
                                                                                    )
                                                                            )
                                                                    )
                                                            )
                                                    )
                                            )
                                    )
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont46_r2 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont46_status_7,
                                            Main::rho_cont_46_part2_1,
                                            eta_cont46
                                    )
                            ),
                            new NegationDisTLFormula(
                                    new UntilDisTLFormula(
                                            new NegationDisTLFormula(
                                                    new TargetDisTLFormula(
                                                            mu_cont46_pow_On,
                                                            Main::rho_cont_46_tar_on,
                                                            eta_cont46
                                                    )
                                            ),
                                            0,
                                            H,
                                            new NegationDisTLFormula(
                                                    new ConjunctionDisTLFormula(
                                                            new BrinkDisTLFormula(
                                                                    mu_cont46_status_2,
                                                                    Main::rho_cont_46_brink_2,
                                                                    eta_cont46
                                                            ),
                                                            new ConjunctionDisTLFormula(
                                                                    new BrinkDisTLFormula(
                                                                            mu_cont46_status_3,
                                                                            Main::rho_cont_46_brink_3,
                                                                            eta_cont46
                                                                    ),
                                                                    new ConjunctionDisTLFormula(
                                                                            new BrinkDisTLFormula(
                                                                                    mu_cont46_status_4,
                                                                                    Main::rho_cont_46_brink_4,
                                                                                    eta_cont46
                                                                            ),
                                                                            new ConjunctionDisTLFormula(
                                                                                    new BrinkDisTLFormula(
                                                                                            mu_cont46_status_5,
                                                                                            Main::rho_cont_46_brink_5,
                                                                                            eta_cont46
                                                                                    ),
                                                                                    new BrinkDisTLFormula(
                                                                                            mu_cont46_status_6,
                                                                                            Main::rho_cont_46_brink_6,
                                                                                            eta_cont46
                                                                                    )
                                                                            )
                                                                    )
                                                            )
                                                    )
                                            )
                                    )
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont46 = new ConjunctionDisTLFormula(
                    phi_cont46_p1,
                    phi_cont46_p2
            );
            DisTLFormula phi_cont46_r = new ConjunctionDisTLFormula(
                    phi_cont46_r1,
                    phi_cont46_r2
            );

            DisTLFormula phi_cont26 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont26_1,
                                            Main::rho_cont26_1,
                                            eta_cont26
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont26_2,
                                            Main::rho_cont26_2,
                                            eta_cont26
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont36_1 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont36_1_1,
                                            Main::rho_cont36_1_1,
                                            eta_cont36_1
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont26_2,
                                            Main::rho_cont26_2,
                                            eta_cont36_1
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont33 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont33_1,
                                            Main::rho_cont33_1,
                                            eta_cont33
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont33_2,
                                            Main::rho_cont33_2,
                                            eta_cont33
                                    ),
                                    0,
                                    2
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont39_1 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont39_1_1,
                                            Main::rho_cont39_1_1,
                                            eta_cont39_1
                                    )
                            ),
                            new TargetDisTLFormula(
                                    mu_cont39_1_2,
                                    Main::rho_cont39_1_2,
                                    eta_cont39_1
                            )
                    ),
                    0,
                    H
            );
            DisTLFormula phi_cont39_1_t2 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont39_1_1,
                                            Main::rho_cont39_1_1,
                                            eta_cont39_1
                                    )
                            ),
                            new TargetDisTLFormula(
                                    mu_cont39_1_t2_2,
                                    Main::rho_cont39_1_t2_2,
                                    eta_cont39_1
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont39_2 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont39_2_1,
                                            Main::rho_cont39_2_1,
                                            eta_cont39_1
                                    )
                            ),
                            new TargetDisTLFormula(
                                    mu_cont39_2_2,
                                    Main::rho_cont39_2_2,
                                    eta_cont39_1
                            )
                    ),
                    0,
                    H
            );
            DisTLFormula phi_cont44 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new NegationDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont44_1,
                                            Main::rho_cont44_1,
                                            eta_cont44
                                    )
                            ),
                            new EventuallyDisTLFormula(
                                    new TargetDisTLFormula(
                                            mu_cont44_2,
                                            Main::rho_cont44_2,
                                            eta_cont44
                                    ),
                                    1,
                                    1
                            )
                    ),
                    0,
                    H
            );

            DisTLFormula phi_cont1 = new AlwaysDisTLFormula(
                    new DisjunctionDisTLFormula(
                            new DisjunctionDisTLFormula(
                                    new DisjunctionDisTLFormula(
                                            new DisjunctionDisTLFormula(
                                                    new DisjunctionDisTLFormula(
                                                            new DisjunctionDisTLFormula(
                                                                    new DisjunctionDisTLFormula(
                                                                            //status 7
                                                                            new TargetDisTLFormula(
                                                                                    mu_test7,
                                                                                    Main::rho_test7,
                                                                                    eta_test0
                                                                            ),
                                                                            new TargetDisTLFormula(
                                                                                    mu_test6,
                                                                                    Main::rho_test6,
                                                                                    eta_test0
                                                                            )
                                                                            //status 6
                                                                    ),
                                                                    new TargetDisTLFormula(
                                                                            mu_test5,
                                                                            Main::rho_test5,
                                                                            eta_test0
                                                                    )
                                                                    //status 5
                                                            ),
                                                            new TargetDisTLFormula(
                                                                    mu_test4,
                                                                    Main::rho_test4,
                                                                    eta_test0
                                                            )
                                                            //status 4
                                                    ),
                                                    new TargetDisTLFormula(
                                                            mu_test3,
                                                            Main::rho_test3,
                                                            eta_test0
                                                    )
                                                    //status 3
                                            ),
                                            new TargetDisTLFormula(
                                                    mu_test2,
                                                    Main::rho_test2,
                                                    eta_test0
                                            )
                                            //status 2
                                    ),
                                    new TargetDisTLFormula(
                                            mu_test1,
                                            Main::rho_test1,
                                            eta_test0
                                    )
                                    //status 1
                            ),
                            new TargetDisTLFormula(
                                    mu_test0,
                                    Main::rho_test0,
                                    eta_test0
                            )
                            //status 0
                    ),
                    0,
                    H
            );

            double value_cont19_1 = new DoubleSemanticsVisitor().eval(phi_cont19_1).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_19_1, in 0, wrt phi_cont19_1: "+value_cont19_1);

            double value_cont19_2 = new DoubleSemanticsVisitor().eval(phi_cont19_2).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_19_t2, in 0, wrt phi_cont19_2: "+value_cont19_2);

            double value_cont1 = new DoubleSemanticsVisitor().eval(phi_cont1).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_1, in 0, wrt phi_cont1: "+value_cont1);

            double value_cont36_3_part2 = new DoubleSemanticsVisitor().eval(phi_cont36_3_part2).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_36_3_part2, in 0, wrt phi_cont36_3_part2: "+value_cont36_3_part2);

            double value_cont36_3_part1 = new DoubleSemanticsVisitor().eval(phi_cont36_3_part1).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_36_3_part1, in 0, wrt phi_cont36_3_part1: "+value_cont36_3_part1);

            double value_cont1_3 = new DoubleSemanticsVisitor().eval(phi_cont1_3).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_1_3, in 0, wrt phi_cont1_3: "+value_cont1_3);

            double value_cont1_3_t2 = new DoubleSemanticsVisitor().eval(phi_cont1_3_t2).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_1_3_t2, in 0, wrt phi_cont1_3_t2: "+value_cont1_3_t2);

            double value_cont38 = new DoubleSemanticsVisitor().eval(phi_cont38).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_38, in 0, wrt phi_cont38: "+value_cont38);

            double value_cont38_t2 = new DoubleSemanticsVisitor().eval(phi_cont38_t2).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_38_t2, in 0, wrt phi_cont38_t2: "+value_cont38_t2);

            double value_sav22 = new DoubleSemanticsVisitor().eval(phi_sav22).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing SAV_22, in 0, wrt phi_sav22: "+value_sav22);

            double value_sav22_p2 = new DoubleSemanticsVisitor().eval(phi_sav22_p2).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing SAV_22_p2, in 0, wrt phi_sav22_p2: "+value_sav22_p2);

            double value_sav1_al23 = new DoubleSemanticsVisitor().eval(phi_sav1_al23).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing SAV_1_al23, in 0, wrt phi_sav1_al23: "+value_sav1_al23);

            double value_sav1_al24 = new DoubleSemanticsVisitor().eval(phi_sav1_al24).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing SAV_1_al24, in 0, wrt phi_sav1_al24: "+value_sav1_al24);

            double value_sav1 = new DoubleSemanticsVisitor().eval(phi_sav1).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing SAV_1, in 0, wrt phi_sav1: "+value_sav1);

            double value_sav1_al23_p = new DoubleSemanticsVisitor().eval(phi_sav1_al23_p).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing SAV_1_al23_p, in 0, wrt phi_sav1_al23_p: "+value_sav1_al23_p);

            double value_sav1_al24_p = new DoubleSemanticsVisitor().eval(phi_sav1_al24_p).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing SAV_1_al24_p, in 0, wrt phi_sav1_al24_p: "+value_sav1_al24_p);

            double value_sav1_p = new DoubleSemanticsVisitor().eval(phi_sav1_p).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing SAV_1_p, in 0, wrt phi_sav1_p: "+value_sav1_p);

            double value_sav17 = new DoubleSemanticsVisitor().eval(phi_sav17).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing SAV_17, in 0, wrt phi_sav17: "+value_sav17);

            double value_cont5 = new DoubleSemanticsVisitor().eval(phi_cont5).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_5, in 0, wrt phi_cont5: "+value_cont5);

            double value_cont6_t3 = new DoubleSemanticsVisitor().eval(phi_cont6_t3).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_6_t3, in 0, wrt phi_cont6_t3: "+value_cont6_t3);

            double value_cont6_t2_v2 = new DoubleSemanticsVisitor().eval(phi_cont6_t2_v2).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_6_t2_v2, in 0, wrt phi_cont6_t2_v2: "+value_cont6_t2_v2);

            double value_cont8 = new DoubleSemanticsVisitor().eval(phi_cont8).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_8, in 0, wrt phi_cont8: "+value_cont8);

            double value_cont10 = new DoubleSemanticsVisitor().eval(phi_cont10).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_10, in 0, wrt phi_cont10: "+value_cont10);

            double value_cont3 = new DoubleSemanticsVisitor().eval(phi_cont3).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_3, in 0, wrt phi_cont3: "+value_cont3);

            double value_cont4_part1 = new DoubleSemanticsVisitor().eval(phi_cont4_part1).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_4_part1, in 0, wrt phi_cont4_part1: "+value_cont4_part1);

            double value_cont4_part2 = new DoubleSemanticsVisitor().eval(phi_cont4_part2).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_4_part2, in 0, wrt phi_cont4_part2: "+value_cont4_part2);

            double value_cont4 = new DoubleSemanticsVisitor().eval(phi_cont4).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_4, in 0, wrt phi_cont4: "+value_cont4);

            double value_cont21_part1 = new DoubleSemanticsVisitor().eval(phi_cont21_part1).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_21_part1, in 0, wrt phi_cont21_part1: "+value_cont21_part1);

            double value_cont21_part2 = new DoubleSemanticsVisitor().eval(phi_cont21_part2).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_21_part2, in 0, wrt phi_cont21_part2: "+value_cont21_part2);

            double value_cont21 = new DoubleSemanticsVisitor().eval(phi_cont21).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_21, in 0, wrt phi_cont21: "+value_cont21);

            double value_cont7_p1 = new DoubleSemanticsVisitor().eval(phi_cont7_p1).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_7_p1, in 0, wrt phi_cont7_p1: "+value_cont7_p1);

            double value_cont7_p2 = new DoubleSemanticsVisitor().eval(phi_cont7_p2).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_7_p2, in 0, wrt phi_cont7_p2: "+value_cont7_p2);

            double value_cont7_p3 = new DoubleSemanticsVisitor().eval(phi_cont7_p3).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_7_p3, in 0, wrt phi_cont7_p3: "+value_cont7_p3);

            double value_cont7_p4 = new DoubleSemanticsVisitor().eval(phi_cont7_p4).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_7_p4, in 0, wrt phi_cont7_p4: "+value_cont7_p4);

            double value_cont7_p5 = new DoubleSemanticsVisitor().eval(phi_cont7_p5).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_7_p5, in 0, wrt phi_cont7_p5: "+value_cont7_p5);

            double value_cont7 = new DoubleSemanticsVisitor().eval(phi_cont7).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_7, in 0, wrt phi_cont7: "+value_cont7);

            double value_cont9_p1 = new DoubleSemanticsVisitor().eval(phi_cont9_p1).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_9_p1, in 0, wrt phi_cont9_p1: "+value_cont9_p1);

            double value_cont9_p2 = new DoubleSemanticsVisitor().eval(phi_cont9_p2).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_9_p2, in 0, wrt phi_cont9_p2: "+value_cont9_p2);

            double value_cont9 = new DoubleSemanticsVisitor().eval(phi_cont9).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_9, in 0, wrt phi_cont9: "+value_cont9);

            double value_cont25_part1_p1 = new DoubleSemanticsVisitor().eval(phi_cont25_part1_p1).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_25_part1_p1, in 0, wrt phi_cont25_part1_p1: "+value_cont25_part1_p1);

            double value_cont25_part1_p2 = new DoubleSemanticsVisitor().eval(phi_cont25_part1_p2).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_25_part1_p2, in 0, wrt phi_cont25_part1_p2: "+value_cont25_part1_p2);

            double value_cont25_part1_p3 = new DoubleSemanticsVisitor().eval(phi_cont25_part1_p3).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_25_part1_p3, in 0, wrt phi_cont25_part1_p3: "+value_cont25_part1_p3);

            double value_cont25_part1 = new DoubleSemanticsVisitor().eval(phi_cont25_part1).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_25_part1, in 0, wrt phi_cont25_part1: "+value_cont25_part1);

            double value_cont25_part2 = new DoubleSemanticsVisitor().eval(phi_cont25_part2).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_25_part2, in 0, wrt phi_cont25_part2: "+value_cont25_part2);

            double value_cont15 = new DoubleSemanticsVisitor().eval(phi_cont15).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_15, in 0, wrt phi_cont15: "+value_cont15);

            double value_sav6 = new DoubleSemanticsVisitor().eval(phi_sav6).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing SAV_6, in 0, wrt phi_sav6: "+value_sav6);

            double value_sav16 = new DoubleSemanticsVisitor().eval(phi_sav16).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing SAV_16, in 0, wrt phi_sav16: "+value_sav16);

            double value_cont46_p1 = new DoubleSemanticsVisitor().eval(phi_cont46_p1).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_46_p1, in 0, wrt phi_cont46_p1: "+value_cont46_p1);

            double value_cont46_p2 = new DoubleSemanticsVisitor().eval(phi_cont46_p2).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_46_p2, in 0, wrt phi_cont46_p2: "+value_cont46_p2);

            double value_cont46 = new DoubleSemanticsVisitor().eval(phi_cont46).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_46, in 0, wrt phi_cont46: "+value_cont46);

            //double value_cont46_r1 = new DoubleSemanticsVisitor().eval(phi_cont46_r1).eval(SIZE_DISTL, 0, sequence);
            //System.out.println("Robustness of testing CONT_46_r1, in 0, wrt phi_cont46_r1: "+value_cont46_r1);

            //double value_cont46_r2 = new DoubleSemanticsVisitor().eval(phi_cont46_r2).eval(SIZE_DISTL, 0, sequence);
            //System.out.println("Robustness of testing CONT_46_r2, in 0, wrt phi_cont46_r2: "+value_cont46_r2);

            //double value_cont46_r = new DoubleSemanticsVisitor().eval(phi_cont46_r).eval(SIZE_DISTL, 0, sequence);
            //System.out.println("Robustness of testing CONT_46_r, in 0, wrt phi_cont46_r: "+value_cont46_r);

            double value_cont26 = new DoubleSemanticsVisitor().eval(phi_cont26).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_26, in 0, wrt phi_cont26: "+value_cont26);

            double value_cont36_1 = new DoubleSemanticsVisitor().eval(phi_cont36_1).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_36_1, in 0, wrt phi_cont36_1: "+value_cont36_1);

            double value_cont33 = new DoubleSemanticsVisitor().eval(phi_cont33).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_33, in 0, wrt phi_cont33: "+value_cont33);

            double value_cont39_1 = new DoubleSemanticsVisitor().eval(phi_cont39_1).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_39_1, in 0, wrt phi_cont39_1: "+value_cont39_1);

            double value_cont39_1_t2 = new DoubleSemanticsVisitor().eval(phi_cont39_1_t2).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_39_1_t2, in 0, wrt phi_cont39_1_t2: "+value_cont39_1_t2);

            double value_cont39_2 = new DoubleSemanticsVisitor().eval(phi_cont39_2).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_39_2, in 0, wrt phi_cont39_2: "+value_cont39_2);

            double value_cont44 = new DoubleSemanticsVisitor().eval(phi_cont44).eval(SIZE_DISTL, 0, sequence);
            System.out.println("Robustness of testing CONT_44, in 0, wrt phi_cont44: "+value_cont44);







            Util.writeToCSV("./slow_novel_03.csv",val_test);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    //Used for the simulation of a sequence
    private static void printLData(RandomGenerator rg, ArrayList<String> label, ArrayList<DataStateExpression> F, SystemState s, int steps, int size) {
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
    //used for simulating perturbed sequences
    private static void printLDataP(RandomGenerator rg, ArrayList<String> label, ArrayList<DataStateExpression> F, Perturbation p, SystemState s, int steps, int size) {
        System.out.println(label);
        double[][] datap = SystemState.sample(rg, F, p, s, steps, size);
        for (int i = 0; i < datap.length; i++) {
            System.out.printf("%d>  ", i);
            for (int j = 0; j < datap[i].length-1; j++) {
                System.out.printf("%f ", datap[i][j]);
            }
            System.out.printf("%f\n", datap[i][datap[i].length -1]);

        }
    }



    //DisTL distributions
    public static List<DataStateUpdate> getDiracCont19(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(selfTest_fail, 1.0));
        updates.add(new DataStateUpdate(Status, 4.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont19_t2_p2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 5.0));
        return updates;
    }

    public static List<DataStateUpdate> getDiracCont36_3(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(timer_exp, 0.0));
        updates.add(new DataStateUpdate(Status, 2.0));
        updates.add(new DataStateUpdate(phase, 3.0));
        updates.add(new DataStateUpdate(timer_triggerDelay, 0.4 + rg.nextDouble()*(2-0.4)));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont36_3_part1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(timer_exp, 0.0));
        updates.add(new DataStateUpdate(Status, 2.0));
        updates.add(new DataStateUpdate(phase, 3.0));
        updates.add(new DataStateUpdate(timer_triggerDelay, 0.5*state.get(timer_insp)));
        return updates;
    }

    public static List<DataStateUpdate> getDiracCont1_3(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(a_IN_valve, 0.0));
        updates.add(new DataStateUpdate(Status, 5.0));
        updates.add(new DataStateUpdate(a_OUT_valve, 1.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont1_3_t2_p1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 5.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont1_3_t2_p2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(a_IN_valve, 0.0));
        updates.add(new DataStateUpdate(a_OUT_valve, 1.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont38_part1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(a_IN_valve, 0.0));
        updates.add(new DataStateUpdate(Status, 3.0));
        updates.add(new DataStateUpdate(a_OUT_valve, 1.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont38_part3(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(a_IN_valve, 0.0));
        updates.add(new DataStateUpdate(Status, 4.0));
        updates.add(new DataStateUpdate(a_OUT_valve, 1.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont38_t2_p1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 3.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont38_t2_p2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 4.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont38_t2_p3(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 5.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracSav22(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 2));
        updates.add(new DataStateUpdate(phase, 3));
        updates.add(new DataStateUpdate(timer_PSV_exp, 0.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracSav22_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(a_LED, 1.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracSav22_p2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 2));
        updates.add(new DataStateUpdate(phase, 3));
        updates.add(new DataStateUpdate(timer_exp, rg.nextInt(3) + T_APNEALAG + 1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracSav1_al23(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(s_OS, rg.nextInt(5) + PM_A_GB_FiO2 + 4));
        updates.add(new DataStateUpdate(Status, rg.nextInt(2)+1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracSav1_al24(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(s_OS, rg.nextInt(5)*-1 + PM_A_GB_FiO2 - 4));
        updates.add(new DataStateUpdate(Status, rg.nextInt(2)+1));
        return updates;
    }public static List<DataStateUpdate> getDiracSav1_al23_p(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(p_OS, rg.nextInt(5) + PM_A_GB_FiO2 + 4));
        updates.add(new DataStateUpdate(Status, rg.nextInt(2)+1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracSav1_al24_p(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(p_OS, rg.nextInt(5)*-1 + PM_A_GB_FiO2 - 4));
        updates.add(new DataStateUpdate(Status, rg.nextInt(2)+1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracSav17_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(V_E, rg.nextInt(10) + MAX_V_E + 1));
        updates.add(new DataStateUpdate(Status, rg.nextInt(2)+1));
        updates.add(new DataStateUpdate(counter_cycles, rg.nextInt(3)+2));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont5_part1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 5.0));
        updates.add(new DataStateUpdate(gui_req_change_mode_PSV, 1.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont5_part2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 2.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont6_t1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 5.0));
        updates.add(new DataStateUpdate(gui_req_change_mode_PCV, 1.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont6_t2_part3(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 1.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont8_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 1.0));
        updates.add(new DataStateUpdate(gui_req_stop_vent, 1.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont8_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 5.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont10_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 2.0));
        updates.add(new DataStateUpdate(gui_req_stop_vent, 1.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont3_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 3.0));
        updates.add(new DataStateUpdate(init_succ, 1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont3_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 4.0));
        return updates;
    }

    public static List<DataStateUpdate> getDiracCont4_part1_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 4.0));
        updates.add(new DataStateUpdate(gui_req_res_ven, 1));
        return updates;
    }

    public static List<DataStateUpdate> getDiracCont4_part1_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 5.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont4_part2_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 4.0));
        updates.add(new DataStateUpdate(power_switch_ok, 1));
        updates.add(new DataStateUpdate(no_leaks_breathing_circuit, 1));
        updates.add(new DataStateUpdate(out_valve_ok, 1));
        updates.add(new DataStateUpdate(alarms_ok, 1));
        return updates;
    }

    public static List<DataStateUpdate> getDiracCont21_part1_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(cycle_done, 1));
        return updates;
    }

    public static List<DataStateUpdate> getDiracCont21_part1_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(phase, 1));
        return updates;
    }

    public static List<DataStateUpdate> getDiracCont21_part2_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(counter_cycles, 0));
        updates.add(new DataStateUpdate(timer_insp, 0));
        return updates;
    }

    public static List<DataStateUpdate> getDiracCont21_part2_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(phase, 3));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont7_part1_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 1));
        updates.add(new DataStateUpdate(phase, 1));
        updates.add(new DataStateUpdate(switch_ready, 1));
        updates.add(new DataStateUpdate(gui_req_IP, 0));
        updates.add(new DataStateUpdate(gui_req_RM, 0));
        updates.add(new DataStateUpdate(timer_PCV_insp, rg.nextInt(3)*-1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont7_part1_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 2));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont7_part2_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 1));
        updates.add(new DataStateUpdate(phase, 2));
        updates.add(new DataStateUpdate(switch_ready, 1));
        updates.add(new DataStateUpdate(gui_req_IP, 0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont7_part3_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 1));
        updates.add(new DataStateUpdate(phase, 2));
        updates.add(new DataStateUpdate(switch_ready, 1));
        updates.add(new DataStateUpdate(timer_IP, rg.nextInt(3)*-1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont7_part4_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 1));
        updates.add(new DataStateUpdate(phase, 5));
        updates.add(new DataStateUpdate(switch_ready, 1));
        updates.add(new DataStateUpdate(gui_req_RM, 0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont7_part5_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 1));
        updates.add(new DataStateUpdate(phase, 5));
        updates.add(new DataStateUpdate(switch_ready, 1));
        updates.add(new DataStateUpdate(timer_RM, rg.nextInt(3)*-1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont9_part1_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 2));
        updates.add(new DataStateUpdate(phase, 3));
        updates.add(new DataStateUpdate(gui_req_EP, 0));
        updates.add(new DataStateUpdate(timer_PSV_exp, rg.nextInt(3)*-1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont9_part1_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 1));
        updates.add(new DataStateUpdate(rr_pcv, RR_AP));
        updates.add(new DataStateUpdate(p_insp_pcv, P_INSP_AP));
        updates.add(new DataStateUpdate(ie_pcv, IE_AP));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont9_part2_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 2));
        updates.add(new DataStateUpdate(phase, 4));
        updates.add(new DataStateUpdate(timer_EP, rg.nextInt(3)*-1));
        updates.add(new DataStateUpdate(gui_req_EP, 1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont25_part1_p1_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(phase, 3));
        updates.add(new DataStateUpdate(Status, 1));
        updates.add(new DataStateUpdate(timer_PCV_exp, rg.nextInt(3)*-1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont25_part1_p1_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(phase, 1));
        updates.add(new DataStateUpdate(Status, 1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont25_part1_p1_3(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(phase, 4));
        updates.add(new DataStateUpdate(Status, 1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont25_part1_p2_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(phase, 4));
        updates.add(new DataStateUpdate(Status, 1));
        updates.add(new DataStateUpdate(timer_EP, rg.nextInt(3)*-1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont25_part1_p3_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(phase, 3));
        updates.add(new DataStateUpdate(Status, 1));
        updates.add(new DataStateUpdate(timer_triggerDelay, rg.nextInt(3)*-1));
        updates.add(new DataStateUpdate(p_drop_PAW, rg.nextDouble()*3 + ITS_PCV));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont25_part2_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(phase, 3));
        updates.add(new DataStateUpdate(Status, 1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont25_part2_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(phase, 4));
        updates.add(new DataStateUpdate(Status, 1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont25_part2_3(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(timer_exp, rg.nextDouble()*(60/(state.get(rr_pcv)*(1+state.get(ie_pcv))))));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont15_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(nr_of_retries, rg.nextDouble()*2+5));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont15_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status,6));
        return updates;
    }
    public static List<DataStateUpdate> getDiracSav6_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(RR_ms, rg.nextDouble()*(MIN_RR-0.1) + 0.01));
        updates.add(new DataStateUpdate(Status, rg.nextInt(2)+1));
        updates.add(new DataStateUpdate(counter_cycles, rg.nextInt(3)+2));
        return updates;
    }
    public static List<DataStateUpdate> getDiracSav6_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(a_LED, 1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracSav16_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(V_E, rg.nextDouble()*(MIN_V_E-0.1)+0.01));
        updates.add(new DataStateUpdate(Status, rg.nextInt(2)+1));
        updates.add(new DataStateUpdate(counter_cycles, rg.nextInt(3)+2));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont46_status_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont46_status_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 2));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont46_status_3(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 3));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont46_status_4(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 4));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont46_status_5(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 5));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont46_status_6(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 6));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont46_status_7(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 7));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont46_pow_Off(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(b_powerOff, 1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont46_pow_On(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(b_powerOn, 1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont26_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 1));
        updates.add(new DataStateUpdate(phase, 3));
        updates.add(new DataStateUpdate(timer_triggerDelay, rg.nextInt(3)*-1));
        updates.add(new DataStateUpdate(p_drop_PAW, rg.nextDouble()*5 + ITS_PCV + 1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont26_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(phase, 1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont36_1_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 2));
        updates.add(new DataStateUpdate(phase, 3));
        updates.add(new DataStateUpdate(timer_triggerDelay, rg.nextInt(3)*-1));
        updates.add(new DataStateUpdate(p_drop_PAW, rg.nextDouble()*5 + ITS_PSV + 1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont33_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 2));
        updates.add(new DataStateUpdate(phase, 1));
        updates.add(new DataStateUpdate(p_Fl1_flow, rg.nextDouble()*((double) ETS/100*state.get(p_peak_flow) - 0.5)));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont33_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(a_IN_valve, 0));
        updates.add(new DataStateUpdate(p_drop_PAW, rg.nextDouble()*(4)+1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont39_1_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 1));
        updates.add(new DataStateUpdate(phase, rg.nextInt(2)+1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont39_1_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(a_OUT_valve, 0));
        updates.add(new DataStateUpdate(a_IN_valve, P_INSP_PCV));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont39_1_t2_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(a_OUT_valve, 0));
        updates.add(new DataStateUpdate(p_PS_ins_pressure,  PM_A_PEEP_VALVE + P_INSP_PCV));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont39_2_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 2));
        updates.add(new DataStateUpdate(phase, rg.nextInt(2)+1));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont39_2_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(a_OUT_valve, 0));
        updates.add(new DataStateUpdate(p_PS_ins_pressure,  PM_A_PEEP_VALVE + P_INSP_PSV));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont44_1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(phase, rg.nextInt(2)+1));
        updates.add(new DataStateUpdate(p_PS_ins_pressure,  rg.nextDouble()*10 + 1 + MAX_P_INSP));
        return updates;
    }
    public static List<DataStateUpdate> getDiracCont44_2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(phase, 3));
        return updates;
    }

    public static List<DataStateUpdate> getDiracTest0(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 0.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracTest1(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 1.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracTest2(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 2.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracTest3(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 3.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracTest4(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 4.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracTest5(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 5.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracTest6(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 6.0));
        return updates;
    }
    public static List<DataStateUpdate> getDiracTest7(RandomGenerator rg, DataState state){
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(Status, 7.0));
        return updates;
    }

    // PENALTY FUNCTIONS
    public static double rho_sav_6(DataState state) {
        if (state.get(RR_ms) < MIN_RR  && state.get(a_LED) == 0 && state.get(RR_ms)!= 0 ){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_sav_6_penal_no_alarm(DataState state) {
        if (state.get(RR_ms) < MIN_RR && state.get(RR_ms) != 0 ){//&& state.get(a_LED) == 0){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_sav_6_dis_1(DataState state) {
        if (state.get(RR_ms) >= MIN_RR || state.get(RR_ms) == 0 ||
                (state.get(Status) != 1 && state.get(Status) != 2) || state.get(counter_cycles) == 0){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_sav_6_alarm(DataState state) {
        if (state.get(a_LED) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_sav_16(DataState state) {
        if (state.get(V_E) < MIN_V_E && state.get(V_E) != 0 && state.get(a_LED) == 0){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_sav_16_penal_no_alarm(DataState state) {
        if (state.get(V_E) < MIN_V_E && state.get(V_E) != 0 && state.get(test_per) == 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_sav_16_dis_1(DataState state) {
        if (state.get(V_E) >= MIN_V_E || state.get(V_E) == 0 ||
                (state.get(Status) != 1 && state.get(Status) != 2) || state.get(counter_cycles) == 0){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont_15(DataState state) {
        if (state.get(nr_of_retries) >= 5 & state.get(Status) != 6){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont_15_penal_no_alarm(DataState state) {
        if (state.get(nr_of_retries) >= 5){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont_15_retries(DataState state) {
        if (state.get(nr_of_retries) < 5){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont_15_status(DataState state) {
        if (state.get(Status) != 6){
            return 1.0;
        } else {
            return 0.0;
        }
    }

    public static double rho_cont19(DataState state) {
        if (state.get(selfTest_fail) == 1 && state.get(Status) == 5){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont19_t2_p1(DataState state) {
        if (state.get(selfTest_fail) == 1 || state.get(Status) != 4){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont19_t2_p2(DataState state) {
        if (state.get(Status) == 5){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont1_3(DataState state) {
        if ((state.get(a_IN_valve) != 0 || state.get(a_OUT_valve) != 1) && state.get(Status) == 5){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont1_3_t2_p1(DataState state) {
        if (state.get(Status) != 5){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont1_3_t2_p2(DataState state) {
        if (state.get(a_IN_valve) != 0 || state.get(a_OUT_valve) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont38_part1(DataState state) {
        if ((state.get(a_IN_valve) != 0 || state.get(a_OUT_valve) != 1) && state.get(Status) == 3){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont38_part3(DataState state) {
        if ((state.get(a_IN_valve) != 0 || state.get(a_OUT_valve) != 1) && state.get(Status) == 4){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont38_t2_p1(DataState state) {
        if (state.get(Status) != 3){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont38_t2_p2(DataState state) {
        if (state.get(Status) != 4){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont38_t2_p3(DataState state) {
        if (state.get(Status) != 5){
            return 1.0;
        } else {
            return 0.0;
        }
    }


    public static double rho_cont36_3(DataState state) {
        if (state.get(timer_exp) == 0 && state.get(Status) == 2 && state.get(phase) == 3 && state.get(timer_triggerDelay) < 0.4){
            return Math.min(Math.pow((0.4-state.get(timer_triggerDelay))/0.3, 2), 1.0);
        } else if (state.get(timer_exp) == 0 && state.get(Status) == 2 && state.get(phase) == 3 && state.get(timer_triggerDelay) > 2){
            return Math.min(Math.pow((state.get(timer_triggerDelay)-2.0), 2), 1.0);
        } else {
            return 0.0;
        }
    }
    public static double rho_cont36_3_part1(DataState state) {
        if (state.get(timer_exp) == 0 && state.get(Status) == 2 && state.get(phase) == 3 && state.get(timer_triggerDelay) != 0.5*state.get(timer_insp)){
            return 1.0;
        } else {
            return 0.0;
        }
    }

    public static double rho_sav22(DataState state) {
        if (state.get(Status) != 2 || state.get(phase) != 3 || state.get(timer_PSV_exp) > 0){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_sav22_2(DataState state) {
        if (state.get(a_LED) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_sav22_p2(DataState state) {
        if (state.get(Status) != 2 || state.get(phase) != 3 || state.get(timer_exp) <= T_APNEALAG){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_sav1_al23(DataState state) {
        if (state.get(s_OS) <= PM_A_GB_FiO2 + 3 || (state.get(Status) != 1 && state.get(Status) != 2)){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_sav1_al24(DataState state) {
        if (state.get(s_OS) >= PM_A_GB_FiO2 - 3 || (state.get(Status) != 1 && state.get(Status) != 2)){
            return 1.0;
        } else {
            return 0.0;
        }
    }public static double rho_sav1_al23_p(DataState state) {
        if (state.get(p_OS) <= PM_A_GB_FiO2 + 3 || (state.get(Status) != 1 && state.get(Status) != 2)){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_sav1_al24_p(DataState state) {
        if (state.get(p_OS) >= PM_A_GB_FiO2 - 3 || (state.get(Status) != 1 && state.get(Status) != 2)){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_sav17(DataState state) {
        if (state.get(V_E) <= MAX_V_E ||
                (state.get(Status) != 1 && state.get(Status) != 2) || state.get(counter_cycles) == 0){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont5_part1(DataState state) {
        if (state.get(Status) != 5 || state.get(gui_req_change_mode_PSV) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont5_part2(DataState state) {
        if (state.get(Status) != 2){
            return 1.0;
        } else {
            return 0.0;
        }
    }

    public static double rho_cont6_t1(DataState state) {
        if (state.get(Status) == 5 && state.get(gui_req_change_mode_PCV) == 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont6_t2_part2(DataState state) {
        if (state.get(Status) != 5 || state.get(gui_req_change_mode_PCV) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont6_t2_part3(DataState state) {
        if (state.get(Status) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont8_1(DataState state) {
        if (state.get(Status) != 1 || state.get(gui_req_stop_vent) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont8_2(DataState state) {
        if (state.get(Status) != 5){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont10_1(DataState state) {
        if (state.get(Status) != 2 || state.get(gui_req_stop_vent) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont3_1(DataState state) {
        if (state.get(Status) != 3 || state.get(init_succ) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont3_2(DataState state) {
        if (state.get(Status) != 4){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont4_part1_1(DataState state) {
        if (state.get(Status) != 4 || state.get(gui_req_res_ven) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont4_part1_2(DataState state) {
        if (state.get(Status) != 5){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont4_part2_1(DataState state) {
        if (state.get(Status) != 4 || state.get(power_switch_ok) != 1 || state.get(alarms_ok) != 1
                || state.get(no_leaks_breathing_circuit) != 1 || state.get(out_valve_ok) != 1 ){
            return 1.0;
        } else {
            return 0.0;
        }
    }


    public static double rho_cont21_part1_1(DataState state) {
        if (state.get(cycle_done) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont21_part1_2(DataState state) {
        if (state.get(phase) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont21_part2_1(DataState state) {
        if (state.get(counter_cycles) != 0 || state.get(timer_insp) != 0){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont21_part2_2(DataState state) {
        if (state.get(phase) == 3){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont7_part1_1(DataState state) {
        if (state.get(Status) != 1 || state.get(phase) != 1 || state.get(timer_PCV_insp) > 0 ||
                state.get(switch_ready) != 1 || state.get(gui_req_IP) != 0 || state.get(gui_req_RM) != 0){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont7_part1_2(DataState state) {
        if (state.get(Status) != 2){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont7_part2_1(DataState state) {
        if (state.get(Status) != 1 || state.get(phase) != 2 || state.get(switch_ready) != 1 || state.get(gui_req_IP) != 0){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont7_part3_1(DataState state) {
        if (state.get(Status) != 1 || state.get(phase) != 2 || state.get(switch_ready) != 1 || state.get(timer_IP) > 0){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont7_part4_1(DataState state) {
        if (state.get(Status) != 1 || state.get(phase) != 5 || state.get(switch_ready) != 1 || state.get(gui_req_RM) != 0){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont7_part5_1(DataState state) {
        if (state.get(Status) != 1 || state.get(phase) != 5 || state.get(switch_ready) != 1 || state.get(timer_RM) > 0){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont9_part1_1(DataState state) {
        if (state.get(Status) != 2 || state.get(phase) != 3 || state.get(timer_PSV_exp) > 0 || state.get(gui_req_EP) != 0){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont9_part1_2(DataState state) {
        if (state.get(Status) != 1 || state.get(rr_pcv) != RR_AP || state.get(p_insp_pcv) != P_INSP_AP || state.get(ie_pcv) != IE_AP){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont9_part2_1(DataState state) {
        if (state.get(Status) != 2 || state.get(phase) != 4 || state.get(timer_EP) > 0 || state.get(gui_req_EP) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont25_part1_p1_1(DataState state) {
        if (state.get(phase) != 3 || state.get(Status) != 1 || state.get(timer_PCV_exp) > 0){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont25_part1_p1_2(DataState state) {
        if (state.get(phase) != 1 || state.get(Status) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont25_part1_p1_3(DataState state) {
        if (state.get(phase) != 4 || state.get(Status) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont25_part1_p2_1(DataState state) {
        if (state.get(phase) != 4 || state.get(Status) != 1 || state.get(timer_EP) > 0){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont25_part1_p3_1(DataState state) {
        if (state.get(phase) != 3 || state.get(Status) != 1 || state.get(timer_triggerDelay) > 0 || state.get(p_drop_PAW) <= ITS_PCV){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont25_part2_1(DataState state) {
        if (state.get(phase) != 3 || state.get(Status) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont25_part2_2(DataState state) {
        if (state.get(phase) != 4 || state.get(Status) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont25_part2_3(DataState state) {
        if (state.get(timer_exp) > 60/(state.get(rr_pcv)*(1+state.get(ie_pcv)))){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont_46_part1_1(DataState state) {
        if (state.get(Status) != 6){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont_46_part2_1(DataState state) {
        if (state.get(Status) != 7){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont_46_brink_1(DataState state) {
        if (state.get(Status) == 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont_46_brink_2(DataState state) {
        if (state.get(Status) == 2){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont_46_brink_3(DataState state) {
        if (state.get(Status) == 3){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont_46_brink_4(DataState state) {
        if (state.get(Status) == 4){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont_46_brink_5(DataState state) {
        if (state.get(Status) == 5){
            return 1.0;
        } else {
            return 0.0;
        }

    }
    public static double rho_cont_46_brink_6(DataState state) {
        if (state.get(Status) == 6){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont_46_brink_7(DataState state) {
        if (state.get(Status) == 7){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont_46_tar_off(DataState state) {
        if (state.get(b_powerOff) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont_46_tar_on(DataState state) {
        if (state.get(b_powerOn) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont26_1(DataState state) {
        if (state.get(Status) != 1 || state.get(phase) != 3 || state.get(timer_triggerDelay) > 0
                || state.get(p_drop_PAW) <= ITS_PCV){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont26_2(DataState state) {
        if (state.get(phase) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont36_1_1(DataState state) {
        if (state.get(Status) != 2 || state.get(phase) != 3 || state.get(timer_triggerDelay) > 0
                || state.get(p_drop_PAW) <= ITS_PSV){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont33_1(DataState state) {
        if (state.get(Status) != 2 || state.get(phase) != 1 || state.get(p_Fl1_flow) >= (ETS/100*p_peak_flow)){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont33_2(DataState state) {
        if (state.get(a_IN_valve) != 1 || state.get(p_drop_PAW) <= 0){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont39_1_1(DataState state) {
        if (state.get(Status) != 1 || (state.get(phase) != 1 && state.get(phase) != 2) ){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont39_1_2(DataState state) {
        if (state.get(a_OUT_valve) != 0 || (state.get(a_IN_valve) != P_INSP_PCV && state.get(a_IN_valve) != P_INSP_AP) ){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont39_1_t2_2(DataState state) {
        if (state.get(a_OUT_valve) != 0 || (state.get(p_PS_ins_pressure) != (P_INSP_PCV + PM_A_PEEP_VALVE)
                && state.get(p_PS_ins_pressure) != (P_INSP_AP + PM_A_PEEP_VALVE))){
            return Math.max(
                    Math.min(
                            Math.min(Math.abs(PM_A_PEEP_VALVE + P_INSP_PCV - state.get(p_PS_ins_pressure)),
                                    Math.abs(PM_A_PEEP_VALVE + P_INSP_AP - state.get(p_PS_ins_pressure))),
                            1.0),
                    state.get(a_OUT_valve)
            );
        } else {
            return 0.0;
        }
    }
    public static double rho_cont39_2_1(DataState state) {
        if (state.get(Status) != 2 || (state.get(phase) != 1 && state.get(phase) != 2) ){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont39_2_2(DataState state) {
        if (state.get(a_OUT_valve) != 0 || state.get(p_PS_ins_pressure) != (P_INSP_PSV + PM_A_PEEP_VALVE)){
            return Math.max(
                    Math.min(Math.abs(PM_A_PEEP_VALVE + P_INSP_PSV - state.get(p_PS_ins_pressure)),
                            1.0),
                    state.get(a_OUT_valve)
            );
        } else {
            return 0.0;
        }
    }
    public static double rho_cont44_1(DataState state) {
        if ((state.get(phase) != 1 && state.get(phase) != 2) || state.get(p_PS_ins_pressure) <= MAX_P_INSP){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_cont44_2(DataState state) {
        if (state.get(phase) != 3){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_basic_test(DataState state) {
        if (state.get(comm_sens_valves_ok) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_test0(DataState state) {
        if (state.get(Status) != 0){
            return 1.0;
        } else {
            return 0.0;
        }
    }

    public static double rho_test1(DataState state) {
        if (state.get(Status) != 1){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_test2(DataState state) {
        if (state.get(Status) != 2){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_test3(DataState state) {
        if (state.get(Status) != 3){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_test4(DataState state) {
        if (state.get(Status) != 4){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_test5(DataState state) {
        if (state.get(Status) != 5){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_test6(DataState state) {
        if (state.get(Status) != 6){
            return 1.0;
        } else {
            return 0.0;
        }
    }
    public static double rho_test7(DataState state) {
        if (state.get(Status) != 7){
            return 1.0;
        } else {
            return 0.0;
        }
    }


    // CONTROLLER OF the MLV

    public static Controller getController_MLV() {

        ControllerRegistry registry = new ControllerRegistry();

        //Start-up mode
        registry.set("P", Controller.ifThenElse(
                        DataState.equalsTo(b_powerOn, 1),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(Status, 3),
                                        new DataStateUpdate(b_powerOn, 0)),
                                registry.reference("P_checkcond")),
                        Controller.doTick(registry.reference("P"))
                )

        );

        registry.set("P_checkcond", Controller.ifThenElse(
                        DataState.equalsTo(conn_breathing, 1),
                        Controller.doTick(registry.reference("P_checkcond1")),
                        Controller.doTick(registry.reference("P_RepNotConnBreath"))
                )
        );

        registry.set("P_checkcond1", Controller.ifThenElse(
                        DataState.equalsTo(conn_air_supply, 1),
                        Controller.doTick(registry.reference("P_checkcond2")),
                        Controller.doTick(registry.reference("P_RepNotConnAir"))
                )
        );

        registry.set("P_checkcond2", Controller.ifThenElse(
                        DataState.equalsTo(conn_power_source, 1),
                        Controller.doTick(registry.reference("P_checkcond3")),
                        Controller.doTick(registry.reference("P_RepNotConnPower"))
                )
        );

        registry.set("P_checkcond3", Controller.ifThenElse(
                        DataState.equalsTo(conn_patient, 0),
                        Controller.doTick(registry.reference("P_start-up")),
                        Controller.doTick(registry.reference("P_RepConnPatient"))
                )
        );

        registry.set("P_RepNotConnBreath", Controller.doAction(
                        (rg, ds) -> List.of(new DataStateUpdate(conn_failToPowerOn, 1)),
                        registry.reference("P")
                )
        );
        registry.set("P_RepNotConnAir", Controller.doAction(
                        (rg, ds) -> List.of(new DataStateUpdate(conn_failToPowerOn, 2)),
                        registry.reference("P")
                )
        );
        registry.set("P_RepNotConnPower", Controller.doAction(
                        (rg, ds) -> List.of(new DataStateUpdate(conn_failToPowerOn, 3)),
                        registry.reference("P")
                )
        );
        registry.set("P_RepConnPatient", Controller.doAction(
                        (rg, ds) -> List.of(new DataStateUpdate(conn_failToPowerOn, 4)),
                        registry.reference("P")
                )
        );

        registry.set("P_start-up", Controller.ifThenElse(
                        DataState.equalsTo(b_powerOff, 1),
                        Controller.doTick(
                                //(rg, ds) -> List.of(new DataStateUpdate(b_powerOff, 0)),
                                registry.reference("P_final")
                        ),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(rr_pcv, RR_PCV),
                                        new DataStateUpdate(ie_pcv, IE_PCV),
                                        new DataStateUpdate(p_insp_pcv, P_INSP_PCV)
                                ),
                                Controller.ifThenElse(
                                        DataState.equalsTo(comm_sens_valves_ok, 1),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(nr_of_retries, 0)),
                                                registry.reference("P_start-up1")
                                        ),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(
                                                        new DataStateUpdate(nr_of_retries, 1 + ds.get(nr_of_retries))),
                                                registry.reference("P_retrySensor")
                                        )
                                )
                        )
                )
        );

        registry.set("P_retrySensor", Controller.ifThenElse(
                DataState.greaterOrEqualThan(nr_of_retries, 5),
                Controller.doAction(
                        (rg, ds) -> List.of(new DataStateUpdate(nr_of_retries, 0)),
                        registry.reference("P_failSafe")
                ),
                Controller.doTick(registry.reference("P_start-up"))
        ));

        registry.set("P_start-up1", Controller.ifThenElse(
                        DataState.equalsTo(b_powerOff, 1),
                        Controller.doTick(
                                //(rg, ds) -> List.of(new DataStateUpdate(b_powerOff, 0)),
                                registry.reference("P_final")
                        ),
                        Controller.ifThenElse(
                                DataState.equalsTo(conn_power_source, 1),
                                Controller.doAction(
                                        (rg, ds) -> List.of(new DataStateUpdate(nr_of_retries_p, 0)),
                                        registry.reference("P_start-up2")
                                ),
                                Controller.doAction(
                                        (rg, ds) -> List.of(
                                                new DataStateUpdate(nr_of_retries_p, 1 + ds.get(nr_of_retries_p))),
                                        registry.reference("P_retryPower")
                                )
                        )
                )
        );

        registry.set("P_retryPower", Controller.ifThenElse(
                DataState.greaterOrEqualThan(nr_of_retries_p, 5),
                Controller.doAction(
                        (rg, ds) -> List.of(new DataStateUpdate(nr_of_retries_p, 0)),
                        registry.reference("P_failSafe")
                ),
                Controller.doTick(registry.reference("P_start-up1"))
        ));

        registry.set("P_start-up2", Controller.ifThenElse(
                        DataState.equalsTo(b_powerOff, 1),
                        Controller.doTick(
                                //(rg, ds) -> List.of(new DataStateUpdate(b_powerOff, 0)),
                                registry.reference("P_final")
                        ),
                        Controller.ifThenElse(
                                DataState.equalsTo(comm_memory, 1).and(DataState.equalsTo(comm_cont_gui_ok, 1)),
                                Controller.doAction(
                                        (rg,ds) ->
                                                List.of(new DataStateUpdate(init_succ, 1)),
                                        registry.reference("P_self-test")
                                ),
                                Controller.doTick(registry.reference("P_start-upFail"))
                        )
                )
        );

        registry.set("P_start-upFail", Controller.doAction(
                        (rg, ds)-> List.of(new DataStateUpdate(init_succ, -1),
                                new DataStateUpdate(sys_out_of_service, 1), new DataStateUpdate(a_IN_valve, 0),
                                new DataStateUpdate(a_OUT_valve, 1)),
                        registry.reference("P_failSafe")
                )
        );

        //Self-test mode
        registry.set("P_self-test", Controller.doAction(
                        (rg,ds) -> List.of(new DataStateUpdate(init_succ, 0),
                                new DataStateUpdate(Status, 4)),
                        Controller.ifThenElse(
                                DataState.equalsTo(b_powerOff, 1),
                                Controller.doTick(
                                        //(rg, ds) -> List.of(new DataStateUpdate(b_powerOff, 0)),
                                        registry.reference("P_final")
                                ),
                                Controller.ifThenElse(
                                        (DataState.equalsTo(gui_req_res_ven, 1).or(
                                                DataState.equalsTo(power_switch_ok, 1).and(
                                                        DataState.equalsTo(no_leaks_breathing_circuit, 1)).and(
                                                        DataState.equalsTo(out_valve_ok, 1).and(
                                                                DataState.equalsTo(alarms_ok, 1))
                                                ))).and((DataState.equalsTo(fs, 1)).negate()),
                                        Controller.doTick(registry.reference("P_VentOff")
                                        ),
                                        Controller.doAction(
                                                (rg,ds) -> List.of(
                                                        new DataStateUpdate(selfTest_fail, 1),
                                                        new DataStateUpdate(sys_out_of_service, 1)),
                                                registry.reference("P_failSafe")
                                        )
                                )
                        )
                )
        );

        //Ventilation off mode
        registry.set("P_VentOff", Controller.doAction(
                        (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                new DataStateUpdate(a_OUT_valve, 1),
                                new DataStateUpdate(Status, 5),
                                new DataStateUpdate(phase, 0),
                                new DataStateUpdate(timer_insp, 0),
                                new DataStateUpdate(timer_exp, 0)),
                        Controller.ifThenElse(
                                DataState.equalsTo(b_powerOff, 1),
                                Controller.doTick(
                                        //(rg, ds) -> List.of(new DataStateUpdate(b_powerOff, 0)),
                                        registry.reference("P_final")
                                ),
                                Controller.ifThenElse(
                                        DataState.equalsTo(fs, 1),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                                        new DataStateUpdate(a_OUT_valve, 0)),
                                                registry.reference("P_failSafe")
                                        ),
                                        Controller.ifThenElse(
                                                DataState.equalsTo(conn_patient, 0),
                                                Controller.doAction(
                                                        (rg, ds) -> List.of(new DataStateUpdate(ind_var, 1)),
                                                        registry.reference("P_VentOff")),
                                                Controller.ifThenElse(
                                                        DataState.equalsTo(gui_req_change_mode_PCV, 1),
                                                        Controller.doTick(registry.reference("P_PCV")),
                                                        Controller.ifThenElse(
                                                                DataState.equalsTo(gui_req_change_mode_PSV, 1),
                                                                Controller.doTick(registry.reference("P_PSV")),
                                                                Controller.doTick(registry.reference("P_VentOff"))
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        //PCV general breathing mode
        registry.set("P_PCV", Controller.ifThenElse(
                DataState.equalsTo(b_powerOff, 1),
                Controller.doAction(
                        (rg, ds) -> List.of(//new DataStateUpdate(b_powerOff, 0),
                                new DataStateUpdate(cycle_done, 0)),
                        registry.reference("P_final")
                ),
                Controller.ifThenElse(
                        DataState.equalsTo(fs, 1),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                        new DataStateUpdate(a_OUT_valve, 1)),
                                registry.reference("P_failSafe")
                        ),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(timer_insp, 0),
                                        new DataStateUpdate(timer_PCV_insp, (60.0*ds.get(ie_pcv))/((ds.get(rr_pcv)) *(1+ds.get(ie_pcv)))),
                                        new DataStateUpdate(a_IN_valve, ds.get(p_insp_pcv)),
                                        new DataStateUpdate(a_OUT_valve, 0),
                                        new DataStateUpdate(Status, 1),
                                        new DataStateUpdate(phase, 1),
                                        new DataStateUpdate(phase_changed, 1),
                                        new DataStateUpdate(cycle_done, 0)),
                                registry.reference("P_PCV_insp")
                        )
                )

        ));

        registry.set("P_PCV_insp", Controller.ifThenElse(
                DataState.equalsTo(b_powerOff, 1),
                Controller.doAction(
                        (rg, ds) -> List.of(//new DataStateUpdate(b_powerOff, 0),
                                new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                        registry.reference("P_final")
                ),
                Controller.ifThenElse(
                        DataState.equalsTo(fs, 1),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                        new DataStateUpdate(a_OUT_valve, 1)),
                                registry.reference("P_failSafe")
                        ),
                        Controller.ifThenElse(
                                DataState.equalsTo(gui_req_stop_vent, 1),
                                Controller.doAction(
                                        (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp)+1)),
                                        registry.reference("P_VentOff")
                                ),
                                Controller.ifThenElse(
                                        DataState.greaterThan(s_PS_ins_pressure, MAX_P_INSP),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                                                registry.reference("P_PCV_exp0")
                                        ),
                                        Controller.ifThenElse(
                                                DataState.greaterThan(timer_PCV_insp, 0),
                                                Controller.doAction(
                                                        (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp) +1),
                                                                new DataStateUpdate(timer_PCV_insp, ds.get(timer_PCV_insp)-1)),
                                                        registry.reference("P_PCV_insp")
                                                ),
                                                Controller.ifThenElse(
                                                        DataState.equalsTo(gui_req_IP, 1),
                                                        Controller.doAction(
                                                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                                                        new DataStateUpdate(a_OUT_valve, 0),
                                                                        new DataStateUpdate(timer_IP, MAX_T_IP),
                                                                        new DataStateUpdate(phase, 2),
                                                                        new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                                                                registry.reference("P_IP_PCV")
                                                        ),
                                                        Controller.ifThenElse(
                                                                DataState.equalsTo(gui_req_RM, 1),
                                                                Controller.doAction(
                                                                        (rg, ds) -> List.of(new DataStateUpdate(timer_RM, RM_TIME),
                                                                                new DataStateUpdate(a_IN_valve, PRM),
                                                                                new DataStateUpdate(a_OUT_valve, 0),
                                                                                new DataStateUpdate(phase, 5),
                                                                                new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                                                                        registry.reference("P_RM")
                                                                ),
                                                                Controller.ifThenElse(
                                                                        DataState.equalsTo(switch_ready, 1),
                                                                        Controller.doAction(
                                                                                (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1),
                                                                                        new DataStateUpdate(switch_ready, 0),
                                                                                        new DataStateUpdate(gui_req_change_mode_PSV, 0)),
                                                                                registry.reference("P_PSV_exp0")
                                                                        ),
                                                                        Controller.doAction(
                                                                                (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                                                                                registry.reference("P_PCV_exp0")
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ));


        registry.set("P_IP_PCV", Controller.ifThenElse(
                DataState.equalsTo(b_powerOff, 1),
                Controller.doTick(
                        //(rg, ds) -> List.of(new DataStateUpdate(b_powerOff, 0)),
                        registry.reference("P_final")
                ),
                Controller.ifThenElse(
                        DataState.equalsTo(fs, 1),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                        new DataStateUpdate(a_OUT_valve, 1)),
                                registry.reference("P_failSafe")
                        ),
                        Controller.ifThenElse(
                                DataState.equalsTo(gui_req_IP, 0),
                                Controller.ifThenElse(
                                        DataState.equalsTo(switch_ready, 1),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp)+1),
                                                        new DataStateUpdate(switch_ready, 0),
                                                        new DataStateUpdate(gui_req_change_mode_PSV, 0)),
                                                registry.reference("P_PSV_exp0")
                                        ),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp)+1)),
                                                registry.reference("P_PCV_exp0")
                                        )
                                ),
                                Controller.ifThenElse(
                                        DataState.greaterThan(timer_IP, 0),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(timer_IP, ds.get(timer_IP)-1),
                                                        new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                                                registry.reference("P_IP_PCV")
                                        ),
                                        Controller.ifThenElse(
                                                DataState.equalsTo(switch_ready, 1),
                                                Controller.doAction(
                                                        (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp)+1),
                                                                new DataStateUpdate(switch_ready, 0),
                                                                new DataStateUpdate(gui_req_change_mode_PSV, 0)),
                                                        registry.reference("P_PSV_exp0")
                                                ),
                                                Controller.doAction(
                                                        (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                                                        registry.reference("P_PCV_exp0")
                                                )
                                        )
                                )
                        )
                )
        ));

        registry.set("P_RM", Controller.ifThenElse(
                DataState.equalsTo(b_powerOff, 1),
                Controller.doTick(
                        //(rg, ds) -> List.of(new DataStateUpdate(b_powerOff, 0)),
                        registry.reference("P_final")
                ),
                Controller.ifThenElse(
                        DataState.equalsTo(fs, 1),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                        new DataStateUpdate(a_OUT_valve, 1)),
                                registry.reference("P_failSafe")
                        ),
                        Controller.ifThenElse(
                                DataState.equalsTo(gui_req_RM, 0),
                                Controller.ifThenElse(
                                        DataState.equalsTo(switch_ready, 1),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(switch_ready, 0),
                                                        new DataStateUpdate(gui_req_change_mode_PSV, 0),
                                                        new DataStateUpdate(timer_insp, ds.get(timer_insp)+1)),
                                                registry.reference("P_PSV_exp0")
                                        ),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                                                registry.reference("P_PCV_exp0")
                                        )
                                ),
                                Controller.ifThenElse(
                                        DataState.greaterThan(timer_RM, 0),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(timer_RM, ds.get(timer_RM) - 1),
                                                        new DataStateUpdate(timer_insp, ds.get(timer_insp)-1)),
                                                registry.reference("P_RM")
                                        ),
                                        Controller.ifThenElse(
                                                DataState.equalsTo(switch_ready, 1),
                                                Controller.doAction(
                                                        (rg, ds) -> List.of(new DataStateUpdate(switch_ready, 0),
                                                                new DataStateUpdate(gui_req_change_mode_PSV, 0),
                                                                new DataStateUpdate(timer_insp, ds.get(timer_insp)+1)),
                                                        registry.reference("P_PSV_exp0")
                                                ),
                                                Controller.doAction(
                                                        (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                                                        registry.reference("P_PCV_exp0")
                                                )
                                        )

                                )
                        )
                )
        ));

        registry.set("P_PCV_exp0", Controller.ifThenElse(
                DataState.equalsTo(b_powerOff, 1),
                Controller.doTick(
                        //(rg, ds) -> List.of(new DataStateUpdate(b_powerOff, 0)),
                        registry.reference("P_final")
                ),
                Controller.ifThenElse(
                        DataState.equalsTo(fs, 1),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                        new DataStateUpdate(a_OUT_valve, 0)),
                                registry.reference("P_failSafe")
                        ),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(timer_PCV_exp, 60/(ds.get(rr_pcv) * (1+ds.get(ie_pcv)))),
                                        new DataStateUpdate(a_IN_valve, 0),
                                        new DataStateUpdate(a_OUT_valve, 1),
                                        new DataStateUpdate(timer_triggerDelay, TRIGGER_WINDOW_DELAY),
                                        new DataStateUpdate(phase, 3),
                                        new DataStateUpdate(phase_changed, 1),
                                        new DataStateUpdate(timer_exp, 0)),
                                registry.reference("P_PCV_exp")
                        )
                )
        ));

        registry.set("P_PCV_exp", Controller.ifThenElse(
                DataState.equalsTo(b_powerOff, 1),
                Controller.doTick(
                        //(rg, ds) -> List.of(new DataStateUpdate(b_powerOff, 0)),
                        registry.reference("P_final")
                ),
                Controller.ifThenElse(
                        DataState.equalsTo(fs, 1),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                        new DataStateUpdate(a_OUT_valve, 1)),
                                registry.reference("P_failSafe")
                        ),
                        Controller.ifThenElse(
                                DataState.equalsTo(gui_req_stop_vent, 1),
                                Controller.doAction(
                                        (rg, ds) -> List.of(new DataStateUpdate(timer_exp, ds.get(timer_exp) +1)),
                                        registry.reference("P_VentOff")
                                ),
                                Controller.ifThenElse(
                                        DataState.greaterThan(timer_triggerDelay, 0),
                                        Controller.ifThenElse(
                                                DataState.greaterThan(timer_PCV_exp, 0),
                                                Controller.doAction(
                                                        (rg, ds) -> List.of(new DataStateUpdate(timer_PCV_exp, ds.get(timer_PCV_exp) - 1),
                                                                new DataStateUpdate(timer_triggerDelay, ds.get(timer_triggerDelay)-1),
                                                                new DataStateUpdate(timer_exp, ds.get(timer_exp)+1)),
                                                        registry.reference("P_PCV_exp")
                                                ),
                                                Controller.ifThenElse(
                                                        DataState.equalsTo(gui_req_EP, 1),
                                                        Controller.doAction(
                                                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                                                        new DataStateUpdate(a_OUT_valve, 0),
                                                                        new DataStateUpdate(timer_EP, MAX_T_EP),
                                                                        new DataStateUpdate(phase, 4),
                                                                        new DataStateUpdate(timer_exp, ds.get(timer_exp)+1)),
                                                                registry.reference("P_EP_PCV")
                                                        ),
                                                        Controller.doAction(
                                                                (rg, ds) -> List.of(new DataStateUpdate(timer_exp, ds.get(timer_exp) +1),
                                                                        new DataStateUpdate(cycle_done, 1)),
                                                                registry.reference("P_PCV")
                                                        )
                                                )
                                        ),
                                        Controller.ifThenElse(
                                                DataState.greaterThan(drop_PAW, ITS_PCV),
                                                Controller.doAction(
                                                        (rg, ds) -> List.of(new DataStateUpdate(timer_exp, ds.get(timer_exp)+1),
                                                                new DataStateUpdate(cycle_done, 1)),
                                                        registry.reference("P_PCV")
                                                ),
                                                Controller.ifThenElse(
                                                        DataState.greaterThan(timer_PCV_exp, 0),
                                                        Controller.doAction(
                                                                (rg, ds) -> List.of(new DataStateUpdate(timer_PCV_exp, ds.get(timer_PCV_exp)-1),
                                                                        new DataStateUpdate(timer_exp, ds.get(timer_exp)+1)),
                                                                registry.reference("P_PCV_exp")
                                                        ),
                                                        Controller.ifThenElse(
                                                                DataState.equalsTo(gui_req_EP, 1),
                                                                Controller.doAction(
                                                                        (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                                                                new DataStateUpdate(a_OUT_valve, 1),
                                                                                new DataStateUpdate(timer_EP, MAX_T_EP),
                                                                                new DataStateUpdate(phase, 4),
                                                                                new DataStateUpdate(timer_exp, ds.get(timer_exp) +1)),
                                                                        registry.reference("P_EP_PCV")
                                                                ),
                                                                Controller.doAction(
                                                                        (rg, ds) -> List.of(new DataStateUpdate(timer_exp, ds.get(timer_exp) + 1),
                                                                                new DataStateUpdate(cycle_done, 1)),
                                                                        registry.reference("P_PCV")
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ));

        registry.set("P_EP_PCV", Controller.ifThenElse(
                DataState.equalsTo(b_powerOff, 1),
                Controller.doTick(
                        //(rg, ds) -> List.of(new DataStateUpdate(b_powerOff, 0)),
                        registry.reference("P_final")
                ),
                Controller.ifThenElse(
                        DataState.equalsTo(fs, 1),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                        new DataStateUpdate(a_OUT_valve, 1)),
                                registry.reference("P_failSafe")
                        ),
                        Controller.ifThenElse(
                                DataState.equalsTo(gui_req_EP, 0),
                                Controller.doAction(
                                        (rg, ds) -> List.of(new DataStateUpdate(timer_exp, ds.get(timer_exp) + 1),
                                                new DataStateUpdate(cycle_done, 1)),
                                        registry.reference("P_PCV")
                                ),
                                Controller.ifThenElse(
                                        DataState.greaterThan(timer_EP, 0),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(timer_EP, ds.get(timer_EP)-1),
                                                        new DataStateUpdate(timer_exp, ds.get(timer_exp) + 1)),
                                                registry.reference("P_EP_PCV")
                                        ),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(timer_exp, ds.get(timer_exp) + 1),
                                                        new DataStateUpdate(cycle_done, 1)),
                                                registry.reference("P_PCV")
                                        )
                                )
                        )
                )
        ));

        //PSV general breathing mode
        registry.set("P_PSV", Controller.ifThenElse(
                DataState.equalsTo(b_powerOff, 1),
                Controller.doAction(
                        (rg, ds) -> List.of(//new DataStateUpdate(b_powerOff, 0),
                                new DataStateUpdate(cycle_done, 0)),
                        registry.reference("P_final")
                ),
                Controller.ifThenElse(
                        DataState.equalsTo(fs, 1),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                        new DataStateUpdate(a_OUT_valve, 1)),
                                registry.reference("P_failSafe")
                        ),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(timer_insp, 0),
                                        new DataStateUpdate(timer_PSV_insp, MAX_INSP_TIME_PSV),
                                        new DataStateUpdate(a_IN_valve, P_INSP_PSV),
                                        new DataStateUpdate(a_OUT_valve, 0),
                                        new DataStateUpdate(Status, 2),
                                        new DataStateUpdate(phase, 1),
                                        new DataStateUpdate(phase_changed, 1),
                                        new DataStateUpdate(cycle_done, 0)),
                                registry.reference("P_PSV_insp")
                        )
                )

        ));

        registry.set("P_PSV_insp", Controller.ifThenElse(
                DataState.equalsTo(b_powerOff, 1),
                Controller.doAction(
                        (rg, ds) -> List.of(//new DataStateUpdate(b_powerOff, 0),
                                new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                        registry.reference("P_final")
                ),
                Controller.ifThenElse(
                        DataState.equalsTo(fs, 1),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                        new DataStateUpdate(a_OUT_valve, 1)),
                                registry.reference("P_failSafe")
                        ),
                        Controller.ifThenElse(
                                DataState.equalsTo(gui_req_stop_vent, 1),
                                Controller.doAction(
                                        (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp)+1)),
                                        registry.reference("P_VentOff")
                                ),
                                Controller.ifThenElse(
                                        (rg, ds) -> ds.get(s_PS_ins_pressure) > MAX_P_INSP || ds.get(s_Fl1_flow) <= ds.get(peak_flow)*ETS/100,
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                                                registry.reference("P_PSV_exp0")
                                        ),
                                        Controller.ifThenElse(
                                                DataState.greaterThan(timer_PSV_insp, 0),
                                                Controller.doAction(
                                                        (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp) +1),
                                                                new DataStateUpdate(timer_PSV_insp, ds.get(timer_PSV_insp)-1)),
                                                        registry.reference("P_PSV_insp")
                                                ),
                                                Controller.ifThenElse(
                                                        DataState.equalsTo(gui_req_IP, 1),
                                                        Controller.doAction(
                                                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                                                        new DataStateUpdate(a_OUT_valve, 0),
                                                                        new DataStateUpdate(timer_IP, MAX_T_IP),
                                                                        new DataStateUpdate(phase, 2),
                                                                        new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                                                                registry.reference("P_IP_PSV")
                                                        ),
                                                        Controller.ifThenElse(
                                                                DataState.equalsTo(gui_req_RM, 1),
                                                                Controller.doAction(
                                                                        (rg, ds) -> List.of(new DataStateUpdate(timer_RM, RM_TIME),
                                                                                new DataStateUpdate(a_IN_valve, PRM),
                                                                                new DataStateUpdate(a_OUT_valve, 0),
                                                                                new DataStateUpdate(phase, 5),
                                                                                new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                                                                        registry.reference("P_RM_PSV")
                                                                ),
                                                                Controller.doAction(
                                                                        (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                                                                        registry.reference("P_PSV_exp0")
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ));

        registry.set("P_IP_PSV", Controller.ifThenElse(
                DataState.equalsTo(b_powerOff, 1),
                Controller.doTick(
                        //(rg, ds) -> List.of(new DataStateUpdate(b_powerOff, 0)),
                        registry.reference("P_final")
                ),
                Controller.ifThenElse(
                        DataState.equalsTo(fs, 1),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                        new DataStateUpdate(a_OUT_valve, 1)),
                                registry.reference("P_failSafe")
                        ),
                        Controller.ifThenElse(
                                DataState.equalsTo(gui_req_IP, 0),
                                Controller.doAction(
                                        (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                                        registry.reference("P_PSV_exp0")
                                ),
                                Controller.ifThenElse(
                                        DataState.greaterThan(timer_IP, 0),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(timer_IP, ds.get(timer_IP)-1),
                                                        new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                                                registry.reference("P_IP_PSV")
                                        ),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                                                registry.reference("P_PSV_exp0")
                                        )
                                )
                        )
                )
        ));

        registry.set("P_RM_PSV", Controller.ifThenElse(
                DataState.equalsTo(b_powerOff, 1),
                Controller.doTick(
                        //(rg, ds) -> List.of(new DataStateUpdate(b_powerOff, 0)),
                        registry.reference("P_final")
                ),
                Controller.ifThenElse(
                        DataState.equalsTo(fs, 1),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                        new DataStateUpdate(a_OUT_valve, 1)),
                                registry.reference("P_failSafe")
                        ),
                        Controller.ifThenElse(
                                DataState.equalsTo(gui_req_RM, 0),
                                Controller.doAction(
                                        (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                                        registry.reference("P_PSV_exp0")
                                ),
                                Controller.ifThenElse(
                                        DataState.greaterThan(timer_RM, 0),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(timer_RM, ds.get(timer_RM) - 1),
                                                        new DataStateUpdate(timer_insp, ds.get(timer_insp)-1)),
                                                registry.reference("P_RM_PSV")
                                        ),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(timer_insp, ds.get(timer_insp) + 1)),
                                                registry.reference("P_PSV_exp0")
                                        )
                                )
                        )
                )
        ));

        registry.set("P_PSV_exp0", Controller.ifThenElse(
                DataState.equalsTo(b_powerOff, 1),
                Controller.doTick(
                        //(rg, ds) -> List.of(new DataStateUpdate(b_powerOff, 0)),
                        registry.reference("P_final")
                ),
                Controller.ifThenElse(
                        DataState.equalsTo(fs, 1),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                        new DataStateUpdate(a_OUT_valve, 0)),
                                registry.reference("P_failSafe")
                        ),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(timer_PSV_exp, T_APNEALAG),
                                        new DataStateUpdate(a_IN_valve, 0),
                                        new DataStateUpdate(a_OUT_valve, 1),
                                        new DataStateUpdate(timer_triggerDelay, 0.5*(ds.get(timer_insp))),
                                        new DataStateUpdate(phase, 3),
                                        new DataStateUpdate(phase_changed, 1),
                                        new DataStateUpdate(timer_exp, 0),
                                        new DataStateUpdate(Status, 2)),
                                registry.reference("P_PSV_exp")
                        )
                )
        ));

        registry.set("P_PSV_exp", Controller.ifThenElse(
                DataState.equalsTo(b_powerOff, 1),
                Controller.doTick(
                        //(rg, ds) -> List.of(new DataStateUpdate(b_powerOff, 0)),
                        registry.reference("P_final")
                ),
                Controller.ifThenElse(
                        DataState.equalsTo(fs, 1),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                        new DataStateUpdate(a_OUT_valve, 1)),
                                registry.reference("P_failSafe")
                        ),
                        Controller.ifThenElse(
                                DataState.equalsTo(gui_req_stop_vent, 1),
                                Controller.doAction(
                                        (rg, ds) -> List.of(new DataStateUpdate(timer_exp, ds.get(timer_exp) + 1)),
                                        registry.reference("P_VentOff")
                                ),
                                Controller.ifThenElse(
                                        DataState.greaterThan(timer_triggerDelay, 0),
                                        Controller.ifThenElse(
                                                DataState.greaterThan(timer_PSV_exp, 0),
                                                Controller.doAction(
                                                        (rg, ds) -> List.of(new DataStateUpdate(timer_PSV_exp, ds.get(timer_PSV_exp) - 1),
                                                                new DataStateUpdate(timer_triggerDelay, ds.get(timer_triggerDelay)-1),
                                                                new DataStateUpdate(timer_exp, ds.get(timer_exp)+1)),
                                                        registry.reference("P_PSV_exp")
                                                ),
                                                Controller.ifThenElse(
                                                        DataState.equalsTo(gui_req_EP, 1),
                                                        Controller.doAction(
                                                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                                                        new DataStateUpdate(a_OUT_valve, 0),
                                                                        new DataStateUpdate(timer_EP, MAX_T_EP),
                                                                        new DataStateUpdate(phase, 4),
                                                                        new DataStateUpdate(timer_exp, ds.get(timer_exp)+1)),
                                                                registry.reference("P_EP_PSV")
                                                        ),
                                                        Controller.doAction(
                                                                (rg, ds) -> List.of(new DataStateUpdate(timer_exp, ds.get(timer_exp) +1),
                                                                        new DataStateUpdate(cycle_done, 1),
                                                                        new DataStateUpdate(rr_pcv, RR_AP),
                                                                        new DataStateUpdate(p_insp_pcv, P_INSP_AP),
                                                                        new DataStateUpdate(ie_pcv, IE_AP)),
                                                                registry.reference("P_PCV")
                                                        )
                                                )
                                        ),
                                        Controller.ifThenElse(
                                                DataState.greaterThan(drop_PAW, ITS_PSV),
                                                Controller.doAction(
                                                        (rg, ds) -> List.of(new DataStateUpdate(timer_exp, ds.get(timer_exp)+1),
                                                                new DataStateUpdate(cycle_done, 1)),
                                                        registry.reference("P_PSV")
                                                ),
                                                Controller.ifThenElse(
                                                        DataState.greaterThan(timer_PSV_exp, 0),
                                                        Controller.doAction(
                                                                (rg, ds) -> List.of(new DataStateUpdate(timer_PSV_exp, ds.get(timer_PSV_exp)-1),
                                                                        new DataStateUpdate(timer_exp, ds.get(timer_exp)+1)),
                                                                registry.reference("P_PSV_exp")
                                                        ),
                                                        Controller.ifThenElse(
                                                                DataState.equalsTo(gui_req_EP, 1),
                                                                Controller.doAction(
                                                                        (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                                                                new DataStateUpdate(a_OUT_valve, 1),
                                                                                new DataStateUpdate(timer_EP, MAX_T_EP),
                                                                                new DataStateUpdate(phase, 4),
                                                                                new DataStateUpdate(timer_exp, ds.get(timer_exp) +1)),
                                                                        registry.reference("P_EP_PSV")
                                                                ),
                                                                Controller.doAction(
                                                                        (rg, ds) -> List.of(new DataStateUpdate(timer_exp, ds.get(timer_exp) + 1),
                                                                                new DataStateUpdate(cycle_done, 1),
                                                                                new DataStateUpdate(rr_pcv, RR_AP),
                                                                                new DataStateUpdate(p_insp_pcv, P_INSP_AP),
                                                                                new DataStateUpdate(ie_pcv, IE_AP)),
                                                                        registry.reference("P_PCV")
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ));

        registry.set("P_EP_PSV", Controller.ifThenElse(
                DataState.equalsTo(b_powerOff, 1),
                Controller.doTick(
                        //(rg, ds) -> List.of(new DataStateUpdate(b_powerOff, 0)),
                        registry.reference("P_final")
                ),
                Controller.ifThenElse(
                        DataState.equalsTo(fs, 1),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                                        new DataStateUpdate(a_OUT_valve, 1)),
                                registry.reference("P_failSafe")
                        ),
                        Controller.ifThenElse(
                                DataState.equalsTo(gui_req_EP, 0),
                                Controller.doAction(
                                        (rg, ds) -> List.of(new DataStateUpdate(timer_exp, ds.get(timer_exp) + 1),
                                                new DataStateUpdate(cycle_done, 1)),
                                        registry.reference("P_PSV")
                                ),
                                Controller.ifThenElse(
                                        DataState.greaterThan(timer_EP, 0),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(timer_EP, ds.get(timer_EP)-1),
                                                        new DataStateUpdate(timer_exp, ds.get(timer_exp) + 1)),
                                                registry.reference("P_EP_PSV")
                                        ),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(timer_exp, ds.get(timer_exp) + 1),
                                                        new DataStateUpdate(cycle_done, 1),
                                                        new DataStateUpdate(rr_pcv, RR_AP),
                                                        new DataStateUpdate(p_insp_pcv, P_INSP_AP),
                                                        new DataStateUpdate(ie_pcv, IE_AP)),
                                                registry.reference("P_PCV")
                                        )
                                )
                        )
                )
        ));

        //Final mode
        registry.set("P_final", Controller.doAction(
                (rg, ds) -> List.of(new DataStateUpdate(Status, 7),
                        new DataStateUpdate(phase, 0),
                        new DataStateUpdate(counter_cycles, 0)),
                //TODO: possibly store paramaters?
                registry.reference("P")
        ));

        //Fail-safe mode
        registry.set("P_failSafe", Controller.doAction(
                (rg, ds) -> List.of(new DataStateUpdate(a_IN_valve, 0),
                        new DataStateUpdate(a_OUT_valve, 1),
                        new DataStateUpdate(Status, 6),
                        new DataStateUpdate(init_succ, 0),
                        new DataStateUpdate(selfTest_fail, 0),
                        new DataStateUpdate(phase, 0),
                        new DataStateUpdate(phase_changed, 1),
                        new DataStateUpdate(fs, 1)),
                registry.reference("P_failSafeI")
        ));

        registry.set("P_failSafeI", Controller.ifThenElse(
                DataState.equalsTo(b_powerOff, 1),
                Controller.doTick(
                        //(rg, ds) -> List.of(new DataStateUpdate(b_powerOff, 0)),
                        registry.reference("P_final")
                ),
                Controller.doTick(registry.reference("P_failSafeI"))
        ));

        return registry.reference("P");

    }


    //  CONTROLLER OF the alarm component
    public static Controller getController_alarm() {

        ControllerRegistry registry = new ControllerRegistry();

        registry.set("P_alarms", Controller.ifThenElse(
                DataState.equalsTo(Status, 1).or(DataState.equalsTo(Status, 2)),
                Controller.ifThenElse(
                        DataState.greaterThan(s_temp, 75).or(
                                DataState.greaterThan(s_PS_ins_pressure, MAX_P_INSP)).or(
                                DataState.greaterThan(s_PS_exp_pressure, MAX_PEEP).or(
                                        DataState.greaterThan(IE_toolow_counter, 4))).or(
                                DataState.equalsTo(phase_changed, 1).and(DataState.equalsTo(phase, 3).and(
                                        (DataState.equalsTo(a_IN_valve, 0)).negate()
                                ))
                        ).or(
                                DataState.equalsTo(phase_changed, 1).and(DataState.equalsTo(phase, 1).and(
                                        (DataState.equalsTo(a_IN_valve, 0))
                                ))
                        ).or(
                                DataState.equalsTo(phase_changed, 1).and(DataState.equalsTo(phase, 1).and(
                                        (DataState.equalsTo(a_OUT_valve, 0)).negate()
                                ))
                        ).or(
                                DataState.equalsTo(phase_changed, 1).and(DataState.equalsTo(phase, 3).and(
                                        (DataState.equalsTo(a_OUT_valve, 1)).negate()
                                ))
                        ).or(DataState.equalsTo(conn_air_supply, 0)),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(fs, 1),
                                        new DataStateUpdate(a_LED, 1),
                                        new DataStateUpdate(a_IN_valve, 0),
                                        new DataStateUpdate(a_OUT_valve, 1)),
                                registry.reference("Idle_Alarms")
                        ),
                        Controller.ifThenElse(
                                DataState.greaterOrEqualThan(nr_of_retries_p, 5),
                                Controller.doAction(
                                        (rg, ds) -> List.of(new DataStateUpdate(fs, 1),
                                                new DataStateUpdate(a_LED, 1),
                                                new DataStateUpdate(a_IN_valve, 0),
                                                new DataStateUpdate(a_OUT_valve, 1),
                                                new DataStateUpdate(nr_of_retries_p, 0)),
                                        registry.reference("Idle_Alarms")
                                ),
                                Controller.ifThenElse(
                                        DataState.equalsTo(conn_power_source, 0),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(nr_of_retries_p, ds.get(nr_of_retries_p) +1)),
                                                registry.reference("P_Alarms")
                                        ),
                                        Controller.ifThenElse(
                                                DataState.greaterThan(counter_cycles, 0),
                                                Controller.ifThenElse(
                                                        (rg, ds) -> ds.get(Status) == 1,
                                                        Controller.ifThenElse(
                                                                (rg, ds) -> ds.get(s_PS_ins_pressure) < ((double) MIN_P_INSP /100*ds.get(p_insp_pcv)) ||
                                                                        ds.get(comm_sens_valves_ok) == 0
                                                                        || ds.get(V_E) < MIN_V_E || ds.get(s_PS_exp_pressure) < MIN_PEEP ||
                                                                        ds.get(RR_ms) > MAX_RR || (ds.get(RR_ms) < MIN_RR && ds.get(RR_ms) != 0) || ds.get(timer_PSV_exp) < 0
                                                                        || ds.get(comm_cont_gui_ok) == 0 || ds.get(V_E) > MAX_V_E || ds.get(s_OS) > PM_A_GB_FiO2 + 3 ||
                                                                        ds.get(s_OS) < PM_A_GB_FiO2 - 3
                                                                ,
                                                                Controller.doAction(
                                                                        (rg, ds) -> List.of(new DataStateUpdate(a_LED, 1)),
                                                                        registry.reference("P_alarms")
                                                                ),
                                                                Controller.doTick(registry.reference("P_alarms"))
                                                        ),
                                                        Controller.ifThenElse(
                                                                (rg, ds) -> ds.get(s_PS_ins_pressure) < ((double) MIN_P_INSP /100*ds.get(P_INSP_PSV)) || ds.get(comm_sens_valves_ok) == 0
                                                                        || ds.get(V_E) < MIN_V_E || ds.get(s_PS_exp_pressure) < MIN_PEEP ||
                                                                        ds.get(RR_ms) > MAX_RR || (ds.get(RR_ms) < MIN_RR && ds.get(RR_ms) != 0) || ds.get(timer_PSV_exp) < 0
                                                                        || ds.get(comm_cont_gui_ok) == 0 || ds.get(V_E) > MAX_V_E || ds.get(s_OS) > PM_A_GB_FiO2 + 3 ||
                                                                        ds.get(s_OS) < PM_A_GB_FiO2 - 3
                                                                ,
                                                                Controller.doAction(
                                                                        (rg, ds) -> List.of(new DataStateUpdate(a_LED, 1)),
                                                                        registry.reference("P_alarms")
                                                                ),
                                                                Controller.doTick(registry.reference("P_alarms"))
                                                        )
                                                ),
                                                //else (counter_cycles == 0)
                                                Controller.ifThenElse(
                                                        (rg, ds) -> ds.get(Status) == 1,
                                                        Controller.ifThenElse(
                                                                (rg, ds) -> ds.get(s_PS_ins_pressure) < ((double) MIN_P_INSP /100*ds.get(p_insp_pcv)) || ds.get(comm_sens_valves_ok) == 0
                                                                        || ds.get(s_PS_exp_pressure) < MIN_PEEP || ds.get(timer_PSV_exp) < 0
                                                                        || ds.get(comm_cont_gui_ok) == 0 || ds.get(s_OS) > PM_A_GB_FiO2 + 3 || ds.get(s_OS) < PM_A_GB_FiO2 - 3
                                                                ,
                                                                Controller.doAction(
                                                                        (rg, ds) -> List.of(new DataStateUpdate(a_LED, 1)),
                                                                        registry.reference("P_alarms")
                                                                ),
                                                                Controller.doTick(registry.reference("P_alarms"))
                                                        ),
                                                        Controller.ifThenElse(
                                                                (rg, ds) -> ds.get(s_PS_ins_pressure) < ((double) MIN_P_INSP /100*P_INSP_PSV) || ds.get(comm_sens_valves_ok) == 0
                                                                        || ds.get(s_PS_exp_pressure) < MIN_PEEP || ds.get(timer_PSV_exp) < 0
                                                                        || ds.get(comm_cont_gui_ok) == 0 || ds.get(s_OS) > PM_A_GB_FiO2 + 3 || ds.get(s_OS) < PM_A_GB_FiO2 - 3
                                                                ,
                                                                Controller.doAction(
                                                                        (rg, ds) -> List.of(new DataStateUpdate(a_LED, 1)),
                                                                        registry.reference("P_alarms")
                                                                ),
                                                                Controller.doTick(registry.reference("P_alarms"))
                                                        )
                                                )
                                        )
                                )
                        )
                ),
                Controller.ifThenElse(
                        DataState.greaterThan(s_temp, 75).or(DataState.equalsTo(conn_air_supply, 0)),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(fs, 1),
                                        new DataStateUpdate(a_LED, 1),
                                        new DataStateUpdate(a_IN_valve, 0),
                                        new DataStateUpdate(a_OUT_valve, 1)),
                                registry.reference("Idle_Alarms")
                        ),
                        Controller.ifThenElse(
                                DataState.greaterOrEqualThan(nr_of_retries_p, 5),
                                Controller.doAction(
                                        (rg, ds) -> List.of(new DataStateUpdate(fs, 1),
                                                new DataStateUpdate(a_LED, 1),
                                                new DataStateUpdate(a_IN_valve, 0),
                                                new DataStateUpdate(a_OUT_valve, 1),
                                                new DataStateUpdate(nr_of_retries_p, 0)),
                                        registry.reference("Idle_Alarms")
                                ),
                                Controller.ifThenElse(
                                        DataState.equalsTo(conn_power_source, 0),
                                        Controller.doAction(
                                                (rg, ds) -> List.of(new DataStateUpdate(nr_of_retries_p, ds.get(nr_of_retries_p) +1)),
                                                registry.reference("P_Alarms")
                                        ),
                                        Controller.ifThenElse(
                                                DataState.equalsTo(comm_sens_valves_ok, 0).or(DataState.equalsTo(comm_cont_gui_ok, 0)),
                                                Controller.doAction(
                                                        (rg, ds) -> List.of(new DataStateUpdate(a_LED, 1)),
                                                        registry.reference("P_alarms")
                                                ),
                                                Controller.doTick(registry.reference("P_alarms"))
                                        )
                                )
                        )
                )
        ));

        registry.set("Idle_Alarms", Controller.ifThenElse(
                DataState.equalsTo(b_powerOff, 1),
                Controller.doTick(registry.reference("P_Alarms_final")),
                Controller.doTick(registry.reference("Idle_Alarms"))
        ));

        registry.set("P_Alarms_final", Controller.ifThenElse(
                DataState.equalsTo(b_powerOn, 1),
                Controller.doAction(
                        (rg, ds) -> List.of(new DataStateUpdate(Status, 0),
                                new DataStateUpdate(phase, 0),
                                new DataStateUpdate(a_LED, 0),
                                new DataStateUpdate(b_powerOn, 0)),
                        registry.reference("P_alarms")
                ),
                Controller.doAction(
                        (rg, ds) -> List.of(new DataStateUpdate(Status, 7),
                                new DataStateUpdate(phase, 0),
                                new DataStateUpdate(a_LED, 0)),
                        //TODO: store paramaters?
                        registry.reference("P_alarms_final")
                )
        ));


        return registry.reference("P_alarms");
    }

    //  CONTROLLER OF the switch component
    public static Controller getController_switch() {

        ControllerRegistry registry = new ControllerRegistry();

        registry.set("P_switch", Controller.ifThenElse(
                DataState.equalsTo(gui_req_change_mode_PSV, 1),
                Controller.ifThenElse(
                        DataState.equalsTo(gui_param_psv_ok, 1),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(switch_ready, 1)),
                                registry.reference("P_switch")
                        ),
                        Controller.doAction(
                                (rg, ds) -> List.of(new DataStateUpdate(switch_ready, 0)),
                                registry.reference("P_switch"))
                ),
                Controller.doAction(
                        (rg, ds) -> List.of(new DataStateUpdate(switch_ready, 0)),
                        registry.reference("P_switch"))
        ));

        return registry.reference("P_switch");
    }

    // ENVIRONMENT FUNCTION

    public static List<DataStateUpdate> getEnvironmentUpdates(RandomGenerator rg, DataState state) {
        List<DataStateUpdate> updates = new LinkedList<>();
        //UPDATING PHYSICAL VALUES
        double new_battery_level = 0.0;
        if (state.get(Status) != 1 && state.get(Status) != 2) {
            new_battery_level = state.get(s_battery_level)- 0.1;
        } else {
            new_battery_level = state.get(s_battery_level) -0.5;
        }
        updates.add(new DataStateUpdate(s_battery_level, new_battery_level));

        double new_pressure_in = 0.0;
        if ((state.get(Status) == 1 || state.get(Status) == 2)) {
            if (state.get(a_IN_valve) != 0) {
                new_pressure_in = state.get(a_IN_valve) + PM_A_PEEP_VALVE;
            } else if (state.get(phase) !=2 && state.get(phase) != 4) {
                new_pressure_in = Math.max(PM_A_PEEP_VALVE, state.get(p_PS_ins_pressure)-0.25*state.get(peak_P_insp));
            } else {
                new_pressure_in = state.get(p_PS_ins_pressure);
            }
        } else {
            new_pressure_in = Math.max(0.0, state.get(p_PS_ins_pressure)*0.7 - 0.5);
        }

        updates.add(new DataStateUpdate(p_PS_ins_pressure, new_pressure_in));

        double new_pressure_out = 0.0;
        if ((state.get(Status) == 1 || state.get(Status) == 2)) {
            if (state.get(a_OUT_valve) == 0) {
                new_pressure_out = PM_A_PEEP_VALVE;
            } else if (state.get(timer_exp) == 0) {
                new_pressure_out = Math.max(PM_A_PEEP_VALVE, PM_A_PEEP_VALVE + 0.2*state.get(peak_P_insp));
            } else {
                new_pressure_out = Math.max(PM_A_PEEP_VALVE, state.get(p_PS_exp_pressure) - 0.15*state.get(peak_P_insp));
            }
        } else {
            new_pressure_out = Math.max(0.0, state.get(p_PS_exp_pressure)*0.7 - 0.5);
        }

        updates.add(new DataStateUpdate(p_PS_exp_pressure, new_pressure_out));

        updates.add(new DataStateUpdate(p_GB_pressure, PM_A_GB_PRESSURE));
        updates.add(new DataStateUpdate(p_OS, PM_A_GB_FiO2));

        double new_flow_in = 0.0;
        if ((state.get(Status) == 1 || state.get(Status) == 2)) {
            if (state.get(timer_insp) == 0) {
                new_flow_in = HIGH_FLOW;
            } else if (state.get(phase) == 1) {
                new_flow_in = Math.max(0, 0.85*state.get(p_Fl1_flow)-0.5);
            } else {
                new_flow_in = 0;
            }
        } else {
            new_flow_in = 0.0;
        }

        updates.add(new DataStateUpdate(p_Fl1_flow, new_flow_in));

        double new_flow_out = 0.0;
        if ((state.get(Status) == 1 || state.get(Status) == 2)) {
            if (state.get(timer_exp) == 0) {
                new_flow_out = -HIGH_FLOW;
            } else if (state.get(phase) == 3) {
                new_flow_out = Math.min(0, 0.7*state.get(p_Fl2_flow)+0.5);
            } else {
                new_flow_out = 0;
            }
        } else {
            new_flow_out = Math.min(0.0, state.get(p_Fl2_flow)*0.7 + 0.5);
        }

        updates.add(new DataStateUpdate(p_Fl2_flow, new_flow_out));

        //store previous_PAW
        updates.add(new DataStateUpdate(previous_PAW, state.get(s_PS_ins_pressure)));
        // Note: this update is not yet known, when I 'get' it later, this is why later on I use 'state.get(s_PS_ins_pressure' instead of state.get(previous_PAW)

        //UPDATING SENSOR VALUES
        double u = 1;
        double t = Math.random()*2*u - u;

        double uu = 0.3;
        double tt = Math.random()*2*uu - uu;

        updates.add(new DataStateUpdate(s_GB_pressure, state.get(p_GB_pressure)+t));
        t = Math.random()*2*u - u;
        updates.add(new DataStateUpdate(s_PS_ins_pressure, new_pressure_in+t));
        updates.add(new DataStateUpdate(drop_PAW, (state.get(s_PS_ins_pressure) - (new_pressure_in+t))/(1)));//TODO: update later to divide by time^2
        t = Math.random()*2*u - u;
        updates.add(new DataStateUpdate(s_PS_exp_pressure, new_pressure_out+t));
        t = Math.random()*2*u - u;
        updates.add(new DataStateUpdate(s_OS, state.get(p_OS)+t));
        updates.add(new DataStateUpdate(s_Fl2_flow, new_flow_out+tt));
        tt = Math.random()*2*uu - uu;
        updates.add(new DataStateUpdate(s_Fl1_flow, new_flow_in+tt));
        if (state.get(p_GB_pressure) == 0) {
            updates.add(new DataStateUpdate(s_GB_pressure, 0.0));
        }
        if (state.get(p_OS) == 0) {
            updates.add(new DataStateUpdate(s_OS, 0.0));
        }
        if (new_flow_out == 0) {
            updates.add(new DataStateUpdate(s_Fl2_flow, 0.0));
        }
        if (new_flow_in == 0) {
            updates.add(new DataStateUpdate(s_Fl1_flow, 0.0));
        }
        if (new_pressure_in == 0) {
            updates.add(new DataStateUpdate(s_PS_ins_pressure, 0.0));
        }
        if (new_pressure_out == 0) {
            updates.add(new DataStateUpdate(s_PS_exp_pressure, 0.0));
        }

        double uuu = 0.1;
        double ttt = Math.random()*2*uuu - uuu;
        updates.add(new DataStateUpdate(s_temp, state.get(p_temp)+ttt));
        //no inaccuracies in power source, fan, battery level
        updates.add(new DataStateUpdate(s_power_source, state.get(p_power_source)));
        updates.add(new DataStateUpdate(s_fan, state.get(p_fan)));

        //ENVIRONMENT ENVIRONMENT VARIABLES FORCED
        if (state.get(init_succ) == 1) {
            updates.add(new DataStateUpdate(conn_patient, 1));
        }
        if (state.get(Status) == 7) {//in the final state, the patient should be disconnected again
            updates.add(new DataStateUpdate(conn_patient, 0));
            //updates.add(new DataStateUpdate(gui_req_change_mode_PCV, 1)); //we want to be able to start breathing again
            updates.add(new DataStateUpdate(gui_req_change_mode_PSV, 0));
        }

        //COMMUNICATING VALUES / COMPUTING VARIABLES
        if (state.get(cycle_done) == 1){
            updates.add(new DataStateUpdate(counter_cycles, state.get(counter_cycles)+1));
            //updates.add(new DataStateUpdate(cycle_done, 0));
        }
        double new_RR = 0.0;
        double new_IE = 0.0;
        if (state.get(cycle_done) == 1) {
            new_RR = 60/(state.get(timer_insp) + state.get(timer_exp));
            new_IE = state.get(timer_insp)/state.get(timer_exp);
        } else if (state.get(Status) != 1 && state.get(Status) != 2) {
            new_RR = 0.0;
            new_IE = 0.0;
        } else {
            new_RR = state.get(RR_ms);
            new_IE = state.get(IE_ms);
        }
        updates.add(new DataStateUpdate(RR_ms, new_RR));
        updates.add(new DataStateUpdate(IE_ms, new_IE));

        double new_IE_counter = 0.0;
        if (state.get(cycle_done) == 1 && state.get(timer_insp)/state.get(timer_exp) < 0.01) {
            new_IE_counter = state.get(IE_toolow_counter) + 1;
        } else if (state.get(cycle_done) == 1) {
            new_IE_counter = 0;
        } else { //we're not in a new breathing cycle so should do nothing
            new_IE_counter = state.get(IE_toolow_counter);
        }
        updates.add(new DataStateUpdate(IE_toolow_counter, new_IE_counter));

        double new_V_tidal = 0.0;
        double new_V_tidal_prev = 0.0;
        if (state.get(Status) == 1 || state.get(Status) == 2) {
            if (state.get(phase) == 1 || state.get(phase) == 2) {
                new_V_tidal = state.get(V_tidal) + (new_flow_in+tt)*1; //TODO: update later 1 to be the time step of 1 iteration
                new_V_tidal_prev = state.get(V_tidal_prev);
            } else if (state.get(phase_changed) == 1 && state.get(phase) == 3) {
                new_V_tidal = 0;
                new_V_tidal_prev = state.get(V_tidal);
            } else {
                new_V_tidal = state.get(V_tidal);
                new_V_tidal_prev = state.get(V_tidal_prev);
            }
        } else {
            new_V_tidal = 0.0;
            new_V_tidal_prev = state.get(V_tidal);
        }

        updates.add(new DataStateUpdate(V_tidal, new_V_tidal));
        updates.add(new DataStateUpdate(V_tidal_prev, new_V_tidal_prev));


        double new_V_E = 0.0;
        if (state.get(cycle_done) == 1) {
            new_V_E = (state.get(V_tidal_prev) * 60/(state.get(timer_insp) + state.get(timer_exp)))/1000;
        } else if (state.get(Status) != 1 && state.get(Status) != 2) {
            new_V_E = 0.0;
        } else {
            new_V_E = state.get(V_E);
        }
        updates.add(new DataStateUpdate(V_E, new_V_E));

        double new_peak_flow = 0.0;
        if (state.get(peak_flow) < state.get(s_Fl1_flow)) {
            new_peak_flow = state.get(s_Fl1_flow);
        } else if (state.get(phase_changed) == 1 && state.get(phase) == 1 || (state.get(Status) != 1 & state.get(Status) != 2)) {
            new_peak_flow = 0;
        } else {
            new_peak_flow = state.get(peak_flow);
        }
        updates.add(new DataStateUpdate(peak_flow, new_peak_flow));

        double new_peak_flow_p = 0.0;
        if (state.get(p_peak_flow) < state.get(p_Fl1_flow)) {
            new_peak_flow_p = state.get(p_Fl1_flow);
        } else if (state.get(phase_changed) == 1 && state.get(phase) == 1 || (state.get(Status) != 1 & state.get(Status) != 2)) {
            new_peak_flow_p = 0;
        } else {
            new_peak_flow_p = state.get(p_peak_flow);
        }
        updates.add(new DataStateUpdate(p_peak_flow, new_peak_flow_p));


        double new_peak_P_insp = 0.0;
        if (state.get(peak_P_insp) < state.get(s_PS_ins_pressure)) {
            new_peak_P_insp = state.get(s_PS_ins_pressure);
        } else if (state.get(phase_changed) == 1 && state.get(phase) == 1 || (state.get(Status) != 1 & state.get(Status) != 2)) {
            new_peak_P_insp = 0;
        } else {
            new_peak_P_insp = state.get(peak_P_insp);
        }
        updates.add(new DataStateUpdate(peak_P_insp, new_peak_P_insp));

        double new_phase_change = 0.0;
        if (state.get(phase_changed) == 1) {
            new_phase_change = 0;
        } else {
            new_phase_change = state.get(phase_changed);
        }
        updates.add(new DataStateUpdate(phase_changed, new_phase_change));

        updates.add(new DataStateUpdate(p_drop_PAW, (state.get(p_PS_ins_pressure) - new_pressure_in)/(1)));

        //Non-deterministic environment updates
        //TODO: think about when to reset the gui_req values
        /*
        if (state.get(counter_cycles) == 2 && state.get(p_insp_pcv) != P_INSP_AP) {
            updates.add(new DataStateUpdate(gui_req_change_mode_PSV, 1));
            updates.add(new DataStateUpdate(gui_param_psv_ok, 1));
        }

        if (state.get(counter_cycles) == 4 && state.get(cycle_done) == 1) {
            updates.add(new DataStateUpdate(b_powerOff, 1));
            updates.add(new DataStateUpdate(gui_req_change_mode_PCV, 0));
            updates.add(new DataStateUpdate(gui_req_change_mode_PSV, 0));
        }*/

        if (state.get(p_PS_ins_pressure) == 0 && (state.get(Status) == 0 || state.get(Status) == 7) && rg.nextDouble() < 0.3) {
            updates.add(new DataStateUpdate(b_powerOn, 1));
        }

        //ensure that the led-light turns of after a random amount of time
        if (state.get(a_LED) == 1 && state.get(alarm_counter) == -1) { //we've not initialized the counter yet
            updates.add(new DataStateUpdate(alarm_counter, Math.ceil(rg.nextDouble()*10)));
        } else if (state.get(a_LED) == 1 && state.get(alarm_counter) > 0) {
            updates.add(new DataStateUpdate(alarm_counter, state.get(alarm_counter) - 1));
        } else if (state.get(a_LED) == 1) {
            //so the counter has run out
            updates.add(new DataStateUpdate(a_LED, 0));
            updates.add(new DataStateUpdate(alarm_counter, -1));
        }

        //ensure that there is some chance to press the power off butten when we're in fail-safe mode
        if (state.get(Status) == 6 && rg.nextDouble() < 0.1) {
            updates.add(new DataStateUpdate(b_powerOff, 1));
        }
        //ensure that there is a slight chance that we request to stop ventilation when in PCV or PSV mode
        if ((state.get(Status) == 1 || state.get(Status) == 2) && rg.nextDouble() < 0.01){
            updates.add(new DataStateUpdate(gui_req_stop_vent, 1));
        }
        //ensure that at some point the doctor requests to change to PSV mode (while in PCV)
        if (state.get(Status) == 1 && rg.nextDouble() < 0.04){
            updates.add(new DataStateUpdate(gui_req_change_mode_PSV, 1));
            updates.add(new DataStateUpdate(gui_req_change_mode_PCV, 0));
        }
        if (state.get(Status) == 5) {
            double choose_mode = rg.nextDouble();
            if (choose_mode < 0.5){
                updates.add(new DataStateUpdate(gui_req_change_mode_PCV, 1));
                updates.add(new DataStateUpdate(gui_req_change_mode_PSV, 0));
            } else if (choose_mode < 0.83){
                updates.add(new DataStateUpdate(gui_req_change_mode_PCV, 0));
                updates.add(new DataStateUpdate(gui_req_change_mode_PSV, 1));
            } else {
                updates.add(new DataStateUpdate(gui_req_change_mode_PCV, 0));
                updates.add(new DataStateUpdate(gui_req_change_mode_PSV, 0));
            }
        }
        //ensure that, if there is a request to switch to PSV, the PSV parameters are confirmed after a random
        //                      CLOCK amount of time (where CLOCK should be smaller than for resetting the request)
        if (state.get(gui_req_change_mode_PSV) == 1 && state.get(psv_param_counter) == -1) { //we've not initialized the counter yet
            updates.add(new DataStateUpdate(psv_param_counter, Math.ceil(rg.nextDouble()*5) + 1));
        } else if (state.get(gui_req_change_mode_PSV) == 1 && state.get(psv_param_counter) > 0) {
            updates.add(new DataStateUpdate(psv_param_counter, state.get(psv_param_counter) - 1));
        } else if (state.get(gui_req_change_mode_PSV) == 1) {
            //so the counter has run out
            updates.add(new DataStateUpdate(gui_param_psv_ok, 1));
            updates.add(new DataStateUpdate(psv_param_counter, -1));
        } else {// the case that gui_req_change_mode_psv is 0
            updates.add(new DataStateUpdate(gui_param_psv_ok, 0));
        }

        //ensure that the request to switch to PSV turns off after a random CLOCK amount of time
        if (state.get(gui_req_change_mode_PSV) == 1 && state.get(req_counter) == -1) { //we've not initialized the counter yet
            updates.add(new DataStateUpdate(req_counter, Math.ceil(rg.nextDouble()*10) + 5));
        } else if (state.get(gui_req_change_mode_PSV) == 1 && state.get(req_counter) > 0) {
            updates.add(new DataStateUpdate(req_counter, state.get(req_counter) - 1));
        } else if (state.get(gui_req_change_mode_PSV) == 1) {
            //so the counter has run out
            updates.add(new DataStateUpdate(gui_req_change_mode_PSV, 0));
            updates.add(new DataStateUpdate(req_counter, -1));
        }

        //ensure that the power-on request can not happen in the first 7 iterations after the system has been powered off
        if (state.get(b_powerOff) == 1 && state.get(on_counter) == -1) {//we've not initialized the counter yet
            updates.add(new DataStateUpdate(on_counter, Math.ceil(rg.nextDouble()*2) + 6));
            updates.add(new DataStateUpdate(b_powerOn, 0));
        } else if (state.get(on_counter) > 0) {
            updates.add(new DataStateUpdate(on_counter, state.get(on_counter) - 1));
            updates.add(new DataStateUpdate(b_powerOn, 0)); // enforce power-on button can not be pressed yet
        } else if (state.get(on_counter) == 0) {
            //the counter has run out
            updates.add(new DataStateUpdate(on_counter, -1));
        }

        if (state.get(b_powerOff) == 1) {
            updates.add(new DataStateUpdate(b_powerOff, 0));
        }
        if (state.get(b_powerOn) == 1) {
            updates.add(new DataStateUpdate(b_powerOn, 0));
        }

        return updates;
    }

    // PERTURBATIONS
    private static Perturbation get_Sav_6Perturbation_t3_2() {
        return new AtomicPerturbation(0, Main::sav_6Perturbation_t3_2);
    }

    private static DataState sav_6Perturbation_t3_2(RandomGenerator rg, DataState state) {
        List<DataStateUpdate> updates = new LinkedList<>();
        if (state.get(RR_ms) != 0) {
            updates.add(new DataStateUpdate(RR_ms, MIN_RR- 1));
        }
        return state.apply(updates);
    }

    private static Perturbation get_Sav_16Perturbation() {
        return new AtomicPerturbation(0, Main::sav_16Perturbation);
    }
    private static Perturbation get_Sav_16Perturbation_sim() {
        return new AtomicPerturbation(25, Main::sav_16Perturbation);
    }

    private static DataState sav_16Perturbation(RandomGenerator rg, DataState state) {
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(V_E, 1.5));
        updates.add(new DataStateUpdate(test_per, 1));
        return state.apply(updates);
    }

    private static Perturbation get_cont_15Perturbation() {
        return new AtomicPerturbation(0, Main::cont_15Perturbation);
    }

    private static DataState cont_15Perturbation(RandomGenerator rg, DataState state) {
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(comm_sens_valves_ok, 0));
        return state.apply(updates);
    }

    private static Perturbation get_test_Perturbation() {
        return new AtomicPerturbation(0, Main::test_Perturbation);
    }

    private static DataState test_Perturbation(RandomGenerator rg, DataState state) {
        List<DataStateUpdate> updates = new LinkedList<>();
        updates.add(new DataStateUpdate(comm_sens_valves_ok, 0));
        return state.apply(updates);
    }


    // INITIALISATION OF DATA STATE

    public static DataState getInitialState( ) {
        Map<Integer, Double> values = new HashMap<>();
        // INITIAL DATA FOR ALL VARIABLES
        values.put(p_GB_pressure, (double) PM_A_GB_PRESSURE);
        values.put(s_GB_pressure, (double) PM_A_GB_PRESSURE);
        values.put(p_PS_ins_pressure, (double) 0);
        values.put(s_PS_ins_pressure, (double) 0);
        values.put(p_PS_exp_pressure, (double) 0);
        values.put(s_PS_exp_pressure, (double) 0);
        values.put(p_OS, (double) PM_A_GB_FiO2);
        values.put(s_OS, (double) PM_A_GB_FiO2);
        values.put(p_Fl1_flow, (double) 0);
        values.put(s_Fl1_flow, (double) 0);
        values.put(p_Fl2_flow, (double) 0);
        values.put(s_Fl2_flow, (double) 0);
        values.put(p_temp, (double) 37);
        values.put(s_temp, (double) 37);
        values.put(p_power_source, (double) 0);
        values.put(s_power_source, (double) 0);
        values.put(s_battery_level, (double) 100);
        values.put(p_fan, (double) 0);
        values.put(s_fan, (double) 0);
        values.put(a_IN_valve, (double) 0);
        values.put(a_OUT_valve, (double) 0);
        values.put(a_LED, (double) 0);
        values.put(RR_ms, (double) 0);
        values.put(peak_P_insp, (double) 0);
        values.put(V_tidal, (double) 0);
        values.put(V_E, (double) 0);
        values.put(t_RM_remaining, (double) 0);
        values.put(Status, (double) 0);
        values.put(IE_ms, (double) 0);
        values.put(b_powerOn, (double) 1);
        values.put(conn_power_source, (double) 1);
        values.put(conn_air_supply, (double) 1);
        values.put(conn_patient, (double) 0);
        values.put(conn_breathing, (double) 1);
        values.put(comm_sens_valves_ok, (double) 1);
        values.put(comm_memory, (double) 1);
        values.put(comm_cont_gui_ok, (double) 1);
        values.put(init_succ, (double) 0);
        values.put(conn_failToPowerOn, (double) 0);
        values.put(sys_out_of_service, (double) 0);
        values.put(selfTest_fail, (double) 0);
        values.put(gui_req_res_ven, (double) 0);
        values.put(power_switch_ok, (double) 1);
        values.put(no_leaks_breathing_circuit, (double) 1);
        values.put(out_valve_ok, (double) 1);
        values.put(alarms_ok, (double) 1);
        values.put(nr_of_retries, (double) 0);
        values.put(nr_of_retries_p, (double) 0);
        values.put(timer_PSV_exp, (double) 0);
        values.put(gui_req_change_mode_PCV, (double) 1);
        values.put(gui_req_change_mode_PSV, (double) 0);
        values.put(gui_req_stop_vent, (double) 0);
        values.put(timer_PCV_insp, (double) 0);
        values.put(timer_PSV_insp, (double) 0);
        values.put(timer_PCV_exp, (double) 0);
        values.put(drop_PAW, (double) 0);
        values.put(gui_req_IP, (double) 0);
        values.put(gui_req_RM, (double) 0);
        values.put(gui_req_EP, (double) 0);
        values.put(timer_IP, (double) 0);
        values.put(timer_EP, (double) 0);
        values.put(timer_RM, (double) 0);
        values.put(timer_triggerDelay, (double) 0);
        values.put(min_exp_time_psv, 0.4);
        values.put(b_powerOff, (double) 0);
        values.put(gui_param_psv_ok, (double) 0);
        values.put(phase, (double) 0);
        values.put(phase_changed, (double) 0);
        values.put(IE_toolow_counter, (double) 0);
        values.put(timer_insp, (double) 0);
        values.put(timer_exp, (double) 0);
        values.put(cycle_done, (double) 0);
        values.put(fs, (double) 0);
        values.put(previous_PAW, (double) 0);
        values.put(peak_flow, (double) 0);
        values.put(V_tidal_prev, (double) 0);
        values.put(rr_pcv, (double) 12);
        values.put(ie_pcv, 0.5);
        values.put(p_insp_pcv, (double) 15);
        values.put(ind_var, (double) 0); //TODO: remove this after my test
        values.put(alarm_counter, (double) -1);
        values.put(counter_cycles, (double) 0);
        values.put(switch_ready, (double) 0);
        values.put(req_counter, (double) -1);
        values.put(on_counter, (double) -1);
        values.put(test_counter, (double)-1);
        values.put(test_per, (double) 0);
        values.put(p_drop_PAW, (double) 0);
        values.put(p_peak_flow, (double) 0);
        values.put(psv_param_counter, (double) -1);

        return new DataState(NUMBER_OF_VARIABLES, i -> values.getOrDefault(i, Double.NaN));
        // if a variable is not initialized then it's automatically set to 0 (in the above line)
    }

}