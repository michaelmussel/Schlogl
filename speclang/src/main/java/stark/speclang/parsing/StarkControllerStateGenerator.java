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

import stark.controller.Controller;
import stark.controller.ControllerRegistry;
import stark.controller.EffectStep;
import stark.ds.DataStateUpdate;
import stark.speclang.StarkSpecificationLanguageBaseVisitor;
import stark.speclang.StarkSpecificationLanguageParser;
import stark.speclang.controller.StarkControllerFunction;
import stark.speclang.semantics.StarkExpressionEvaluationFunction;
import stark.speclang.semantics.StarkExpressionEvaluator;
import stark.speclang.values.StarkValue;
import stark.speclang.variables.*;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class StarkControllerStateGenerator extends StarkSpecificationLanguageBaseVisitor<StarkControllerFunction> {

    private final StarkExpressionEvaluationContext context;
    private final StarkVariableRegistry registry;
    private final StarkVariableAllocation allocation;
    private final Map<String, StarkControllerFunction> controllerMap;

    private final ControllerRegistry controllerRegistry;


    public StarkControllerStateGenerator(StarkExpressionEvaluationContext context, StarkVariableRegistry registry, StarkVariableAllocation allocation, Map<String, StarkControllerFunction> controllerMap, ControllerRegistry controllerRegistry) {
        this.context = context;
        this.registry = registry;
        this.allocation = allocation;
        this.controllerMap = controllerMap;
        this.controllerRegistry = controllerRegistry;
    }

    public static StarkControllerFunction generate(StarkExpressionEvaluationContext context, StarkVariableRegistry registry, StarkVariableAllocation allocation, Map<String, StarkControllerFunction> controllerMap, ControllerRegistry controllerRegistry, StarkSpecificationLanguageParser.ControllerBlockBehaviourContext body) {
        return body.accept(new StarkControllerStateGenerator(context, registry, allocation, controllerMap, controllerRegistry));
    }


    @Override
    public StarkControllerFunction visitControllerBlockBehaviour(StarkSpecificationLanguageParser.ControllerBlockBehaviourContext ctx) {
        return StarkControllerFunction.sequential(ctx.controllerCommand().stream().map(it -> it.accept(this)).collect(Collectors.toList()));
    }

    @Override
    public StarkControllerFunction visitControllerSequentialBehaviour(StarkSpecificationLanguageParser.ControllerSequentialBehaviourContext ctx) {
        BiFunction<RandomGenerator, StarkStore, List<DataStateUpdate>> assignments = getVariableAssignments(ctx.statements);
        StarkControllerFunction next = ctx.last.accept(this);
        return (rg, ds) -> next.apply(rg, ds).applyBefore(assignments.apply(rg, ds));
    }

    @Override
    public StarkControllerFunction visitControllerVariableAssignment(StarkSpecificationLanguageParser.ControllerVariableAssignmentContext ctx) {
        BiFunction<RandomGenerator, StarkStore, Optional<DataStateUpdate>> assignment = getVariableAssignment(ctx);
        return (rg, ds) -> {
            Optional<DataStateUpdate> result = assignment.apply(rg, ds);
            return result.<EffectStep<Controller>>map(dataStateUpdate -> new EffectStep<>(List.of(dataStateUpdate))).orElseGet(EffectStep::new);
        };
    }

    private BiFunction<RandomGenerator, StarkStore, List<DataStateUpdate>> getVariableAssignments(List<StarkSpecificationLanguageParser.ControllerVariableAssignmentContext> statements) {
        List<BiFunction<RandomGenerator, StarkStore, Optional<DataStateUpdate>>> assignments = statements.stream().map(this::getVariableAssignment).toList();
        return (rg, store) -> assignments.stream().map(a -> a.apply(rg, store)).filter(Optional::isPresent).map(Optional::get).toList();
    }

    @Override
    public StarkControllerFunction visitControllerCaseStatment(StarkSpecificationLanguageParser.ControllerCaseStatmentContext ctx) {
        //TODO: FIXME!
        return super.visitControllerCaseStatment(ctx);
    }

    @Override
    public StarkControllerFunction visitControllerLetAssignment(StarkSpecificationLanguageParser.ControllerLetAssignmentContext ctx) {
        StarkVariable variable = registry.getOrRegister(ctx.name.getText());
        StarkExpressionEvaluationFunction valueFunction = StarkExpressionEvaluator.eval(context, registry, ctx.value);
        StarkControllerFunction letBody = ctx.body.accept(this);
        return (rg, store) -> letBody.apply(rg, StarkStore.let(variable, valueFunction.eval(rg, store), store));
    }

    public BiFunction<RandomGenerator, StarkStore, Optional<DataStateUpdate>> getVariableAssignment(StarkSpecificationLanguageParser.ControllerVariableAssignmentContext ctx) {
        StarkExpressionEvaluationFunction valueFunction = StarkExpressionEvaluator.eval(context, registry, ctx.value);
        StarkVariable variable = registry.get(StarkVariable.getTargetVariableName(ctx.target.name.getText()));
        if (ctx.guard != null) {
            StarkExpressionEvaluationFunction guardFunction = StarkExpressionEvaluator.eval(context, registry, ctx.guard);
            return (rg, s) -> {
                if (StarkValue.isTrue(guardFunction.eval(rg, s))) {
                    return allocation.set(variable, valueFunction.eval(rg, s));
                } else {
                    return Optional.empty();
                }
            };
        } else {
            return (rg, s) -> allocation.set(variable, valueFunction.eval(rg, s));
        }
    }

    @Override
    public StarkControllerFunction visitControllerExecAction(StarkSpecificationLanguageParser.ControllerExecActionContext ctx) {
        String referencedName = ctx.target.getText();
        return (rg, store) -> {
            StarkControllerFunction function = controllerMap.get(referencedName);
            if (function != null) {
                return function.apply(rg, store);
            } else {
                return ControllerRegistry.NIL.next(rg, null);
            }
        };
    }

    @Override
    public StarkControllerFunction visitControllerStepAtion(StarkSpecificationLanguageParser.ControllerStepAtionContext ctx) {
        String referencedName = ctx.target.getText();
        Controller controller = controllerRegistry.reference(referencedName);
        if (ctx.steps == null) {
            return (rg, store) -> new EffectStep<>(List.of(), controller);
        } else {
            StarkExpressionEvaluationFunction steps = StarkExpressionEvaluator.eval(context, registry, ctx.steps);
            return (rg, store) -> {
                int k = (int) steps.eval(rg, store).toDouble();
                if (k<1) {
                    return new EffectStep<>(List.of(), controller);
                } else {
                    return new EffectStep<>(List.of(), Controller.doTick(k-1, controller));
                }
            };
        }
    }

    @Override
    public StarkControllerFunction visitControllerIfThenElseBehaviour(StarkSpecificationLanguageParser.ControllerIfThenElseBehaviourContext ctx) {
        return StarkControllerFunction.ifThenElse(StarkExpressionEvaluator.eval(context, registry, ctx.guard),
                ctx.thenBranch.accept(this),
                ctx.elseBranch.accept(this));
    }
}
