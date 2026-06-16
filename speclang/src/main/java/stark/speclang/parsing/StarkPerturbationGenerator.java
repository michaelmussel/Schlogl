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

import stark.perturbation.AtomicPerturbation;
import stark.perturbation.IterativePerturbation;
import stark.perturbation.Perturbation;
import stark.perturbation.SequentialPerturbation;
import stark.ds.DataStateFunction;
import stark.ds.DataStateUpdate;
import stark.speclang.StarkSpecificationLanguageBaseVisitor;
import stark.speclang.StarkSpecificationLanguageParser;
import stark.speclang.semantics.StarkExpressionEvaluationFunction;
import stark.speclang.semantics.StarkExpressionEvaluator;
import stark.speclang.values.StarkValue;
import org.apache.commons.math3.random.RandomGenerator;
import stark.speclang.variables.*;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class StarkPerturbationGenerator extends StarkSpecificationLanguageBaseVisitor<Perturbation> {
    private final StarkVariableAllocation allocation;
    private final StarkExpressionEvaluationContext context;
    private final StarkVariableRegistry registry;
    private final Map<String, Perturbation> perturbationMap;

    public StarkPerturbationGenerator(StarkVariableAllocation allocation, StarkExpressionEvaluationContext context, StarkVariableRegistry registry, Map<String, Perturbation> perturbationMap) {
        this.allocation = allocation;
        this.context = context;
        this.registry = registry;
        this.perturbationMap = perturbationMap;
    }

    @Override
    public Perturbation visitPerturbationExpressionReference(StarkSpecificationLanguageParser.PerturbationExpressionReferenceContext ctx) {
        return perturbationMap.getOrDefault(ctx.name.getText(), Perturbation.NONE);
    }

    @Override
    public Perturbation visitPerturbationExpressionBrackets(StarkSpecificationLanguageParser.PerturbationExpressionBracketsContext ctx) {
        return ctx.perturbationExpression().accept(this);
    }

    @Override
    public Perturbation visitPerturbationExpressionIteration(StarkSpecificationLanguageParser.PerturbationExpressionIterationContext ctx) {
        int iteration = StarkValue.intValue(StarkExpressionEvaluator.evalToValue(context, registry, ctx.iterationValue));
        return new IterativePerturbation(iteration, ctx.argument.accept(this));
    }

    @Override
    public Perturbation visitPerturbationExpressionAtomic(StarkSpecificationLanguageParser.PerturbationExpressionAtomicContext ctx) {
        int iteration = StarkValue.intValue(StarkExpressionEvaluator.evalToValue(context, registry, ctx.time));
        return new AtomicPerturbation(iteration, getAssignment(ctx.assignments));
    }

    private DataStateFunction getAssignment(List<StarkSpecificationLanguageParser.PerturbationAssignmentContext> assignments) {
        List<BiFunction<RandomGenerator, StarkStore, DataStateUpdate>> updates = assignments.stream().map(this::getAssignment).toList();
        return (rg, ds) -> {
            StarkStore store = StarkStore.storeOf(allocation,ds);
            return ds.apply(updates.stream().map(uf -> uf.apply(rg, store)).toList());
        };
    }

    private BiFunction<RandomGenerator, StarkStore, DataStateUpdate> getAssignment(StarkSpecificationLanguageParser.PerturbationAssignmentContext assignment) {
        StarkVariable variable = registry.get(assignment.name.getText());
        StarkExpressionEvaluationFunction value = StarkExpressionEvaluator.eval(context, registry, assignment.value);
        return (rg, s) -> allocation.set(variable, value.eval(rg, s)).get();
    }

        @Override
    public Perturbation visitPerturbationExpressionSequence(StarkSpecificationLanguageParser.PerturbationExpressionSequenceContext ctx) {
        return new SequentialPerturbation(ctx.first.accept(this), ctx.second.accept(this));
    }

    @Override
    public Perturbation visitPerturbationExpressionNil(StarkSpecificationLanguageParser.PerturbationExpressionNilContext ctx) {
        return Perturbation.NONE;
    }
}
