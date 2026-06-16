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

import stark.ds.DataStateUpdate;
import stark.speclang.StarkSpecificationLanguageBaseVisitor;
import stark.speclang.StarkSpecificationLanguageParser;
import stark.speclang.semantics.*;
import stark.speclang.values.StarkValue;
import org.apache.commons.math3.random.RandomGenerator;
import stark.speclang.variables.*;

import java.util.Optional;
import java.util.function.BiFunction;

public class StarkEnvironmentGenerator extends StarkSpecificationLanguageBaseVisitor<StarkEnvironmentUpdateFunction> {

    private final StarkVariableAllocation allocation;
    private final StarkExpressionEvaluationContext context;
    private final StarkVariableRegistry registry;

    public StarkEnvironmentGenerator(StarkVariableAllocation allocation, StarkExpressionEvaluationContext context, StarkVariableRegistry registry) {
        this.allocation = allocation;
        this.context = context;
        this.registry = registry;
    }

    @Override
    public StarkEnvironmentUpdateFunction visitEnvironmentBlock(StarkSpecificationLanguageParser.EnvironmentBlockContext ctx) {
        return new StarkEnvironmentBlockFunction(this.allocation, ctx.commands.stream().map(c -> c.accept(this)).toList());
    }

    @Override
    public StarkEnvironmentUpdateFunction visitEnvironmentAssignment(StarkSpecificationLanguageParser.EnvironmentAssignmentContext ctx) {
        return new StarkEnvironmentAssignmentFunction(this.allocation, getEnvironmentAssignmentFunction(ctx.variableAssignment()));
    }

    private BiFunction<RandomGenerator, StarkStore, Optional<DataStateUpdate>> getEnvironmentAssignmentFunction(StarkSpecificationLanguageParser.VariableAssignmentContext variableAssignmentContext) {
        StarkVariable variable = registry.get(StarkVariable.getTargetVariableName(variableAssignmentContext.target.name.getText()));
        StarkExpressionEvaluationFunction valueFunction = StarkExpressionEvaluator.eval(context, registry, variableAssignmentContext.value);
        if (variableAssignmentContext.guard != null) {
            StarkExpressionEvaluationFunction guardFunction = StarkExpressionEvaluator.eval(context, registry, variableAssignmentContext.guard);
            return (rg, s) -> (StarkValue.isTrue(guardFunction.eval(rg, s))?allocation.set(variable, valueFunction.eval(rg, s)):Optional.empty());
        } else {
            return (rg, s) -> allocation.set(variable, valueFunction.eval(rg, s));
        }
    }


    @Override
    public StarkEnvironmentUpdateFunction visitEnvironmentIfThenElse(StarkSpecificationLanguageParser.EnvironmentIfThenElseContext ctx) {
        if (ctx.elseCommand != null) {
            return new StarkEnvironmentConditionalUpdateFunction(
                    this.allocation,
                    StarkExpressionEvaluator.eval(context, registry, ctx.guard),
                    ctx.thenCommand.accept(this),
                    ctx.elseCommand.accept(this));
        } else {
            return new StarkEnvironmentConditionalUpdateFunction(
                    this.allocation,
                    StarkExpressionEvaluator.eval(context, registry, ctx.guard),
                    ctx.thenCommand.accept(this));

        }
    }

    @Override
    public StarkEnvironmentUpdateFunction visitEnvironmentLetCommand(StarkSpecificationLanguageParser.EnvironmentLetCommandContext ctx) {
        StarkVariable[] variables = new StarkVariable[ctx.localVariables.size()];
        StarkExpressionEvaluationFunction[] localVariablesValues = new StarkExpressionEvaluationFunction[variables.length];
        for(int i=0; i<variables.length; i++) {
            variables[i] = registry.getOrRegister(ctx.localVariables.get(i).name.getText());
            localVariablesValues[i] = StarkExpressionEvaluator.eval(context, registry, ctx.localVariables.get(i).expression());
        }
        return new StarkEnvironmentLetUpdateFunction(allocation, variables, localVariablesValues, ctx.body.accept(this));
    }
}
