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
import stark.speclang.values.StarkBoolean;
import stark.speclang.values.StarkValue;

/**
 * This type describes the set of boolean values.
 */
public final class StarkBooleanType implements StarkType {

    private static StarkBooleanType instance;

    public static StarkBooleanType getInstance() {
        if (instance == null) {
            instance = new StarkBooleanType();
        }
        return instance;
    }

    private StarkBooleanType() {}

    @Override
    public StarkType merge(StarkType other) {
        if (other == StarkType.BOOLEAN_TYPE) {
            return this;
        }
        if (other.deterministicType() == StarkType.BOOLEAN_TYPE) {
            return other;
        }
        return StarkType.ERROR_TYPE;
    }

    @Override
    public boolean isCompatibleWith(StarkType other) {
        return (other.deterministicType() == StarkType.BOOLEAN_TYPE);
    }

    @Override
    public boolean isNumerical() {
        return false;
    }


    @Override
    public boolean isError() {
        return false;
    }

    @Override
    public boolean canBeMergedWith(StarkType other) {
        return (other.deterministicType() == StarkType.BOOLEAN_TYPE);
    }

    @Override
    public boolean isBoolean() {
        return true;
    }

    @Override
    public StarkValue valueOf(double v) {
        if (v==0) {
            return StarkBoolean.FALSE;
        } else {
            return StarkBoolean.TRUE;
        }
    }

    @Override
    public DataRange getDefaultDataRange() {
        return new DataRange(0, 1.0);
    }

    @Override
    public String toString() {
        return StarkType.BOOLEAN_TYPE_STRING;
    }

    public StarkValue fromDouble(double v) {
        return (v>0.0? StarkBoolean.TRUE: StarkBoolean.FALSE);
    }
}
