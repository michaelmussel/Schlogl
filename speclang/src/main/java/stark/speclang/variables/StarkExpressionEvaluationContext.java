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

package stark.speclang.variables;

import stark.speclang.semantics.StarkFunction;
import stark.speclang.types.StarkCustomType;
import stark.speclang.values.StarkCustomValue;
import stark.speclang.values.StarkValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class StarkExpressionEvaluationContext {

    private final Map<String, StarkValue> values;

    private final Map<String, StarkFunction> functions;

    private final Map<String, StarkCustomValue> customValues;

    public StarkExpressionEvaluationContext(Map<String, StarkValue> parameters) {
        values = new HashMap<>();
        functions = new HashMap<>();
        customValues = new HashMap<>();
        values.putAll(parameters);
    }


    public void set(String name, StarkValue value) {
        this.values.put(name, value);
    }

    public boolean isDefined(String name) {
        return values.containsKey(name)||customValues.containsKey(name);
    }

    public StarkValue get(String name) {
        if (this.values.containsKey(name)) {
            return this.values.get(name);
        }
        if (this.customValues.containsKey(name)) {
            return this.values.get(name);
        }
        return StarkValue.ERROR_VALUE;
    }

    public boolean isAFunction(String name) {
        return this.functions.containsKey(name);
    }

    public StarkFunction getFunction(String name) {
        return this.functions.get(name);
    }

    public void recordFunction(String name, StarkFunction function) {
        this.functions.put(name, function);
    }

    public void recordType(String typeName, StarkCustomType type) {
        StarkCustomValue[] values = type.getValues();
        Arrays.stream(values).sequential().forEach(v -> this.customValues.put(v.name(), v));
    }
}
