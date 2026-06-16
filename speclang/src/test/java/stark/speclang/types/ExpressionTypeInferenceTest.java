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

import stark.speclang.StarkSpecificationLanguageLexer;
import stark.speclang.StarkSpecificationLanguageParser;
import stark.speclang.parsing.ParseErrorCollector;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionTypeInferenceTest {

    private final static Map<String, StarkType> typeTests = new HashMap<>();


    @BeforeAll
    public static void initTypeTests() {
        typeTests.put("2", StarkType.INTEGER_TYPE);
        typeTests.put("2.", StarkType.REAL_TYPE);
        typeTests.put("true", StarkType.BOOLEAN_TYPE);
        typeTests.put("false", StarkType.BOOLEAN_TYPE);
        typeTests.put("2+3", StarkType.INTEGER_TYPE);
        typeTests.put("2.+3", StarkType.REAL_TYPE);
        typeTests.put("2+3.", StarkType.REAL_TYPE);
        typeTests.put("2.+3.", StarkType.REAL_TYPE);
        typeTests.put("true & true", StarkType.BOOLEAN_TYPE);
        typeTests.put("true | true", StarkType.BOOLEAN_TYPE);
        typeTests.put("2 ^ 3", StarkType.REAL_TYPE);
        typeTests.put("2 * 3", StarkType.INTEGER_TYPE);
        typeTests.put("2. * 3", StarkType.REAL_TYPE);
        typeTests.put("2 * 3.", StarkType.REAL_TYPE);
        typeTests.put("2. * 3.", StarkType.REAL_TYPE);
        typeTests.put("2 + 3", StarkType.INTEGER_TYPE);
        typeTests.put("2. + 3", StarkType.REAL_TYPE);
        typeTests.put("2 + 3.", StarkType.REAL_TYPE);
        typeTests.put("2. + 3.", StarkType.REAL_TYPE);
        typeTests.put("2. < 3", StarkType.BOOLEAN_TYPE);
        typeTests.put("!true", StarkType.BOOLEAN_TYPE);
        typeTests.put("(2<3?1.0:2.0)", StarkType.REAL_TYPE);
        typeTests.put("(2<3?1.0:2)", StarkType.REAL_TYPE);
        typeTests.put("(2<3?1:2.0)", StarkType.REAL_TYPE);
        typeTests.put("(2<3?1:2)", StarkType.INTEGER_TYPE);
        typeTests.put("abs(1)", StarkType.REAL_TYPE);
        typeTests.put("acos(1)", StarkType.REAL_TYPE);
        typeTests.put("asin(1)", StarkType.REAL_TYPE);
        typeTests.put("atan(1)", StarkType.REAL_TYPE);
        typeTests.put("cbrt(1)", StarkType.REAL_TYPE);
        typeTests.put("ceil(1)", StarkType.REAL_TYPE);
        typeTests.put("cos(1)", StarkType.REAL_TYPE);
        typeTests.put("cosh(1)", StarkType.REAL_TYPE);
        typeTests.put("exp(1)", StarkType.REAL_TYPE);
        typeTests.put("expm1(1)", StarkType.REAL_TYPE);
        typeTests.put("floor(1)", StarkType.REAL_TYPE);
        typeTests.put("log(1)", StarkType.REAL_TYPE);
        typeTests.put("log10(1)", StarkType.REAL_TYPE);
        typeTests.put("log1p(1)", StarkType.REAL_TYPE);
        typeTests.put("signum(1)", StarkType.REAL_TYPE);
        typeTests.put("sin(1)", StarkType.REAL_TYPE);
        typeTests.put("sinh(1)", StarkType.REAL_TYPE);
        typeTests.put("sqrt(1)", StarkType.REAL_TYPE);
        typeTests.put("tan(1)", StarkType.REAL_TYPE);
        typeTests.put("atan2(1,2)", StarkType.REAL_TYPE);
        typeTests.put("hypot(1,2)", StarkType.REAL_TYPE);
        typeTests.put("max(1,2)", StarkType.REAL_TYPE);
        typeTests.put("min(1,2)", StarkType.REAL_TYPE);
        typeTests.put("pow(1,2)", StarkType.REAL_TYPE);
        typeTests.put("N[0.,1.]", new StarkRandomType(StarkType.REAL_TYPE));
        typeTests.put("N[0,1]", new StarkRandomType(StarkType.REAL_TYPE));
        typeTests.put("U[true,false]", new StarkRandomType(StarkType.BOOLEAN_TYPE));
        typeTests.put("U[1,2,3]", new StarkRandomType(StarkType.INTEGER_TYPE));
        typeTests.put("U[1.0,2,3]", new StarkRandomType(StarkType.REAL_TYPE));
        typeTests.put("R", new StarkRandomType(StarkType.REAL_TYPE));
        typeTests.put("R[1, 10]", new StarkRandomType(StarkType.REAL_TYPE));
        typeTests.put("(R<R)", new StarkRandomType(StarkType.BOOLEAN_TYPE));
        typeTests.put("(R<R?1:2)", new StarkRandomType(StarkType.INTEGER_TYPE));

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


    @Test
    public void testExpressions() {
        for (Map.Entry<String, StarkType> test: typeTests.entrySet()) {
            assertEquals(test.getValue(), inferTypeOf(true, getParseTree(test.getKey())), test.getKey());
        }
    }


}