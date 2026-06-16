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

package stark.speclang.semantics;

import stark.ds.DataStateExpression;
import stark.speclang.StarkSpecificationLanguageBaseVisitor;
import stark.speclang.StarkSpecificationLanguageParser;
import stark.speclang.values.StarkInteger;
import stark.speclang.values.StarkBoolean;
import stark.speclang.values.StarkReal;
import stark.speclang.values.StarkValue;
import stark.speclang.variables.*;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.DoubleUnaryOperator;

public class StarkExpressionEvaluator extends StarkSpecificationLanguageBaseVisitor<StarkExpressionEvaluationFunction> {


    private final static Map<String, BiFunction<StarkValue, StarkValue, StarkValue>> binaryOperators = Map.of(
            "+", StarkValue::sum,
            "*", StarkValue::product,
            "-", StarkValue::subtraction,
            "/", StarkValue::division, //TODO: Check how to handle division by zero!
            "%", StarkValue::modulo,
            "atan2", (x,y) -> StarkValue.apply(Math::atan2, x, y),
            "hypot", (x,y) -> StarkValue.apply(Math::hypot, x, y),
            "max", (x,y) -> StarkValue.apply(Math::max, x, y),
            "min", (x,y) -> StarkValue.apply(Math::min, x, y),
            "pow",   (x,y) -> StarkValue.apply(Math::pow, x, y)
    );

    private final static Map<String, DoubleUnaryOperator> unaryOperators = Map.ofEntries(
            Map.entry("+", x -> +x),
            Map.entry("-", x -> -x),
            Map.entry("abs", Math::abs),
            Map.entry("acos", Math::acos),
            Map.entry("asin", Math::asin),
            Map.entry("atan", Math::atan),
            Map.entry("cbrt", Math::cbrt),
            Map.entry("ceil", Math::ceil),
            Map.entry("cos", Math::cos),
            Map.entry("cosh", Math::cosh),
            Map.entry("exp", Math::exp),
            Map.entry("expm1", Math::expm1),
            Map.entry("floor", Math::floor),
            Map.entry("log", Math::log),
            Map.entry("log10", Math::log10),
            Map.entry("log1p", Math::log1p),
            Map.entry("signum", Math::signum),
            Map.entry("sin", Math::sin),
            Map.entry("sinh", Math::sinh),
            Map.entry("sqrt", Math::sqrt),
            Map.entry("tan", Math::tan)
    );

    private final StarkExpressionEvaluationContext context;

    private final StarkVariableRegistry registry;


    public StarkExpressionEvaluator(StarkExpressionEvaluationContext context, StarkVariableRegistry registry) {
        this.context = context;
        this.registry = registry;
    }

    public static StarkValue evalToValue(StarkExpressionEvaluationContext context, StarkVariableRegistry registry, StarkSpecificationLanguageParser.ExpressionContext expression) {
        return expression.accept(new StarkExpressionEvaluator(context, registry)).eval(null, null);
    }

    public static StarkExpressionEvaluationFunction eval(StarkExpressionEvaluationContext context, StarkVariableRegistry registry, StarkSpecificationLanguageParser.ExpressionContext expression) {
        return expression.accept(new StarkExpressionEvaluator(context, registry));
    }

    public static DataStateExpression evalToDataStateExpression(StarkVariableAllocation allocation, StarkExpressionEvaluationContext context, StarkVariableRegistry registry, StarkSpecificationLanguageParser.ExpressionContext expression) {
        StarkExpressionEvaluationFunction evaluation = expression.accept(new StarkExpressionEvaluator(context, registry));
        return ds -> evaluation.eval(StarkStore.storeOf(allocation, ds)).toDouble();
    }

    @Override
    public StarkExpressionEvaluationFunction visitNegationExpression(StarkSpecificationLanguageParser.NegationExpressionContext ctx) {
        StarkExpressionEvaluationFunction arg = ctx.arg.accept(this);
        return (rg, s) -> StarkValue.negate(arg.eval(rg, s));
    }

    @Override
    public StarkExpressionEvaluationFunction visitExponentExpression(StarkSpecificationLanguageParser.ExponentExpressionContext ctx) {
        StarkExpressionEvaluationFunction leftEvaluation = ctx.left.accept(this);
        StarkExpressionEvaluationFunction rightEvaluation = ctx.right.accept(this);
        return (rg, s) -> StarkValue.apply(Math::pow, leftEvaluation.eval(rg, s), rightEvaluation.eval(rg, s));
    }

    private StarkExpressionEvaluationFunction evalBinary(BiFunction<StarkValue, StarkValue, StarkValue> op, StarkSpecificationLanguageParser.ExpressionContext firstArgument, StarkSpecificationLanguageParser.ExpressionContext secondArgument) {
        StarkExpressionEvaluationFunction firstArgumentEvaluation = firstArgument.accept(this);
        StarkExpressionEvaluationFunction secondArgumentEvaluation = secondArgument.accept(this);
        return (rg, s) -> op.apply(firstArgumentEvaluation.eval(rg, s), secondArgumentEvaluation.eval(rg, s));
    }


    @Override
    public StarkExpressionEvaluationFunction visitBinaryMathCallExpression(StarkSpecificationLanguageParser.BinaryMathCallExpressionContext ctx) {
        return evalBinary(getBinaryOperator(ctx.binaryMathFunction().start.getText()), ctx.left, ctx.right);
    }

    private BiFunction<StarkValue, StarkValue, StarkValue> getBinaryOperator(String op) {
        return binaryOperators.getOrDefault(op, (x,y) -> StarkValue.ERROR_VALUE);
    }

    @Override
    public StarkExpressionEvaluationFunction visitTrueValue(StarkSpecificationLanguageParser.TrueValueContext ctx) {
        return StarkExpressionEvaluationFunction.of( StarkBoolean.TRUE );
    }


    @Override
    public StarkExpressionEvaluationFunction visitRelationExpression(StarkSpecificationLanguageParser.RelationExpressionContext ctx) {
        return evalBinary(getRelationOperator(ctx.op.getText()),ctx.left, ctx.right);
    }

    private BiFunction<StarkValue, StarkValue, StarkValue> getRelationOperator(String op) {
        return switch (op) {
            case "<" -> StarkValue::isLessThan;
            case "<=" -> StarkValue::isLessOrEqualThan;
            case "==" -> StarkValue::isEqualTo;
            case ">=" -> StarkValue::isGreaterOrEqualThan;
            case ">" -> StarkValue::isGreaterThan;
            default -> (x, y) -> StarkValue.ERROR_VALUE;
        };
    }

    @Override
    public StarkExpressionEvaluationFunction visitBracketExpression(StarkSpecificationLanguageParser.BracketExpressionContext ctx) {
        return ctx.expression().accept(this);
    }

    @Override
    public StarkExpressionEvaluationFunction visitFalseValue(StarkSpecificationLanguageParser.FalseValueContext ctx) {
        return StarkExpressionEvaluationFunction.of(StarkBoolean.FALSE);
    }

    @Override
    public StarkExpressionEvaluationFunction visitAndExpression(StarkSpecificationLanguageParser.AndExpressionContext ctx) {
        return evalBinary(StarkValue::and, ctx.left, ctx.right);
    }


    @Override
    public StarkExpressionEvaluationFunction visitUnaryMathCallExpression(StarkSpecificationLanguageParser.UnaryMathCallExpressionContext ctx) {
        return evalUnary(unaryOperators.get(ctx.unaryMathFunction().start.getText()), ctx.argument);
    }

    private StarkExpressionEvaluationFunction evalUnary(DoubleUnaryOperator op, StarkSpecificationLanguageParser.ExpressionContext argument) {
        StarkExpressionEvaluationFunction argumentEvaluator = argument.accept(this);
        return (rg, s) -> StarkValue.apply(op, argumentEvaluator.eval(rg, s));
    }


    @Override
    public StarkExpressionEvaluationFunction visitUnaryExpression(StarkSpecificationLanguageParser.UnaryExpressionContext ctx) {
        return evalUnary(unaryOperators.get(ctx.op.getText()), ctx.arg);
    }


    @Override
    public StarkExpressionEvaluationFunction visitReferenceExpression(StarkSpecificationLanguageParser.ReferenceExpressionContext ctx) {
        String name = ctx.name.getText();
        return  getTargetEvaluationFunction(name);
    }

    private StarkExpressionEvaluationFunction getTargetEvaluationFunction(String name) {
        if (context.isDefined(name)) {
            return StarkExpressionEvaluationFunction.of(context.get(name));
        }
        if (registry.isDeclared(name)) {
            StarkVariable variable = registry.get(name);
            return (rg, s) -> s.get(variable);
        }
        return StarkExpressionEvaluationFunction.of(StarkValue.ERROR_VALUE);
    }


    @Override
    public StarkExpressionEvaluationFunction visitIntValue(StarkSpecificationLanguageParser.IntValueContext ctx) {
        StarkValue value = new StarkInteger(Integer.parseInt(ctx.getText()));
        return StarkExpressionEvaluationFunction.of(value);
    }

    @Override
    public StarkExpressionEvaluationFunction visitNormalExpression(StarkSpecificationLanguageParser.NormalExpressionContext ctx) {
        StarkExpressionEvaluationFunction meanEvaluation = ctx.mean.accept(this);
        StarkExpressionEvaluationFunction varianceEvaluation = ctx.variance.accept(this);
        return (rg, s) -> StarkValue.sampleNormal(rg, meanEvaluation.eval(rg, s), varianceEvaluation.eval(rg, s));
    }

    @Override
    public StarkExpressionEvaluationFunction visitUniformExpression(StarkSpecificationLanguageParser.UniformExpressionContext ctx) {
        StarkExpressionEvaluationFunction[] elements = ctx.expression().stream().map(e -> e.accept(this)).toArray(StarkExpressionEvaluationFunction[]::new);
        return (rg, s) -> {
            int selected = rg.nextInt(elements.length);
            return elements[selected].eval(rg, s);
        };
    }


    @Override
    public StarkExpressionEvaluationFunction visitOrExpression(StarkSpecificationLanguageParser.OrExpressionContext ctx) {
        StarkExpressionEvaluationFunction leftEvaluation = ctx.left.accept(this);
        StarkExpressionEvaluationFunction rightEvaluation = ctx.right.accept(this);
        return (rg, s) -> StarkValue.or( leftEvaluation.eval(rg, s), rightEvaluation.eval(rg, s));
    }

    @Override
    public StarkExpressionEvaluationFunction visitIfThenElseExpression(StarkSpecificationLanguageParser.IfThenElseExpressionContext ctx) {
        StarkExpressionEvaluationFunction guardEvaluation = ctx.guard.accept(this);
        StarkExpressionEvaluationFunction thenEvaluation = ctx.thenBranch.accept(this);
        StarkExpressionEvaluationFunction elseEvaluation = ctx.elseBranch.accept(this);
        return (rg, s) -> StarkValue.ifThenElse(guardEvaluation.eval(rg, s), () -> thenEvaluation.eval(rg, s), () -> elseEvaluation.eval(rg, s));
    }

    @Override
    public StarkExpressionEvaluationFunction visitRealValue(StarkSpecificationLanguageParser.RealValueContext ctx) {
        StarkValue value = new StarkReal(Double.parseDouble(ctx.getText()));
        return StarkExpressionEvaluationFunction.of(value);
    }

    @Override
    public StarkExpressionEvaluationFunction visitCallExpression(StarkSpecificationLanguageParser.CallExpressionContext ctx) {
        String functionName = ctx.name.getText();
        if (context.isAFunction(functionName)) {
            StarkExpressionEvaluationFunction[] arguments = ctx.callArguments.stream().map(e -> e.accept(this)).toArray(StarkExpressionEvaluationFunction[]::new);
            StarkFunction function = context.getFunction(functionName);
            return (rg, s) -> function.apply(rg, Arrays.stream(arguments).map(e -> e.eval(rg, s)).toArray(StarkValue[]::new) );
        } else {
            return StarkExpressionEvaluationFunction.of(StarkValue.ERROR_VALUE);
        }
    }



    @Override
    public StarkExpressionEvaluationFunction visitMulDivExpression(StarkSpecificationLanguageParser.MulDivExpressionContext ctx) {
        return evalBinary(getBinaryOperator(ctx.op.getText()), ctx.left, ctx.right);
    }

    @Override
    public StarkExpressionEvaluationFunction visitAddSubExpression(StarkSpecificationLanguageParser.AddSubExpressionContext ctx) {
        return evalBinary(getBinaryOperator(ctx.op.getText()), ctx.left, ctx.right);
    }

    @Override
    public StarkExpressionEvaluationFunction visitRandomExpression(StarkSpecificationLanguageParser.RandomExpressionContext ctx) {
        if (ctx.from == null) {
            return (rg, s) -> new StarkReal(rg.nextDouble());
        } else {
            StarkExpressionEvaluationFunction fromEvaluation = ctx.from.accept(this);
            StarkExpressionEvaluationFunction toEvaluation = ctx.to.accept(this);
            return (rg, s) -> StarkValue.sample(rg, fromEvaluation.eval(rg, s), toEvaluation.eval(rg, s));
        }
    }



    private StarkExpressionEvaluationFunction getExpressionEvaluationFunction(StarkSpecificationLanguageParser.ExpressionContext expression) {
        return expression.accept(new StarkExpressionEvaluator(context, registry));
    }


}
