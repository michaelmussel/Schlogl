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

package stark.speclang.values;

import stark.speclang.types.StarkType;

import java.util.Objects;

public final class StarkBoolean implements StarkValue {

    public static final StarkValue TRUE = new StarkBoolean(true);
    public static final StarkValue FALSE = new StarkBoolean(false);
    private final boolean value;

    private StarkBoolean(boolean value) {
        this.value = value;
    }

    public static StarkValue of(boolean b) {
        return (b?TRUE:FALSE);
    }


    @Override
    public StarkType getJSpearType() {
        return StarkType.BOOLEAN_TYPE;
    }

    @Override
    public double toDouble() {
        return (value?1.0:0.0);
    }

    public StarkValue negate() {
        return (this.value?FALSE:TRUE);
    }

    public StarkValue and(StarkValue other) {
        if (other instanceof StarkBoolean booleanValue) {
            return StarkBoolean.of(this.value()&&booleanValue.value());
        }
        return StarkValue.ERROR_VALUE;
    }

    public StarkValue or(StarkValue other) {
        if (other instanceof StarkBoolean booleanValue) {
            return StarkBoolean.of(this.value()||booleanValue.value());
        }
        return StarkValue.ERROR_VALUE;
    }

    public boolean value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StarkBoolean that = (StarkBoolean) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
