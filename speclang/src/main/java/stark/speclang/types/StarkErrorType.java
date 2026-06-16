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

import stark.ds.DataRange;
import stark.speclang.values.StarkValue;

/**
 * This type describes error values. This class is a singleton.
 */
public final class StarkErrorType implements StarkType {

    private static StarkErrorType instance;

    /**
     * Returns the instance of error type.
     *
     * @return the instance of error type.
     */
    public static StarkErrorType getInstance() {
        if (instance == null) {
            instance = new StarkErrorType();
        }
        return instance;
    }

    /**
     * Creates a new instance of error type.
     */
    private StarkErrorType() {}

    @Override
    public StarkType merge(StarkType other) {
        return this;
    }

    @Override
    public boolean isCompatibleWith(StarkType other) {
        return false;
    }

    @Override
    public boolean isNumerical() {
        return false;
    }


    @Override
    public boolean isError() {
        return true;
    }

    @Override
    public boolean canBeMergedWith(StarkType other) {
        return true;
    }

    @Override
    public StarkValue valueOf(double v) {
        return StarkValue.ERROR_VALUE;
    }

    @Override
    public DataRange getDefaultDataRange() {
        return new DataRange(Double.NaN, Double.NaN);
    }

    @Override
    public String toString() {
        return StarkType.ERROR_TYPE_STRING;
    }

}
