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

package stark.speclang.types;

import stark.speclang.StarkSpecificationLanguageBaseVisitor;
import stark.speclang.StarkSpecificationLanguageParser;
import stark.speclang.parsing.ParseErrorCollector;
import stark.speclang.parsing.ParseUtil;

public class StarkFunctionStatementTypeInference extends StarkSpecificationLanguageBaseVisitor<StarkType> {
    private final ParseErrorCollector errors;

    private final TypeEvaluationContext context;

    public StarkFunctionStatementTypeInference(TypeEvaluationContext context, ParseErrorCollector errors) {
        this.errors = errors;
        this.context = context;
    }

    @Override
    public StarkType visitFunctionLetStatement(StarkSpecificationLanguageParser.FunctionLetStatementContext ctx) {
        StarkType letType = ExpressionTypeInference.infer(context, errors, ctx.value);
        if (!letType.isError()) {
            return StarkFunctionStatementTypeInference.infer(TypeEvaluationContext.letContext(this.context, ctx.name, letType), errors, ctx.body);
        } else {
            return StarkType.ERROR_TYPE;
        }
    }

    private static StarkType infer(TypeEvaluationContext context, ParseErrorCollector errors, StarkSpecificationLanguageParser.FunctionStatementContext ctx) {
        return ctx.accept(new StarkFunctionStatementTypeInference(context, errors));
    }

    @Override
    public StarkType visitFunctionIfThenElseStatement(StarkSpecificationLanguageParser.FunctionIfThenElseStatementContext ctx) {
        StarkType guardType = ExpressionTypeInference.infer(context, errors, ctx.guard);
        if (!guardType.isBoolean()) {
            errors.record(ParseUtil.typeError(StarkType.BOOLEAN_TYPE, guardType, ctx.guard.start));
        }
        StarkType thenType = ctx.thenStatement.accept(this);
        StarkType elseType = ctx.elseStatement.accept(this);
        if (!thenType.canBeMergedWith(elseType)) {
            errors.record(ParseUtil.typeError(thenType, elseType, ctx.elseStatement.start));
            return StarkType.ERROR_TYPE;
        }
        StarkType result = StarkType.merge(thenType, elseType);
        if (!result.isError()&&guardType.isRandom()) {
            return new StarkRandomType(result);
        } else {
            return result;
        }
    }

    @Override
    public StarkType visitFunctionReturnStatement(StarkSpecificationLanguageParser.FunctionReturnStatementContext ctx) {
        return ExpressionTypeInference.infer(context, errors, ctx.expression());
    }

    @Override
    public StarkType visitFunctionBlockStatement(StarkSpecificationLanguageParser.FunctionBlockStatementContext ctx) {
        return ctx.functionStatement().accept(this);
    }
}
