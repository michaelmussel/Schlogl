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

package stark.distance;

import java.util.function.DoubleBinaryOperator;
import java.util.function.ToDoubleFunction;

import org.apache.commons.math3.random.RandomGenerator;

import stark.EvolutionSequence;
import stark.ds.DataStateExpression;

/**
 * Class SkorokhodDistanceExpression implements the skorokhod distance expression
 * evaluating the Wasserstein distance between the distributions reached at a
 * given time step by two given evolution sequences, after applying
 * the time transfer function used to determine the skorokhod distance.
 */
public final class SkorokhodDistanceExpression implements DistanceExpression {

    //private final int maxIntervalScale;
    //private int absoluteRightInterval;

    private final int leftRetimingDecrement;
    private final int rightRetimingIncrement;
    private final DataStateExpression rho; // used to normalize distance
    private final ToDoubleFunction<Integer> rho2; // used to normalize time
    private final DoubleBinaryOperator distanceOperator;
    private final DoubleBinaryOperator muLogic; // used to determine mu from timestamp, and distance

    private final boolean direction;
    private final int relativeRightBound;
    private final int relativeLeftBound;
    private int intervalSize;
    private int absoluteLeftBound;
    private int absoluteRightBound;

    private final double resolution;
    private int maxOffset;
    private int minOffset;
    private int finalStep;
    private int finalOffset;
    private int firstOffset;
    private int previousStep;
    private double skorokhodDistance;

    private int[] offsets;

    private final double[][] DPTable; // Dynamic Programming table, used to store calculated wasserstein distances, to avoid calculating them multiple times

    private final boolean minimizeAverage;
    private double[][] PFTable; // PathFinding table, used to find the offsets resulting in the lowest average distance

    // stores reference to the sequences used to compute the skorokhod distance
    private EvolutionSequence sequence1;
    private EvolutionSequence sequence2;

    /**
     * Generates the Skorokhod distance expression that will use the given parameters
     * @param rho the penalty function
     * @param distance ground distance on reals.
     * @param muLogic logic to assign weight/cost to sampled lambda
     * @param rho2 for normalizing time
     * @param leftBound defines interval in which skorokhod distance is evaluated: [step + leftBound, step + rightbound] (see compute())
     * @param rightBound defines interval in which skorokhod distance is evaluated: [step + leftBound, step + rightbound] (see compute())
     * @param direction direction to allow time jumps toward, true = forward/positive offsets, false = backward/negative offsets.
     * @param resolution the resolution in which the skorokhod distance will be estimated using the binary search in the algorithm
     * @param minimizeAverge Wether to minimize the average distance (without increasing the Skorokhod distance) using Dijkstra's algorithm
     */
    public SkorokhodDistanceExpression(DataStateExpression rho, DoubleBinaryOperator distance, DoubleBinaryOperator muLogic ,ToDoubleFunction<Integer> rho2,
                                              int leftBound, int rightBound, boolean direction, double resolution, boolean minimizeAverge, int leftRetimingDecrement, int rightRetimingIncrement) {

        this.leftRetimingDecrement = leftRetimingDecrement;
        this.rightRetimingIncrement = rightRetimingIncrement;
        this.rho = rho;
        this.rho2 = rho2;
        this.distanceOperator = distance;
        this.direction = direction;
        this.muLogic = muLogic;
        this.resolution = resolution;
        this.minimizeAverage = minimizeAverge;
        this.relativeRightBound = rightBound;
        this.relativeLeftBound = leftBound;
        this.intervalSize = rightBound - leftBound;

        this.maxOffset = 0;
        this.PFTable = null;
        this.sequence1 = null;
        this.sequence2 = null;
        this.offsets = null;
        this.minOffset = Integer.MAX_VALUE;
        this.finalStep = 0;
        this.firstOffset = Integer.MAX_VALUE;

        // int size = this.intervalSize + 1;
        // rho2.applyAsDouble(Math.abs(_offset))
        int size_1 = this.intervalSize + 1;
        int size_2 = this.intervalSize + 1 + this.rightRetimingIncrement + this.leftRetimingDecrement;
        // System.out.println(size_1);
        // System.out.println(size_2);

        // + 1 since leftbount = 0, rightbound = 1 should result in 2 (by 2) wasserstein distances
        this.DPTable = new double[size_1][size_2];

        // fill with negative numbers to indicate that the distances are not yet calculated.
        for (int i = 0; i < size_1; i++) {
            for (int j = 0; j < size_2; j++) {
                this.DPTable[i][j] = -1;
            }
        }
    }

    /**
     * Computes the skorokhod distance between two evolution sequences over the time interval [step + leftBound, step + rightbound].
     *
     * @param step time step at which we start the evaluation of the expression
     * @param seq1 an evolution sequence
     * @param seq2 an evolution sequence
     * @return the skorokhod distance between two evolution sequences over the time interval [step + leftBound, step + rightbound].
     */
    @Override
    public double compute(int step, EvolutionSequence seq1, EvolutionSequence seq2) {
        // If the sequences have changed since previous compute, offsets should be recomputed
        if (this.previousStep != step)
        {
            this.Reset();
        }
        this.previousStep = step;

        if (this.sequence1 != seq1 || this.sequence2 != seq2)
        {
            ResetDPTable();
            this.Reset();
        }

        // recompute skorokhod distance and corresponding offsets
        if (this.offsets == null)
        {
            computeSkorokhod(step, seq1, seq2);
        }
        return this.skorokhodDistance;
    }

    // not yet implemented:
    @Override
    public double[] evalCI(RandomGenerator rg, int step, EvolutionSequence seq1, EvolutionSequence seq2, int m, double z){
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Returns the distance between the distributions reached at a
     * given time step by two given evolution sequences, after applying
     * the time transfer function used to determine the skorokhod distance
     * over the time interval [step + leftBound, step + rightbound]
     *
     * @param step time step at which the atomic is evaluated
     * @param seq1 an evolution sequence
     * @param seq2 an evolution sequence
     * @return the distance between the distributions reached at a
     * given time step by two given evolution sequences, after applying
     * the time transfer function used to determine the skorokhod distance.
     */
    public double sampleDistance(int step, EvolutionSequence seq1, EvolutionSequence seq2)
    {
        // if this step falls outside the bounds, return regular wasserstein distance
        if (step > this.absoluteRightBound || step < this.absoluteLeftBound)
        {
            return seq1.get(step).distance(this.rho, this.distanceOperator, seq2.get(step));
        }

        if (this.offsets == null || (this.sequence1 != seq1 || this.sequence2 != seq2))
        {
            System.err.println("Call compute() first!");
            return -1;
        }

        // sample wasserstein distance using offset
        return sample(step, this.offsets[step]);
    }

    // computes skorokhod distance, and places it in this.skorokhodDistance
    // writes the optimal offsets in this.offsets
    private void computeSkorokhod(int step, EvolutionSequence seq1, EvolutionSequence seq2)
    {
        if (this.offsets == null)
        {
            this.absoluteRightBound = step + this.relativeRightBound;
            this.absoluteLeftBound = step + this.relativeLeftBound;
            //this.absoluteRightInterval = (step + this.relativeRightBound) + (this.maxIntervalScale-1)*(this.relativeRightBound-this.relativeLeftBound);

            // System.out.println("this.absoluteRightBound = " + this.absoluteRightBound);
            // System.out.println("this.absoluteLeftBound = " + this.absoluteLeftBound);
            // System.out.println("this.absoluteRightInterval = " + this.absoluteRightInterval);

            this.offsets = new int[step + relativeRightBound + relativeLeftBound + 1];
            // store sequences that were used to compute offsets
            this.sequence1 = seq1;
            this.sequence2 = seq2;

            // System.out.println("\nDetermining offsets\n");
            // fill offset list
            this.skorokhodDistance = FindSkorokhodDistance(this.resolution);

            if (this.minimizeAverage)
            {
                System.out.println("Minimising average distance");
                Dijkstra(this.skorokhodDistance);
            }

            // non-decreasing lambda check
            for (int i = 1; i < finalStep; i++) {
                if (offsets[i - 1] - offsets[i] > 1)
                {
                    System.err.println("produced retiming is decreasing!");
                    break;
                }
            }
        }
        else
        {
            System.err.println("this.offsets was not null! Skorokhod is not computed.");
        }
    }

    /**
     * Evaluates the skorokhod distance between two evolution sequences,
     * and additionally returns the offsets used to achieve it
     * evaluated in interval [step + leftBound, step + rightbound]
     *
     * @param resolution the maximum allowed deviation from the resulting and
     * actual Skorokhod distance
     * @return the minimum skorokhod distance that the sequences conform to,
     * with maximum deviation of param resolution
     */
    private double FindSkorokhodDistance(double resolution)
    {
        // Find skorokhod distance at desired resolution, using binary search.
        double upper = 1.0;
        double lower = 0.0;

        Boolean conformance = false;
        double maxDistance = (upper + lower) / 2;
        while (!conformance || upper - lower >= resolution)
        {
            maxDistance = (upper + lower) / 2;
            conformance = EvaluateSkorokhodConformance(maxDistance);

            // if the sequence meets the current max skorokhod distance,
            // set upper to maxDistance, else set lower to maxDistance
            upper = conformance ? maxDistance : upper;
            lower = conformance ? lower : maxDistance;
        }
        return maxDistance;
    }

    /**
     * Evaluates whether the sequences conform to a maximum Skorokhod distance
     * and additionally returns the offsets used to achieve it in _offsets
     * evaluated in interval [step + leftBound, step + rightbound]
     *
     * @param maxDistance the maximum allowed Skorokhod distance
     * @return whether the sequences conform to the maximum Skorokhod distance
     *
     */
    private Boolean EvaluateSkorokhodConformance(double maxDistance)
    {
        this.maxOffset = Integer.MIN_VALUE;
        this.minOffset = Integer.MAX_VALUE;
        this.firstOffset = Integer.MAX_VALUE;
        int _offset = - this.leftRetimingDecrement;
        int currentStep = this.absoluteLeftBound;
        // stop checking once one of the sequences would be sampled beyond the right bound.
        //while (currentStep + _offset <= this.absoluteRightBound && currentStep <= this.absoluteRightBound)

        while (currentStep <= this.absoluteRightBound && currentStep + _offset <= this.absoluteRightBound + this.rightRetimingIncrement) {
            // calculate distance at this step, using normalised distance and time
            double timeOffset = rho2.applyAsDouble(Math.abs(_offset));
            double sampledDistance = sample(currentStep, _offset);
            double mu = this.muLogic.applyAsDouble(timeOffset, sampledDistance);

            // increase offset if distance is too large
            while (mu > maxDistance) {
                _offset++;
                timeOffset = rho2.applyAsDouble(Math.abs(_offset));
                // if new offset exceeds bounds, no offset was found within bounds that still meets the max distance
                //if ((_offset > 0 && timeOffset > maxDistance) || currentStep + _offset > this.absoluteRightBound)
                if ((_offset > -leftRetimingDecrement && timeOffset > maxDistance) || currentStep + _offset > this.absoluteRightBound + this.rightRetimingIncrement)
                {
                    return false;
                }

                // recalculate mu using increased offset
                sampledDistance = sample(currentStep, _offset);
                mu = this.muLogic.applyAsDouble(timeOffset, sampledDistance);
            }
            if (this.firstOffset == Integer.MAX_VALUE)
            {
                this.firstOffset = _offset;
            }

            this.offsets[currentStep] = _offset;
            // if this offset is min or max, store it.
            if (_offset < this.minOffset ) this.minOffset = _offset;
            if (_offset > this.maxOffset ) this.maxOffset = _offset;
            // allow decreasing 1 offset per step.
            _offset--;
            currentStep++;
        }
        this.finalStep = currentStep - 1; // -1 since step++ is done after last offset is stored
        this.finalOffset = _offset + 1;
        // fill remaining steps on right with offset of 0, these steps should not be included in robustness analysis
        while (currentStep <= this.absoluteRightBound)
        {
            this.offsets[currentStep] = 0;
            currentStep++;
        }

        // before left bound, offset = 0
        currentStep = 0;
        while (currentStep < this.absoluteLeftBound)
        {
            this.offsets[currentStep] = 0;
            currentStep++;
        }

        return true;
    }

    /**
     * Minimises average distance between sequences without increasing SkorokhodDistance
     * using Dijkstra's algorithm, writes to this.offsets.
     *
     * @param skorokhodDistance the maximum allowed Skorokhod distance
     * @return the offsets used to achieve the resulting
     * average distance, written to this.offsets
     *
     */
    private void Dijkstra(double skorokhodDistance)
    {
        // + 1 such that all offsets have a spot in the matrix
        int offsetSpan = this.maxOffset - this.minOffset + 1;

        // pathfinding wont help if this holds
        if (offsetSpan <= 1)
        {
            return;
        }

        // + 1 such that the final step is included
        int size = this.finalStep - this.absoluteLeftBound + 1;
        this.PFTable = new double[size][offsetSpan];

        double inf = Double.MAX_VALUE / 4;

        // fill all nodes with infinity ( / 4 to avoid overflow)
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < offsetSpan; j++) {
                this.PFTable[i][j] = inf;
            }
        }

        // set starting node distance to 0
        this.PFTable[0][this.firstOffset - this.minOffset] = 0;

        // visit all nodes
        // stop 1 earlier, since final nodes do not need to be visited themselves
        for (int unvisitedStepRelative = 0; unvisitedStepRelative < size - 1; unvisitedStepRelative++)
        {
            for (int unvisitedOffset = this.minOffset; unvisitedOffset <= this.maxOffset; unvisitedOffset++)
            {
                double sourceDistance = this.PFTable[unvisitedStepRelative][unvisitedOffset - this.minOffset];

                // scan over all reachable neighbours from this node, setting the min distance to source
                // offset may decrease by 1 every step, so start visiting neighbours from unvisitedOffset - 1 up to and including maxOffset
                for (int neighbourOffset = Math.max(unvisitedOffset - 1, this.minOffset); neighbourOffset <= this.maxOffset; neighbourOffset++) {
                    // absolute step that this neighbour may be indexed at:
                    int neighbourStep = this.absoluteLeftBound + unvisitedStepRelative + 1;
                    if (neighbourStep + neighbourOffset <= this.absoluteRightBound && neighbourStep + neighbourOffset >= this.absoluteLeftBound)
                    {
                        double timeOffset = rho2.applyAsDouble(Math.abs(neighbourOffset));
                        double neighbourDistance = sample(neighbourStep, neighbourOffset);
                        double mu = this.muLogic.applyAsDouble(timeOffset, neighbourDistance);

                        // if the distance exceeds skorokhod distance, set it to infinity
                        double distance = (mu > skorokhodDistance) ? inf : Math.min(neighbourDistance + sourceDistance, inf);

                        // if moving from current node to this neighbour results in a lower total distance, save it.
                        if (distance < this.PFTable[unvisitedStepRelative + 1][neighbourOffset - this.minOffset])
                        {
                            this.PFTable[unvisitedStepRelative + 1][neighbourOffset - this.minOffset] = distance;
                        }
                    }
                }
            }
        }

        // print pathfinding matrix:
        // for (int i = 0; i < size; i++) {
        //     for (int j = 0; j < offsetSpan; j++) {
        //         if (this.PFTable[i][j] >= 2000) {
        //             System.out.printf(" inf ");
        //         } else {
        //             System.out.printf(" %.3f ", this.PFTable[i][j]);
        //         }
        //     }
        //     System.out.println();
        // }

        int PrevNodeOffset = this.maxOffset;

        // fill entire offset list
        for (int currentStep = (this.finalStep - this.absoluteLeftBound); currentStep > 0; currentStep--)
        {
            double minDistance = Double.MAX_VALUE;
            int bestOffset = PrevNodeOffset;
            for (int i = PrevNodeOffset - this.minOffset; i >= 0; i--)
            {
                if (this.PFTable[currentStep][i] < minDistance)
                {
                    minDistance = this.PFTable[currentStep][i];
                    bestOffset = i + this.minOffset;
                }
            }
            this.offsets[currentStep + this.absoluteLeftBound] = bestOffset;
            // add one because the path may decrease offset once per step
            PrevNodeOffset = Math.min(bestOffset + 1, this.maxOffset);
        }
        this.finalOffset = this.offsets[this.finalStep];
        // print all produced offsets:
        // System.out.println("");
        // for (int i = 0; i < size; i++) {
        //     System.out.print(_offsets[i + leftBound]);
        //     System.out.print(",");
        // }
        // System.out.println("");
    }

    /**
     * Samples wasserstein distance between this.sequence1 and this.sequence2, as set by compute(). Samples one of the sequences
     * at an offset.
     *
     * @param step time step at which the sequences will be evaluated
     * @param offset one of the sequences will be sampled at an offset from the other
     * @return the wasserstein distance between 2 sequences
     */
    private double sample(int step, int offset)
    {
        // if forward direction, iterate over seq2 by adding the offset to its index
        // else iterate over seq 1
        int indexSeq1 = this.direction ? step           : step + offset;
        int indexSeq2 = this.direction ? step + offset  : step;

        // do not use DPTable before left bound
        if (offset >= 0 && (indexSeq1 < this.absoluteLeftBound || indexSeq2 < this.absoluteLeftBound))
        {
            return this.sequence1.get(indexSeq1).distance(this.rho, this.distanceOperator, this.sequence2.get(indexSeq2));
        }

        int DPIndex1 = indexSeq1 - this.absoluteLeftBound;
        int DPIndex2 = indexSeq2 - this.absoluteLeftBound;

        // is intended to throw an exception if sampled beyond the bounds, since this indicates a problem in the algorithm.
        double distance = this.DPTable[DPIndex1][DPIndex2];

        // calculate distance, and put into table
        if (distance < 0)
        {
            distance = this.sequence1.get(indexSeq1).distance(this.rho, this.distanceOperator, this.sequence2.get(indexSeq2));
            this.DPTable[DPIndex1][DPIndex2] = distance;
        }

        return distance;
    }

    /**
     * Samples bootstrap wasserstein distance given an offset and 2 sequences
     *
     * @param step time step at which the sequences will be evaluated
     * @param offset one of the sequences will be sampled at an offset from the other
     * @param seq1 an evolution sequence
     * @param seq2 the other evolution sequence
     * @return the wasserstein distance between 2 sequences
     */
    private double[] bootstrapSample(RandomGenerator rg, int step, int offset, EvolutionSequence seq1, EvolutionSequence seq2, int m, double z)
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Returns the list of offsets used for computing skorokhod distance after the previous compute() call.
     */
    public int[] GetOffsetArray()
    {
        return this.offsets;
    }

    /**
     * Returns the maximum offset in the list offsets used for computing skorokhod distance after the previous compute() call.
     */
    public int GetMaxOffset()
    {
        return this.maxOffset;
    }

    /**
     * Returns the final offset in the list offsets used for computing skorokhod distance after the previous compute() call.
     */
    public int GetFinalOffset()
    {
        return this.finalOffset;
    }

    /**
     * Resets internal variables for next compute() call.
     */
    private void Reset()
    {
        this.offsets = null;
        this.skorokhodDistance = Integer.MIN_VALUE;
        this.maxOffset = Integer.MIN_VALUE;
        this.finalOffset = Integer.MIN_VALUE;
        this.minOffset = Integer.MAX_VALUE;
        this.finalStep = Integer.MIN_VALUE;
        this.firstOffset = Integer.MAX_VALUE;
    }

    /**
     * Resets programming table in case sequences have changed since last compute() call.
     */
    private void ResetDPTable()
    {
        for (int i = 0; i < this.intervalSize + 1; i++) { // was this.intervalSize + 1;
            for (int j = 0; j < this.intervalSize + 1 + rightRetimingIncrement + leftRetimingDecrement; j++) { // was this.intervalSize + 1;
                this.DPTable[i][j] = -1;
            }
        }
    }

}