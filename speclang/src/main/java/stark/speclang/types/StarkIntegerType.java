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
import stark.speclang.values.StarkInteger;
import stark.speclang.values.StarkValue;

/**
 * A class representing an integer type.
 */
public final class StarkIntegerType implements StarkType {

    private static StarkIntegerType instance;

    public static StarkIntegerType getInstance() {
        if (instance == null) {
            instance = new StarkIntegerType();
        }
        return instance;
    }

    public StarkIntegerType() {}

    @Override
    public StarkType merge(StarkType other) {
        if ((this == other)) return this;
        if ((other.deterministicType()==REAL_TYPE)||(other.deterministicType()==INTEGER_TYPE)) return other;
        return StarkType.ERROR_TYPE;
    }

    @Override
    public boolean isCompatibleWith(StarkType other) {
        return (other.deterministicType() == INTEGER_TYPE);
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
        return (this==deterministicOther)||(deterministicOther==REAL_TYPE);
    }

    @Override
    public boolean isInteger() {
        return true;
    }

    @Override
    public StarkValue valueOf(double v) {
        return new StarkInteger((int) v);
    }

    @Override
    public DataRange getDefaultDataRange() {
        return new DataRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public String toString() {
        return StarkType.INTEGER_TYPE_STRING;
    }

    public StarkValue fromDouble(double v) {
        return new StarkInteger((int) v);
    }
}
