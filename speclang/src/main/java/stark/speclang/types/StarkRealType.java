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
import stark.speclang.values.StarkReal;
import stark.speclang.values.StarkValue;

/**
 * A class representing a real type.
 */
public final class StarkRealType implements StarkType {

    private static StarkRealType instance;

    public static StarkRealType getInstance() {
        if (instance == null) {
            instance = new StarkRealType();
        }
        return instance;
    }

    @Override
    public StarkType merge(StarkType other) {
        if ((this == other)) return this;
        if (other.deterministicType()==REAL_TYPE) return other;
        if (other.deterministicType()==INTEGER_TYPE) {
            if (other.isRandom()) {
                return new StarkRandomType(this);
            } else {
                return this;
            }
        }
        return StarkType.ERROR_TYPE;
    }

    @Override
    public boolean isCompatibleWith(StarkType other) {
        return other.isNumerical();
    }

    @Override
    public boolean isNumerical() {
        return true;
    }


    @Override
    public boolean isError() {
        return false;
    }

    @Override
    public boolean canBeMergedWith(StarkType other) {
        StarkType deterministicOther = other.deterministicType();
        return (this==deterministicOther)||(deterministicOther==INTEGER_TYPE);
    }

    @Override
    public boolean isReal() {
        return true;
    }

    @Override
    public StarkValue valueOf(double v) {
        return new StarkReal(v);
    }

    @Override
    public DataRange getDefaultDataRange() {
        return new DataRange();
    }

    @Override
    public String toString() {
        return StarkType.REAL_TYPE_STRING;
    }

}
