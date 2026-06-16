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

package stark.speclang.types;

import stark.speclang.StarkSpecificationLanguageBaseVisitor;
import stark.speclang.StarkSpecificationLanguageParser;
import stark.speclang.parsing.ParseErrorCollector;
import stark.speclang.parsing.ParseUtil;
import org.antlr.v4.runtime.ParserRuleContext;

/**
 * This visitor is used to infer types of expressions.
 */
public class ExpressionTypeInference extends StarkSpecificationLanguageBaseVisitor<StarkType> {
    private final TypeEvaluationContext context;
    private final ParseErrorCollector errors;
    private final boolean randomExpressionAllowed;

    /**
     * Creates a new visitor used to infer the type of expressions. This constructor takes as parameters
     * a
     * @param context
     * @param errors
     */
    public  ExpressionTypeInference(TypeEvaluationContext context, ParseErrorCollector errors, boolean randomExpressionAllowed) {
        this.context = context;
        this.errors = errors;
        this.randomExpressionAllowed = randomExpressionAllowed;
    }

    public ExpressionTypeInference(TypeEvaluationContext context, ParseErrorCollector errors) {
        this(context, errors, false);
    }

    public static StarkType infer(TypeEvaluationContext context, ParseErrorCollector errors, StarkSpecificationLanguageParser.ExpressionContext value) {
        return value.accept(new ExpressionTypeInference(context, errors, true));
    }

    public static StarkType infer(TypeEvaluationContext context, ParseErrorCollector errors, boolean randomExpressionAllowed, StarkSpecificationLanguageParser.ExpressionContext value) {
        return value.accept(new ExpressionTypeInference(context, errors, randomExpressionAllowed));
    }


    public StarkType inferAndCheck(StarkType expectedType, ParserRuleContext ctx) {
        StarkType actualType = ctx.accept(this);
        if (!expectedType.isCompatibleWith(actualType)) {
            errors.record(ParseUtil.typeError(expectedType, actualType, ctx.start));
            return StarkType.ERROR_TYPE;
        }
        return actualType;
    }

    public boolean checkType(StarkType expectedType, ParserRuleContext ctx) {
        return !inferAndCheck(expectedType, ctx).isError();
    }

    @Override
    public StarkType visitNegationExpression(StarkSpecificationLanguageParser.NegationExpressionContext ctx) {
        if (checkType(StarkType.BOOLEAN_TYPE, ctx.arg)) {
            return StarkType.BOOLEAN_TYPE;
        }
        return StarkType.ERROR_TYPE;
    }

    @Override
    public StarkType visitExponentExpression(StarkSpecificationLanguageParser.ExponentExpressionContext ctx) {
        return combineToRealType(ctx.left, ctx.right);
    }

    private StarkType combineToRealType(ParserRuleContext left, ParserRuleContext right) {
        StarkType leftType = checkNumerical(left);
        StarkType rightType = checkNumerical(right);
        return (leftType.isRandom()||rightType.isRandom()?new StarkRandomType(StarkType.REAL_TYPE): StarkType.REAL_TYPE);
    }

    private StarkType checkNumerical(ParserRuleContext ctx) {
        StarkType type = ctx.accept(this);
        if (!type.isNumerical()) {
            errors.record(ParseUtil.expectedNumericalType(type, ctx.start));
            return StarkType.ERROR_TYPE;
        }
        return type;
    }

    @Override
    public StarkType visitBinaryMathCallExpression(StarkSpecificationLanguageParser.BinaryMathCallExpressionContext ctx) {
        return combineToRealType(ctx.left, ctx.right);
    }


    @Override
    public StarkType visitReferenceExpression(StarkSpecificationLanguageParser.ReferenceExpressionContext ctx) {
        String name = ctx.name.getText();
        if (!context.isDefined(name)) {
            errors.record(ParseUtil.unknownSymbol(ctx.name));
            return StarkType.ERROR_TYPE;
        }
        if (!context.isAReference(name)) {
            errors.record(ParseUtil.illegalUseOfName(ctx.name));
            return StarkType.ERROR_TYPE;
        }
        return context.getTypeOf(name);
    }

    @Override
    public StarkType visitIntValue(StarkSpecificationLanguageParser.IntValueContext ctx) {
        return StarkType.INTEGER_TYPE;
    }

    @Override
    public StarkType visitTrueValue(StarkSpecificationLanguageParser.TrueValueContext ctx) {
        return StarkType.BOOLEAN_TYPE;
    }

    @Override
    public StarkType visitRelationExpression(StarkSpecificationLanguageParser.RelationExpressionContext ctx) {
        StarkType leftType = ctx.left.accept(this);
        StarkType rightType = ctx.right.accept(this);
        if (!leftType.canBeMergedWith(rightType)) {
            this.errors.record(ParseUtil.typeError(leftType,rightType, ctx.right.start));
        }
        return (leftType.isRandom()||rightType.isRandom()?new StarkRandomType(StarkType.BOOLEAN_TYPE): StarkType.BOOLEAN_TYPE);
    }

    @Override
    public StarkType visitBracketExpression(StarkSpecificationLanguageParser.BracketExpressionContext ctx) {
        return ctx.expression().accept(this);
    }

    @Override
    public StarkType visitOrExpression(StarkSpecificationLanguageParser.OrExpressionContext ctx) {
        boolean isRandom = checkType(StarkType.BOOLEAN_TYPE, ctx.left);
        checkType(StarkType.BOOLEAN_TYPE, ctx.right);
        return StarkType.BOOLEAN_TYPE;
    }

    @Override
    public StarkType visitIfThenElseExpression(StarkSpecificationLanguageParser.IfThenElseExpressionContext ctx) {
        StarkType guardType = inferAndCheck(StarkType.BOOLEAN_TYPE, ctx.guard);
        StarkType thenType = ctx.thenBranch.accept(this);
        StarkType elseType = ctx.elseBranch.accept(this);
        if (!thenType.canBeMergedWith(elseType)) {
            errors.record(ParseUtil.typeError(thenType, elseType, ctx.elseBranch.start));
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
    public StarkType visitFalseValue(StarkSpecificationLanguageParser.FalseValueContext ctx) {
        return StarkType.BOOLEAN_TYPE;
    }

    @Override
    public StarkType visitRealValue(StarkSpecificationLanguageParser.RealValueContext ctx) {
        return StarkType.REAL_TYPE;
    }

    @Override
    public StarkType visitAndExpression(StarkSpecificationLanguageParser.AndExpressionContext ctx) {
        checkType(StarkType.BOOLEAN_TYPE, ctx.left);
        checkType(StarkType.BOOLEAN_TYPE, ctx.right);
        return StarkType.BOOLEAN_TYPE;
    }

    @Override
    public StarkType visitCallExpression(StarkSpecificationLanguageParser.CallExpressionContext ctx) {
        String functionName = ctx.name.getText();
        if (!context.isAFunction(functionName)) {
            errors.record(ParseUtil.isNotAFunction(ctx.name));
            return StarkType.ERROR_TYPE;
        }
        StarkType[] expectedArguments = context.getArgumentsType(functionName);
        if (ctx.callArguments.size() != expectedArguments.length) {
            errors.record(ParseUtil.illegalNumberOfArguments(ctx.name, expectedArguments.length, ctx.callArguments.size()));
            return StarkType.ERROR_TYPE;
        }
        for(int i=0; i<expectedArguments.length; i++) {
            checkType(expectedArguments[i], ctx.callArguments.get(i));
        }
        return context.getReturnType(functionName);
    }

    @Override
    public StarkType visitMulDivExpression(StarkSpecificationLanguageParser.MulDivExpressionContext ctx) {
        StarkType leftType = checkNumerical(ctx.left);
        StarkType rightType = checkNumerical(ctx.right);
        return StarkType.merge(leftType, rightType);
    }

    @Override
    public StarkType visitAddSubExpression(StarkSpecificationLanguageParser.AddSubExpressionContext ctx) {
        StarkType leftType = checkNumerical(ctx.left);
        StarkType rightType = checkNumerical(ctx.right);
        return StarkType.merge(leftType, rightType);
    }

    @Override
    public StarkType visitUnaryMathCallExpression(StarkSpecificationLanguageParser.UnaryMathCallExpressionContext ctx) {
        checkNumerical(ctx.argument);
        return StarkType.REAL_TYPE;
    }

    @Override
    public StarkType visitUnaryExpression(StarkSpecificationLanguageParser.UnaryExpressionContext ctx) {
        return checkNumerical(ctx.arg);
    }


    @Override
    public StarkType visitNormalExpression(StarkSpecificationLanguageParser.NormalExpressionContext ctx) {
        if (!randomExpressionAllowed) {
            this.errors.record(ParseUtil.illegalUseOfRandomExpression(ctx.start));
            return StarkType.ERROR_TYPE;
        } else {
            if (checkType(StarkType.REAL_TYPE, ctx.mean)&checkType(StarkType.REAL_TYPE, ctx.variance)) {
                return new StarkRandomType( StarkType.REAL_TYPE );
            }
        }
        return StarkType.ERROR_TYPE;
    }

    @Override
    public StarkType visitUniformExpression(StarkSpecificationLanguageParser.UniformExpressionContext ctx) {
        if (!randomExpressionAllowed) {
            this.errors.record(ParseUtil.illegalUseOfRandomExpression(ctx.start));
            return StarkType.ERROR_TYPE;
        }
        StarkType type = null;
        for (StarkSpecificationLanguageParser.ExpressionContext v: ctx.values) {
            StarkType current = v.accept(this);
            if (type == null) {
                type = current;
            } else {
                if (type.canBeMergedWith(current)) {
                    type = StarkType.merge(type, current);
                } else {
                    this.errors.record(ParseUtil.typeError(type, current, v.start));
                    return StarkType.ERROR_TYPE;
                }
            }
        }
        if (type == null) {
            return StarkType.ERROR_TYPE;
        }
        return new StarkRandomType( type );
    }

    @Override
    public StarkType visitRandomExpression(StarkSpecificationLanguageParser.RandomExpressionContext ctx) {
        if (!randomExpressionAllowed) {
            this.errors.record(ParseUtil.illegalUseOfRandomExpression(ctx.start));
            return StarkType.ERROR_TYPE;
        }
        if (ctx.from != null) {
            if (checkType(StarkType.REAL_TYPE, ctx.from)&checkType(StarkType.REAL_TYPE, ctx.to)) {
                return new StarkRandomType( StarkType.REAL_TYPE );
            } else {
                return StarkType.ERROR_TYPE;
            }
        } else {
            return new StarkRandomType( StarkType.REAL_TYPE );
        }
    }



}
