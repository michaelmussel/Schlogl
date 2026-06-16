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

package stark.speclang.controller;

import stark.controller.Controller;
import stark.controller.EffectStep;
import stark.speclang.semantics.StarkExpressionEvaluationFunction;
import stark.speclang.values.StarkValue;
import stark.speclang.variables.StarkStore;
import stark.speclang.variables.StarkVariableAllocation;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.List;

@FunctionalInterface
public interface StarkControllerFunction {


    EffectStep<Controller> apply(RandomGenerator rg, StarkStore store);


    static Controller toController(StarkVariableAllocation allocation, StarkControllerFunction function) {
        return (rg, ds) -> function.apply(rg, StarkStore.storeOf(allocation, ds));
    }

    static StarkControllerFunction ifThenElse(StarkExpressionEvaluationFunction guard, StarkControllerFunction thenCase, StarkControllerFunction elseCase) {
        return (rg, s) -> {
            if (StarkValue.isTrue(guard.eval(rg, s))) {
                return thenCase.apply(rg, s);
            } else {
                return elseCase.apply(rg, s);
            }
        };
    }

    static StarkControllerFunction sequential(List<StarkControllerFunction> functions) {
        return (rg, s) -> {
            EffectStep<Controller> effect = new EffectStep<>(List.of(), null);
            for (StarkControllerFunction function : functions) {
                effect = effect.applyAfter(function.apply(rg, s));
                if (effect.isCompleted()) return effect;
            }
            return effect;
        };
    }

}
