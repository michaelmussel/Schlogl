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

import stark.ds.DataStateUpdate;
import stark.speclang.variables.StarkStore;
import stark.speclang.variables.StarkVariableAllocation;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class StarkEnvironmentAssignmentFunction extends StarkAbstractEnvironmentFunction {

    private final BiFunction<RandomGenerator, StarkStore, Optional<DataStateUpdate>> assignments;

    public StarkEnvironmentAssignmentFunction(StarkVariableAllocation allocation, BiFunction<RandomGenerator, StarkStore, Optional<DataStateUpdate>> assignments) {
        super(allocation);
        this.assignments = assignments;
    }

    @Override
    public List<DataStateUpdate> apply(RandomGenerator randomGenerator, StarkStore starkStore) {
        Optional<DataStateUpdate> optionalUpdate = assignments.apply(randomGenerator, starkStore);
        return optionalUpdate.map(List::of).orElseGet(List::of);
    }
}
