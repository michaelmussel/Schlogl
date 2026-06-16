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

import stark.distance.DistanceExpression;
import stark.ds.RelationOperator;
import stark.perturbation.Perturbation;
import stark.speclang.StarkSpecificationLanguageBaseVisitor;
import stark.speclang.StarkSpecificationLanguageParser;
import stark.robtl.*;
import stark.speclang.semantics.StarkExpressionEvaluator;
import stark.speclang.values.StarkValue;
import stark.speclang.variables.StarkExpressionEvaluationContext;
import stark.speclang.variables.StarkVariableAllocation;
import stark.speclang.variables.StarkVariableRegistry;

import java.util.Map;

public class StarkRobustnessFormulaGenerator extends StarkSpecificationLanguageBaseVisitor<RobustnessFormula> {
    private final StarkVariableAllocation allocation;
    private final StarkExpressionEvaluationContext context;
    private final StarkVariableRegistry registry;
    private final Map<String, DistanceExpression> distanceMap;
    private final Map<String, Perturbation> perturbationMap;
    private final Map<String, RobustnessFormula> formulaMap;

    public StarkRobustnessFormulaGenerator(StarkVariableAllocation allocation, StarkExpressionEvaluationContext context, StarkVariableRegistry registry, Map<String, Perturbation> perturbationMap, Map<String, DistanceExpression> distanceExpressionMap, Map<String, RobustnessFormula> formulaMap) {
        this.allocation = allocation;
        this.context = context;
        this.registry = registry;
        this.distanceMap = distanceExpressionMap;
        this.perturbationMap = perturbationMap;
        this.formulaMap = formulaMap;
    }

    @Override
    public RobustnessFormula visitRobtlFormulaDistance(StarkSpecificationLanguageParser.RobtlFormulaDistanceContext ctx) {
        double value = StarkExpressionEvaluator.evalToValue(context, registry, ctx.value).toDouble();
        return new AtomicRobustnessFormula(perturbationMap.get(ctx.perturbationReference.getText()), distanceMap.get(ctx.expressionReference.getText()), RelationOperator.get(ctx.op.getText()), value);
    }

    @Override
    public RobustnessFormula visitRobtlFormulaConjunction(StarkSpecificationLanguageParser.RobtlFormulaConjunctionContext ctx) {
        return new ConjunctionRobustnessFormula(ctx.left.accept(this), ctx.right.accept(this));
    }

    @Override
    public RobustnessFormula visitRobtlFormulaTrue(StarkSpecificationLanguageParser.RobtlFormulaTrueContext ctx) {
        return new TrueRobustnessFormula();
    }

    @Override
    public RobustnessFormula visitRobtlFormulaReference(StarkSpecificationLanguageParser.RobtlFormulaReferenceContext ctx) {
        return formulaMap.get(ctx.name.getText());
    }

    @Override
    public RobustnessFormula visitRobtlFormulaUntil(StarkSpecificationLanguageParser.RobtlFormulaUntilContext ctx) {
        int from = StarkValue.intValue(StarkExpressionEvaluator.evalToValue(context, registry, ctx.from));
        int to = StarkValue.intValue(StarkExpressionEvaluator.evalToValue(context, registry, ctx.to));
        return new UntilRobustnessFormula(ctx.left.accept(this), from, to, ctx.right.accept(this));
    }

    @Override
    public RobustnessFormula visitRobtlFormulaNegation(StarkSpecificationLanguageParser.RobtlFormulaNegationContext ctx) {
        return new NegationRobustnessFormula(ctx.argument.accept(this));
    }

    @Override
    public RobustnessFormula visitRobtlFormulaDisjunction(StarkSpecificationLanguageParser.RobtlFormulaDisjunctionContext ctx) {
        return new DisjunctionRobustnessFormula(ctx.left.accept(this), ctx.right.accept(this));
    }

    @Override
    public RobustnessFormula visitRobtlFormulaFalse(StarkSpecificationLanguageParser.RobtlFormulaFalseContext ctx) {
        return new FalseRobustnessFormula();
    }

    @Override
    public RobustnessFormula visitRobtlFormulaAlways(StarkSpecificationLanguageParser.RobtlFormulaAlwaysContext ctx) {
        int from = StarkValue.intValue(StarkExpressionEvaluator.evalToValue(context, registry, ctx.from));
        int to = StarkValue.intValue(StarkExpressionEvaluator.evalToValue(context, registry, ctx.to));
        return new AlwaysRobustnessFormula(ctx.argument.accept(this), from, to);
    }

    @Override
    public RobustnessFormula visitRobtlFormulaEventually(StarkSpecificationLanguageParser.RobtlFormulaEventuallyContext ctx) {
        int from = StarkValue.intValue(StarkExpressionEvaluator.evalToValue(context, registry, ctx.from));
        int to = StarkValue.intValue(StarkExpressionEvaluator.evalToValue(context, registry, ctx.to));
        return new EventuallyRobustnessFormula(ctx.argument.accept(this), from, to);
    }
}
