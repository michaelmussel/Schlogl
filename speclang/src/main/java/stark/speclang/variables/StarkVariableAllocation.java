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

import stark.ds.DataRange;
import stark.ds.DataState;
import stark.ds.DataStateUpdate;
import stark.speclang.types.StarkType;
import stark.speclang.values.StarkValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class is used to allocate variables in data store.
 */
public class StarkVariableAllocation {

    private final Map<StarkVariable, StarkVariableAllocationData> allocationData = new HashMap<>();


    /**
     * Adds the given variable with the given type to this allocation.
     *
     * @param variable declared variable.
     * @param type variable type.
     */
    public void add(StarkVariable variable, StarkType type) {
        if (allocationData.containsKey(variable)) {
            throw new IllegalArgumentException("Duplicated variable "+variable);
        }
        this.allocationData.put(variable, new StarkVariableAllocationData(variable, allocationData.size(), type));
    }

    public void add(StarkVariable variable, StarkType type, DataRange range) {
        if (allocationData.containsKey(variable)) {
            throw new IllegalArgumentException("Duplicated variable "+variable);
        }
        this.allocationData.put(variable, new StarkVariableAllocationData(variable, allocationData.size(), type, range));
    }

    public StarkValue get(StarkVariable variable, DataState state) {
        StarkVariableAllocationData variableAllocationData = allocationData.get(variable);
        if (variableAllocationData == null) {
            return StarkValue.ERROR_VALUE;
        } else {
            return variableAllocationData.get(state);
        }
    }

    public Optional<DataStateUpdate> set(StarkVariable variable, StarkValue value) {
        StarkVariableAllocationData variableAllocationData = allocationData.get(variable);
        if (variableAllocationData == null) {
            return Optional.empty();
        } else {
            return Optional.of(variableAllocationData.set(value));
        }
    }

    public DataState getDataState(Map<StarkVariable, StarkValue> initialValues) {
        DataRange[] range = new DataRange[this.allocationData.size()];
        double[] values = new double[this.allocationData.size()];
        for (StarkVariableAllocationData v: this.allocationData.values()) {
            range[v.index] = v.range;
            values[v.index] = initialValues.get(v.variable).toDouble();
        }
        return new DataState(range, values);
    }

    private static class StarkVariableAllocationData {

        private final StarkVariable variable;

        private final int index;

        private final StarkType type;

        private final DataRange range;

        private StarkVariableAllocationData(StarkVariable variable, int index, StarkType type) {
            this(variable, index, type, type.getDefaultDataRange());
        }

        public StarkVariableAllocationData(StarkVariable variable, int index, StarkType type, DataRange range) {
            this.variable = variable;
            this.index = index;
            this.type = type;
            this.range = range;
        }


        /**
         * Returns the value associated to the variable allocated with this object on the given state.
         *
         * @param state data state used to read variable value.
         * @return the value associated to the variable allocated with this object on the given state.
         */
        public StarkValue get(DataState state) {
            return type.valueOf(state.get(index));
        }

        public DataStateUpdate set(StarkValue value) {
            return new DataStateUpdate(index, value.toDouble());
        }
    }
}
