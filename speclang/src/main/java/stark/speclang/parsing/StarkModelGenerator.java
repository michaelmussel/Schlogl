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

import stark.ControlledSystem;
import stark.SystemSpecification;
import stark.controller.Controller;
import stark.controller.ControllerRegistry;
import stark.controller.ParallelController;
import stark.distance.DistanceExpression;
import stark.ds.*;
import stark.perturbation.Perturbation;
import stark.robtl.RobustnessFormula;
import stark.speclang.StarkSpecificationLanguageBaseVisitor;
import stark.speclang.StarkSpecificationLanguageParser;
import stark.speclang.controller.StarkControllerFunction;
import stark.speclang.semantics.StarkExpressionEvaluationFunction;
import stark.speclang.semantics.StarkExpressionEvaluator;
import stark.speclang.types.StarkCustomType;
import stark.speclang.types.StarkType;
import stark.speclang.values.StarkValue;
import org.antlr.v4.runtime.Token;
import org.apache.commons.math3.random.RandomGenerator;
import stark.speclang.variables.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class StarkModelGenerator extends StarkSpecificationLanguageBaseVisitor<Boolean> {


    private final StarkExpressionEvaluationContext context;

    private final Map<StarkVariable, StarkValue> initialValues = new HashMap<>();

    private final StarkVariableRegistry registry = new StarkVariableRegistry();

    private final Map<String, StarkCustomType> customTypes = new HashMap<>();

    private final StarkVariableAllocation allocation = new StarkVariableAllocation();

    private final ParseErrorCollector errors;
    private BiFunction<RandomGenerator, StarkStore, List<DataStateUpdate>> environmentFunction;

    private final ControllerRegistry controllerRegistry = new ControllerRegistry();

    private final Map<String, StarkControllerFunction> controllerMap = new HashMap<>();

    private Controller controller = null;

    private final Map<String, Perturbation> perturbationMap = new HashMap<>();

    private final Map<String, DistanceExpression> distanceExpressionMap = new HashMap<>();

    private final Map<String, RobustnessFormula> formulaMap = new HashMap<>();

    private final Map<String, DataStateExpression> penalties = new HashMap<>();

    public StarkModelGenerator(ParseErrorCollector errors) {
        this.errors = errors;
        this.context = new StarkExpressionEvaluationContext(new HashMap<>());
    }


    @Override
    public Boolean visitStarkSpecificationModel(StarkSpecificationLanguageParser.StarkSpecificationModelContext ctx) {
        if (!ctx.accept(new StarkGlobalVariableCollector(this.errors, this.registry))) {
            return false;
        }
        boolean flag = true;
        for (StarkSpecificationLanguageParser.ElementContext element: ctx.element()) {
            flag &= element.accept(this);
        }
        return flag;
    }

    @Override
    public Boolean visitDeclarationFunction(StarkSpecificationLanguageParser.DeclarationFunctionContext ctx) {
        StarkVariable[] localVariables = registerLocalVariables(ctx.arguments);
        StarkExpressionEvaluationFunction bodyFunction = StarkFunctionEvaluator.eval(context, registry, ctx.functionBlockStatement());
        context.recordFunction(ctx.name.getText(), (rg, args) -> bodyFunction.eval(rg, StarkStore.storeOf(localVariables, args)));
        return true;
    }

    private StarkVariable[] registerLocalVariables(List<StarkSpecificationLanguageParser.FunctionArgumentContext> arguments) {
        return arguments.stream().map(arg -> registry.getOrRegister(arg.name.getText())).toArray(StarkVariable[]::new);
    }

    @Override
    public Boolean visitDeclarationComponent(StarkSpecificationLanguageParser.DeclarationComponentContext ctx) {
        String componentName = ctx.name.getText();
        for(StarkSpecificationLanguageParser.VariableDeclarationContext v: ctx.variables) {
            recordVariable(v);
        }
        for(StarkSpecificationLanguageParser.ControllerStateDeclarationContext state: ctx.states) {
            String stateName = state.name.getText();
            StarkControllerFunction function = StarkControllerStateGenerator.generate(context, registry, allocation, controllerMap, controllerRegistry, state.body);
            controllerRegistry.set(stateName, StarkControllerFunction.toController(allocation, function));
            controllerMap.put(stateName, function);
        }
        Controller componentController = ctx.controller.accept(new StarkControllerGenerator(controllerRegistry));
        if (controller == null) {
            controller = componentController;
        } else {
            controller = new ParallelController(controller, componentController);
        }
        return true;
    }

    @Override
    public Boolean visitDeclarationPenalty(StarkSpecificationLanguageParser.DeclarationPenaltyContext ctx) {
        String penaltyName = ctx.name.getText();
        StarkExpressionEvaluationFunction value = StarkExpressionEvaluator.eval(context, registry, ctx.value);
        penalties.put(penaltyName, ds -> value.eval(StarkStore.storeOf(allocation, ds)).toDouble());
        return true;
    }

    @Override
    public Boolean visitDeclarationEnvironmnet(StarkSpecificationLanguageParser.DeclarationEnvironmnetContext ctx) {
        this.environmentFunction = ctx.block.accept(new StarkEnvironmentGenerator(this.allocation, this.context, this.registry));
        return this.environmentFunction != null;
    }

    @Override
    public Boolean visitDeclarationType(StarkSpecificationLanguageParser.DeclarationTypeContext ctx) {
        String typeName = ctx.name.getText();
        StarkCustomType customType = new StarkCustomType(typeName, ctx.elements.stream().map(t -> t.name.getText()).toArray(String[]::new));
        context.recordType(ctx.name.getText(), customType);
        customTypes.put(typeName, customType);
        return true;
    }

    @Override
    public Boolean visitDeclarationVariables(StarkSpecificationLanguageParser.DeclarationVariablesContext ctx) {
        for(StarkSpecificationLanguageParser.VariableDeclarationContext v: ctx.variableDeclaration()) {
            recordVariable(v);
        }
        return true;
    }

    private void recordVariable(StarkSpecificationLanguageParser.VariableDeclarationContext v) {
        StarkVariable variable = registry.get(v.name.getText());
        StarkType type = getType(v.type());
        if (v.from != null) {
            double from = StarkExpressionEvaluator.evalToValue(this.context, this.registry, v.from).toDouble();
            double to = StarkExpressionEvaluator.evalToValue(this.context, this.registry, v.to).toDouble();
            this.allocation.add(variable, type, new DataRange(from, to));
        } else {
            this.allocation.add(variable, type);
        }
        this.initialValues.put(variable, StarkExpressionEvaluator.evalToValue(this.context, this.registry, v.value));
    }

    private StarkType getType(StarkSpecificationLanguageParser.TypeContext type) {
        if (type instanceof StarkSpecificationLanguageParser.IntegerTypeContext) {
            return StarkType.INTEGER_TYPE;
        }
        if (type instanceof StarkSpecificationLanguageParser.RealTypeContext) {
            return StarkType.REAL_TYPE;
        }
        if (type instanceof StarkSpecificationLanguageParser.BooleanTypeContext) {
            return StarkType.INTEGER_TYPE;
        }
        if (type instanceof StarkSpecificationLanguageParser.CustomTypeContext) {
            String typeName = ((StarkSpecificationLanguageParser.CustomTypeContext) type).name.getText();
            if (customTypes.containsKey(typeName)) {
                return customTypes.get(typeName);
            }
        }
        return StarkType.ERROR_TYPE;
    }

    @Override
    public Boolean visitDeclarationParameter(StarkSpecificationLanguageParser.DeclarationParameterContext ctx) {
        return recordValue(ctx.name, ctx.expression());
    }

    @Override
    public Boolean visitDeclarationConstant(StarkSpecificationLanguageParser.DeclarationConstantContext ctx) {
        return recordValue(ctx.name, ctx.expression());
    }

    private Boolean recordValue(Token name, StarkSpecificationLanguageParser.ExpressionContext expression) {
        this.context.set(name.getText(), StarkExpressionEvaluator.evalToValue(this.context, this.registry, expression));
        return true;
    }

    public SystemSpecification getSystemSpecification() {
        return new SystemSpecification(getControlledSystem(), this.penalties, this.formulaMap, this.perturbationMap, this.distanceExpressionMap);
    }

    private ControlledSystem getControlledSystem() {
        return new ControlledSystem(this.controller, getEnvironment(), getDataState());
    }

    private DataState getDataState() {
        return allocation.getDataState(initialValues);
    }

    private DataStateFunction getEnvironment() {
        return (rg, ds) -> ds.apply(this.environmentFunction.apply(rg, StarkStore.storeOf(allocation, ds)));
    }

    @Override
    public Boolean visitDeclarationFormula(StarkSpecificationLanguageParser.DeclarationFormulaContext ctx) {
        this.formulaMap.put(ctx.name.getText(), ctx.value.accept(new StarkRobustnessFormulaGenerator(allocation, context, registry, perturbationMap, distanceExpressionMap, formulaMap)));
        return true;
    }

    @Override
    public Boolean visitDeclarationDistance(StarkSpecificationLanguageParser.DeclarationDistanceContext ctx) {
        this.distanceExpressionMap.put(ctx.name.getText(), ctx.value.accept(new StarkDistanceGenerator(allocation, context, registry, distanceExpressionMap, penalties)));
        return true;
    }

    @Override
    public Boolean visitDeclarationPerturbation(StarkSpecificationLanguageParser.DeclarationPerturbationContext ctx) {
        this.perturbationMap.put(ctx.name.getText(), ctx.value.accept(new StarkPerturbationGenerator(allocation, context, registry, perturbationMap)));
        return true;
    }
}
