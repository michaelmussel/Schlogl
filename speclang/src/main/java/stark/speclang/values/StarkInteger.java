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
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

public final class StarkInteger implements StarkValue {
    private final int value;

    public StarkInteger(int value) {
        this.value = value;
    }

    @Override
    public StarkType getJSpearType() {
        return StarkType.INTEGER_TYPE;
    }

    @Override
    public double toDouble() {
        return this.value;
    }



    public int value() {
        return value;
    }

    public StarkValue sum(StarkValue v) {
        if (v instanceof StarkInteger intValue) {
            return new StarkInteger(this.value+ intValue.value);
        }
        if (v instanceof StarkReal realValue) {
            return new StarkReal(this.value+ realValue.value());
        }
        return StarkValue.ERROR_VALUE;
    }

    public StarkValue product(StarkValue v) {
        if (v instanceof StarkInteger intValue) {
            return new StarkInteger(this.value* intValue.value);
        }
        if (v instanceof StarkReal realValue) {
            return new StarkReal(this.value* realValue.value());
        }
        return StarkValue.ERROR_VALUE;
    }

    public StarkValue subtraction(StarkValue v) {
        if (v instanceof StarkInteger intValue) {
            return new StarkInteger(this.value- intValue.value);
        }
        if (v instanceof StarkReal realValue) {
            return new StarkReal(this.value- realValue.value());
        }
        return StarkValue.ERROR_VALUE;
    }

    public StarkValue division(StarkValue v) {
        if (v instanceof StarkInteger intValue) {
            return new StarkInteger(this.value/ intValue.value);
        }
        if (v instanceof StarkReal realValue) {
            return new StarkReal(this.value/ realValue.value());
        }
        return StarkValue.ERROR_VALUE;
    }

    public StarkValue modulo(StarkValue v) {
        if (v instanceof StarkInteger intValue) {
            return new StarkInteger(this.value%intValue.value);
        }
        if (v instanceof StarkReal realValue) {
            return new StarkReal(this.value%realValue.value());
        }
        return StarkValue.ERROR_VALUE;
    }

    public StarkValue apply(DoubleBinaryOperator op, StarkValue v) {
        if (v instanceof StarkInteger intValue) {
            return new StarkReal(op.applyAsDouble(this.value, intValue.value));
        }
        if (v instanceof StarkReal realValue) {
            return new StarkReal(op.applyAsDouble(this.value, realValue.value()));
        }
        return StarkValue.ERROR_VALUE;
    }

    public StarkValue apply(DoubleUnaryOperator op) {
        return new StarkReal(op.applyAsDouble(this.value));
    }

    public StarkValue isLessThan(StarkValue other) {
        if (other instanceof StarkInteger intValue) {
            return StarkBoolean.of(this.value()<intValue.value());
        }
        if (other instanceof StarkReal realValue) {
            return StarkBoolean.of(this.value()<realValue.value());
        }
        return StarkValue.ERROR_VALUE;
    }

    public StarkValue isLessOrEqualThan(StarkValue other) {
        if (other instanceof StarkInteger intValue) {
            return StarkBoolean.of(this.value()<=intValue.value());
        }
        if (other instanceof StarkReal realValue) {
            return StarkBoolean.of(this.value()<=realValue.value());
        }
        return StarkValue.ERROR_VALUE;
    }

    public StarkValue isEqualTo(StarkValue other) {
        if (other instanceof StarkInteger intValue) {
            return StarkBoolean.of(this.value()==intValue.value());
        }
        if (other instanceof StarkReal realValue) {
            return StarkBoolean.of(this.value()==realValue.value());
        }
        return StarkValue.ERROR_VALUE;
    }

    public StarkValue isGreaterOrEqualThan(StarkValue other) {
        if (other instanceof StarkInteger intValue) {
            return StarkBoolean.of(this.value()>=intValue.value());
        }
        if (other instanceof StarkReal realValue) {
            return StarkBoolean.of(this.value()>=realValue.value());
        }
        return StarkValue.ERROR_VALUE;
    }

    public StarkValue isGreaterThan(StarkValue other) {
        if (other instanceof StarkInteger intValue) {
            return StarkBoolean.of(this.value()>intValue.value());
        }
        if (other instanceof StarkReal realValue) {
            return StarkBoolean.of(this.value()>realValue.value());
        }
        return StarkValue.ERROR_VALUE;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StarkInteger that = (StarkInteger) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
