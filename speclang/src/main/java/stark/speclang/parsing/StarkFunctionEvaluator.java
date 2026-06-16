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

import stark.speclang.StarkSpecificationLanguageBaseVisitor;
import stark.speclang.StarkSpecificationLanguageParser;
import stark.speclang.semantics.StarkExpressionEvaluationFunction;
import stark.speclang.semantics.StarkExpressionEvaluator;
import stark.speclang.values.StarkValue;
import stark.speclang.variables.StarkExpressionEvaluationContext;
import stark.speclang.variables.StarkStore;
import stark.speclang.variables.StarkVariable;
import stark.speclang.variables.StarkVariableRegistry;

public class StarkFunctionEvaluator extends StarkSpecificationLanguageBaseVisitor<StarkExpressionEvaluationFunction> {

    private final StarkExpressionEvaluationContext context;

    private final StarkVariableRegistry registry;

    public StarkFunctionEvaluator(StarkExpressionEvaluationContext context, StarkVariableRegistry registry) {
        this.context = context;
        this.registry = registry;
    }

    public static StarkExpressionEvaluationFunction eval(StarkExpressionEvaluationContext context, StarkVariableRegistry registry, StarkSpecificationLanguageParser.FunctionBlockStatementContext functionBlockStatement) {
        return functionBlockStatement.accept(new StarkFunctionEvaluator(context, registry));
    }


    @Override
    public StarkExpressionEvaluationFunction visitFunctionLetStatement(StarkSpecificationLanguageParser.FunctionLetStatementContext ctx) {
        StarkExpressionEvaluationFunction valueFunction = StarkExpressionEvaluator.eval(context, registry, ctx.value);
        StarkVariable variable = registry.getOrRegister(ctx.name.getText());
        StarkExpressionEvaluationFunction bodyFunction = ctx.body.accept(this);
        return (rg, s) -> bodyFunction.eval(rg, StarkStore.let(variable, valueFunction.eval(rg, s), s));
    }

    @Override
    public StarkExpressionEvaluationFunction visitFunctionIfThenElseStatement(StarkSpecificationLanguageParser.FunctionIfThenElseStatementContext ctx) {
        StarkExpressionEvaluationFunction guardFunction = StarkExpressionEvaluator.eval(context, registry, ctx.guard);
        StarkExpressionEvaluationFunction thenFunction = ctx.thenStatement.accept(this);
        StarkExpressionEvaluationFunction elseFunction = ctx.elseStatement.accept(this);
        return (rg, s) -> {
            if (StarkValue.isTrue(guardFunction.eval(rg, s))) {
                return thenFunction.eval(rg, s);
            } else {
                return elseFunction.eval(rg, s);
            }
        };
    }

    @Override
    public StarkExpressionEvaluationFunction visitFunctionReturnStatement(StarkSpecificationLanguageParser.FunctionReturnStatementContext ctx) {
        return StarkExpressionEvaluator.eval(context, registry, ctx.expression());
    }

    @Override
    public StarkExpressionEvaluationFunction visitFunctionBlockStatement(StarkSpecificationLanguageParser.FunctionBlockStatementContext ctx) {
        return ctx.functionStatement().accept(this);
    }
}
