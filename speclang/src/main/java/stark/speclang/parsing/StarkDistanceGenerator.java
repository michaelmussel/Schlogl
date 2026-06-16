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

package stark.speclang.parsing;

import stark.distance.*;
import stark.ds.DataStateExpression;
import stark.ds.RelationOperator;
import stark.speclang.StarkSpecificationLanguageBaseVisitor;
import stark.speclang.StarkSpecificationLanguageParser;
import stark.speclang.semantics.StarkExpressionEvaluator;
import stark.speclang.values.StarkValue;
import stark.speclang.variables.StarkExpressionEvaluationContext;
import stark.speclang.variables.StarkVariableAllocation;
import stark.speclang.variables.StarkVariableRegistry;

import java.util.Map;

public class StarkDistanceGenerator extends StarkSpecificationLanguageBaseVisitor<DistanceExpression> {
    private final StarkVariableAllocation allocation;
    private final StarkExpressionEvaluationContext context;
    private final StarkVariableRegistry registry;
    private final Map<String, DistanceExpression> distanceMap;
    private final Map<String, DataStateExpression> penalties;

    public StarkDistanceGenerator(StarkVariableAllocation allocation, StarkExpressionEvaluationContext context, StarkVariableRegistry registry, Map<String, DistanceExpression> distanceExpressionMap, Map<String, DataStateExpression> penalties) {
        this.allocation = allocation;
        this.context = context;
        this.registry = registry;
        this.distanceMap = distanceExpressionMap;
        this.penalties = penalties;
    }


    @Override
    public DistanceExpression visitDistanceExpressionAtomicLeft(StarkSpecificationLanguageParser.DistanceExpressionAtomicLeftContext ctx) {
        DataStateExpression expression =  this.penalties.getOrDefault(ctx.panaltyName.getText(), ds -> Double.NaN); //StarkExpressionEvaluator.evalToDataStateExpression(this.allocation, this.context, this.registry, ctx.expression());
        return new AtomicDistanceExpressionLeq(expression);
    }

    @Override
    public DistanceExpression visitDistanceExpressionAtomicRight(StarkSpecificationLanguageParser.DistanceExpressionAtomicRightContext ctx) {
        DataStateExpression expression =  this.penalties.getOrDefault(ctx.panaltyName.getText(), ds -> Double.NaN); //StarkExpressionEvaluator.evalToDataStateExpression(this.allocation, this.context, this.registry, ctx.expression());
        return new AtomicDistanceExpressionGeq(expression);
    }

    @Override
    public DistanceExpression visitDistanceExpressionFinally(StarkSpecificationLanguageParser.DistanceExpressionFinallyContext ctx) {
        int from = StarkValue.intValue(StarkExpressionEvaluator.evalToValue(context, registry, ctx.from));
        int to = StarkValue.intValue(StarkExpressionEvaluator.evalToValue(context, registry, ctx.to));
        return new MinIntervalDistanceExpression(ctx.argument.accept(this),from, to);
    }

    @Override
    public DistanceExpression visitDistanceExpressionReference(StarkSpecificationLanguageParser.DistanceExpressionReferenceContext ctx) {
        return distanceMap.get(ctx.name.getText());
    }

    @Override
    public DistanceExpression visitDistanceExpressionMax(StarkSpecificationLanguageParser.DistanceExpressionMaxContext ctx) {
        return new MaxDistanceExpression(ctx.first.accept(this), ctx.second.accept(this));
    }

    @Override
    public DistanceExpression visitDistanceExpressionUntil(StarkSpecificationLanguageParser.DistanceExpressionUntilContext ctx) {
        int from = StarkValue.intValue(StarkExpressionEvaluator.evalToValue(context, registry, ctx.from));
        int to = StarkValue.intValue(StarkExpressionEvaluator.evalToValue(context, registry, ctx.to));
        return new UntilDistanceExpression(ctx.left.accept(this), from, to, ctx.right.accept(this));
    }

    @Override
    public DistanceExpression visitDistanceExpressionMin(StarkSpecificationLanguageParser.DistanceExpressionMinContext ctx) {
        return new MinDistanceExpression(ctx.first.accept(this), ctx.second.accept(this));
    }

    @Override
    public DistanceExpression visitDistanceExpressionGlobally(StarkSpecificationLanguageParser.DistanceExpressionGloballyContext ctx) {
        int from = StarkValue.intValue(StarkExpressionEvaluator.evalToValue(context, registry, ctx.from));
        int to = StarkValue.intValue(StarkExpressionEvaluator.evalToValue(context, registry, ctx.to));
        return new MaxIntervalDistanceExpression(ctx.argument.accept(this),from, to);
    }

    @Override
    public DistanceExpression visitDistanceExpressionBrackets(StarkSpecificationLanguageParser.DistanceExpressionBracketsContext ctx) {
        return ctx.distanceExpression().accept(this);
    }

    @Override
    public DistanceExpression visitDistanceExpressionLinearCombination(StarkSpecificationLanguageParser.DistanceExpressionLinearCombinationContext ctx) {
        double[] weights = ctx.weights.stream().map(exp -> StarkExpressionEvaluator.evalToValue(context, registry, exp)).mapToDouble(StarkValue::doubleOf).toArray();
        DistanceExpression[] distanceExpressions = ctx.values.stream().map(de -> de.accept(this)).toArray(DistanceExpression[]::new);
        return new ConvexCombinationDistanceExpression(weights, distanceExpressions);
    }


    @Override
    public DistanceExpression visitDistanceExpressionThreshold(StarkSpecificationLanguageParser.DistanceExpressionThresholdContext ctx) {
        double value = StarkExpressionEvaluator.evalToValue(context, registry, ctx.right).toDouble();
        return new ThresholdDistanceExpression(ctx.left.accept(this), RelationOperator.get(ctx.op.getText()), value);
    }
}
