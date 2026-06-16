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

import stark.ds.DataState;
import stark.speclang.values.StarkValue;

import java.util.HashMap;
import java.util.Map;

/**
 * This interface represents the store used to evaluate expressions. Each store associates variables with values.
 */
public interface StarkStore {

    static StarkStore storeOf(Map<StarkVariable, StarkValue> localStore, StarkStore starkStore) {
        return v -> {
            if (localStore.containsKey(v)) {
                return localStore.get(v);
            } else {
                return starkStore.get(v);
            }
        };
    }

    static StarkStore storeOf(StarkVariable[] localVariables, StarkValue[] args) {
        Map<StarkVariable, StarkValue> localStore = new HashMap<>();
        for(int i=0; i<localVariables.length; i++) {
            localStore.put(localVariables[i], args[i]);
        }
        return StarkStore.storeOf(localStore);
    }

    /**
     * Returns the value associated with the given variable.
     *
     * @param variable variable to read.
     * @return the value associated with the given element index.
     */
    StarkValue get(StarkVariable variable);


    /**
     * Returns a new store that enriches the given one with the binging of <code>variable</code> to <code>value</code>.
     *
     * @param variable assigned variables
     * @param value assigned value
     * @param store enriched value
     * @return a new store that enriches the given one with the binging of <code>variable</code> to <code>value</code>.
     */
    static StarkStore let(StarkVariable variable, StarkValue value, StarkStore store) {
        return v -> (variable.equals(v)?value:store.get(variable));
    }

    /**
     * Returns the store whose binding are the same as the given map.
     *
     * @param map a map associating variables to values.
     * @return the store whose binding are the same as the given map.
     */
    static StarkStore storeOf(Map<StarkVariable, StarkValue> map) {
        return v -> map.getOrDefault(v, StarkValue.ERROR_VALUE);
    }

    /**
     * Returns the store whose binding are defined in terms of the given allocation and data state.
     *
     * @param allocation variable allocation.
     * @param state data state.
     * @return the store whose binding are defined in terms of the given allocation and data state.
     */
    static StarkStore storeOf(StarkVariableAllocation allocation, DataState state) {
        return v -> allocation.get(v, state);
    }
}


