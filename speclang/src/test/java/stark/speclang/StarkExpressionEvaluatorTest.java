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

package stark.speclang;

import stark.speclang.parsing.ParseErrorCollector;
import stark.speclang.semantics.StarkExpressionEvaluator;
import stark.speclang.types.ExpressionTypeInference;
import stark.speclang.types.StarkRandomType;
import stark.speclang.types.StarkType;
import stark.speclang.types.LocalTypeContext;
import stark.speclang.values.StarkInteger;
import stark.speclang.values.StarkBoolean;
import stark.speclang.values.StarkReal;
import stark.speclang.values.StarkValue;
import stark.speclang.variables.StarkExpressionEvaluationContext;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StarkExpressionEvaluatorTest {

    private  static Map<String, StarkValue> valuesTests = new HashMap<>();


    @BeforeAll
    public static void initTypeTests() {
        valuesTests.put("2", new StarkInteger(2));
        valuesTests.put("2.", new StarkReal(2.0));
        valuesTests.put("true", StarkBoolean.TRUE);
        valuesTests.put("false", StarkBoolean.FALSE);
        valuesTests.put("2+3", new StarkInteger(5));
        valuesTests.put("2.+3", new StarkReal(5.0));
        valuesTests.put("2+3.", new StarkReal(5.0));
        valuesTests.put("2.+3.", new StarkReal(5.0));
        valuesTests.put("true & true", StarkBoolean.TRUE);
        valuesTests.put("true | true", StarkBoolean.TRUE);
        valuesTests.put("2 ^ 3", new StarkReal(Math.pow(2, 3)));
        valuesTests.put("2 * 3", new StarkInteger(6));
        valuesTests.put("2. * 3", new StarkReal(6.0));
        valuesTests.put("2 * 3.", new StarkReal(6.0));
        valuesTests.put("2. * 3.", new StarkReal(6.0));
        valuesTests.put("2 < 3", StarkBoolean.TRUE);
        valuesTests.put("2. <= 3", StarkBoolean.TRUE);
        valuesTests.put("2 == 3.", StarkBoolean.FALSE);
        valuesTests.put("2. > 3.", StarkBoolean.FALSE);
        valuesTests.put("2. >= 3", StarkBoolean.FALSE);
        valuesTests.put("!true", StarkBoolean.FALSE);
        valuesTests.put("(2<3?1.0:2.0)", new StarkReal(1.0));
        valuesTests.put("(2>3?1.0:2)", new StarkInteger(2));
        valuesTests.put("abs(1)", new StarkReal(Math.abs(1)));
        valuesTests.put("acos(1)", new StarkReal(Math.acos(1)));
        valuesTests.put("asin(1)", new StarkReal(Math.asin(1)));
        valuesTests.put("atan(1)", new StarkReal(Math.atan(1)));
        valuesTests.put("cbrt(1)", new StarkReal(Math.cbrt(1)));
        valuesTests.put("ceil(1)", new StarkReal(Math.ceil(1)));
        valuesTests.put("cos(1)", new StarkReal(Math.cos(1)));
        valuesTests.put("cosh(1)", new StarkReal(Math.cosh(1)));
        valuesTests.put("exp(1)", new StarkReal(Math.exp(1)));
        valuesTests.put("expm1(1)", new StarkReal(Math.expm1(1)));
        valuesTests.put("floor(1)", new StarkReal(Math.floor(1)));
        valuesTests.put("log(2)", new StarkReal(Math.log(2)));
        valuesTests.put("log10(2)", new StarkReal(Math.log10(2)));
        valuesTests.put("log1p(2)", new StarkReal(Math.log1p(2)));
        valuesTests.put("signum(1)", new StarkReal(Math.signum(1)));
        valuesTests.put("sin(1)", new StarkReal(Math.sin(1)));
        valuesTests.put("sinh(1)", new StarkReal(Math.sinh(1)));
        valuesTests.put("sqrt(1)", new StarkReal(Math.sqrt(1)));
        valuesTests.put("tan(1)", new StarkReal(Math.tan(1)));
        valuesTests.put("atan2(1,2)", new StarkReal(Math.atan2(1,2)));
        valuesTests.put("hypot(1,2)", new StarkReal(Math.hypot(1,2)));
        valuesTests.put("max(1,2)", new StarkReal(Math.max(1,2)));
        valuesTests.put("min(1,2)", new StarkReal(Math.min(1,2)));
        valuesTests.put("pow(2,3)", new StarkReal(Math.pow(2,3)));

    }

    private ParseTree getParseTree(String code) {
        StarkSpecificationLanguageLexer lexer = new StarkSpecificationLanguageLexer(CharStreams.fromString(code));
        CommonTokenStream tokens =  new CommonTokenStream(lexer);
        StarkSpecificationLanguageParser parser = new StarkSpecificationLanguageParser(tokens);
        return parser.expression();
    }


    @Test
    void shouldInferIntegerType() {
        ParseTree parseTree = getParseTree("2");
        assertEquals(StarkType.INTEGER_TYPE, inferTypeOf(parseTree));
    }

    @Test
    void shouldInferIntegerTypeFromVariable() {
        ParseTree parseTree = getParseTree("x");
        assertEquals(StarkType.INTEGER_TYPE, inferTypeOf(Map.of("x", StarkType.INTEGER_TYPE), parseTree));
    }

    @Test
    void shouldInferRealTypeFromVariable() {
        ParseTree parseTree = getParseTree("x");
        assertEquals(StarkType.REAL_TYPE, inferTypeOf(Map.of("x", StarkType.REAL_TYPE), parseTree));
    }

    @Test
    void shouldInferBooleanTypeFromVariable() {
        ParseTree parseTree = getParseTree("x");
        assertEquals(StarkType.BOOLEAN_TYPE, inferTypeOf(Map.of("x", StarkType.BOOLEAN_TYPE), parseTree));
    }


    @Test
    void shouldInferRealType() {
        ParseTree parseTree = getParseTree("2.");
        assertEquals(StarkType.REAL_TYPE, inferTypeOf(parseTree));
    }


    @Test
    void shouldInferBooleanTypeFromTrue() {
        ParseTree parseTree = getParseTree("true");
        assertEquals(StarkType.BOOLEAN_TYPE, inferTypeOf(parseTree));
    }

    @Test
    void shouldInferBooleanTypeFromFalse() {
        ParseTree parseTree = getParseTree("false");
        assertEquals(StarkType.BOOLEAN_TYPE, inferTypeOf(parseTree));
    }

    @Test
    void shouldInferRandomBooleanType() {
        ParseTree parseTree = getParseTree("R < R");
        assertEquals(new StarkRandomType(StarkType.BOOLEAN_TYPE), inferTypeOf(true, parseTree));
    }

    @Test
    void shouldInferRandomRealType() {
        ParseTree parseTree = getParseTree("R");
        assertEquals(new StarkRandomType(StarkType.REAL_TYPE), inferTypeOf(true, parseTree));
    }


    private StarkType inferTypeOf(ParseTree parseTree) {
        return inferTypeOf(false, parseTree);
    }

    private StarkType inferTypeOf(boolean randomExpressionAllowed, ParseTree parseTree) {
        return inferTypeOf(Map.of(), randomExpressionAllowed, parseTree);
    }

    private StarkType inferTypeOf(Map<String, StarkType> types, ParseTree expression) {
        return inferTypeOf(types, false, expression);
    }

    private StarkType inferTypeOf(Map<String, StarkType> types, boolean randomExpressionAllowed, ParseTree expression) {
        ExpressionTypeInference inference = new ExpressionTypeInference(new LocalTypeContext(types), new ParseErrorCollector(), randomExpressionAllowed);
        return expression.accept(inference);
    }


    private StarkValue evalExpression(ParseTree expression, Map<String, StarkValue> args) {
        return expression.accept(new StarkExpressionEvaluator(new StarkExpressionEvaluationContext(args), null)).eval();
    }

    @Test
    public void testExpressions() {
        for (Map.Entry<String, StarkValue> test: valuesTests.entrySet()) {
            assertEquals(test.getValue(), evalExpression(getParseTree(test.getKey()), new HashMap<>()), test.getKey());
        }
    }


}