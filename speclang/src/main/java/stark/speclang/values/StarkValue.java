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
import org.apache.commons.math3.random.RandomGenerator;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;

public sealed interface StarkValue permits StarkBoolean, StarkInteger, StarkReal, StarkCustomValue, StarkErrorValue {

    StarkValue ERROR_VALUE = new StarkErrorValue();

    static boolean isTrue(StarkValue value) {
        if (value instanceof StarkBoolean booleanValue) {
            return booleanValue.value();
        }
        return false;
    }

    static int intValue(StarkValue value) {
        if (value instanceof StarkInteger integerValue) {
            return integerValue.value();
        }
        return 0;
    }


//    static StarkValue of(StarkType type, String variable, DataState ds) {
//        if (type.isAnArray()) {
//            StarkValue[] elements = IntStream.range(variable.getFirstCellIndex(), variable.getFirstCellIndex()+variable.getSize())
//                    .mapToObj(i -> StarkValue.realValue(ds.getValue(i)))
//                    .toArray(StarkValue[]::new);
//            return new JSpearArray(elements);
//        }
//        if (type.isInteger()) {
//            return new StarkInteger((int) ds.getValue(variable.getFirstCellIndex()));
//        }
//        if (type.isReal()) {
//            return new StarkReal(ds.getValue(variable.getFirstCellIndex()));
//        }
//        if (type.isBoolean()) {
//            return StarkBoolean.getBooleanValue(ds.getValue(variable.getFirstCellIndex())>0);
//        }
//        if (type.isCustom()) {
//            return new StarkCustomValue((StarkCustomType) type, (int) ds.getValue(variable.getFirstCellIndex()));
//        }
//        return StarkValue.ERROR_VALUE;
//    }


    StarkType getJSpearType();

    static StarkValue sum(StarkValue v1, StarkValue v2) {
        if (v1 instanceof StarkInteger intValue) {
            return intValue.sum(v2);
        }
        if (v1 instanceof StarkReal realValue) {
            return realValue.sum(v2);
        }
        return StarkValue.ERROR_VALUE;
    }

    static StarkValue product(StarkValue v1, StarkValue v2) {
        if (v1 instanceof StarkInteger intValue) {
            return intValue.product(v2);
        }
        if (v1 instanceof StarkReal realValue) {
            return realValue.product(v2);
        }
        return StarkValue.ERROR_VALUE;
    }

    static StarkValue subtraction(StarkValue v1, StarkValue v2) {
        if (v1 instanceof StarkInteger intValue) {
            return intValue.subtraction(v2);
        }
        if (v1 instanceof StarkReal realValue) {
            return realValue.subtraction(v2);
        }
        return StarkValue.ERROR_VALUE;
    }

    static StarkValue division(StarkValue v1, StarkValue v2) {
        if (v1 instanceof StarkInteger intValue) {
            return intValue.division(v2);
        }
        if (v1 instanceof StarkReal realValue) {
            return realValue.division(v2);
        }
        return StarkValue.ERROR_VALUE;
    }


    static StarkValue modulo(StarkValue v1, StarkValue v2) {
        if (v1 instanceof StarkInteger intValue) {
            return intValue.modulo(v2);
        }
        if (v1 instanceof StarkReal realValue) {
            return realValue.modulo(v2);
        }
        return StarkValue.ERROR_VALUE;
    }

    static StarkValue apply(DoubleBinaryOperator op, StarkValue v1, StarkValue v2) {
        if (v1 instanceof StarkInteger intValue) {
            return intValue.apply(op, v2);
        }
        if (v1 instanceof StarkReal realValue) {
            return realValue.apply(op, v2);
        }
        return StarkValue.ERROR_VALUE;
    }

    static StarkValue apply(DoubleUnaryOperator op, StarkValue v) {
        if (v instanceof StarkInteger intValue) {
            return intValue.apply(op);
        }
        if (v instanceof StarkReal realValue) {
            return realValue.apply(op);
        }
        return StarkValue.ERROR_VALUE;
    }

    static StarkValue negate(StarkValue v) {
        if (v instanceof StarkBoolean booleanValue) {
            return booleanValue.negate();
        }
        return StarkValue.ERROR_VALUE;
    }

    static StarkValue isLessThan(StarkValue v1, StarkValue v2) {
        if (v1 instanceof StarkInteger intValue) {
            return intValue.isLessThan(v2);
        }
        if (v1 instanceof StarkReal realValue) {
            return realValue.isLessThan(v2);
        }
        return StarkValue.ERROR_VALUE;
    }

    static StarkValue isLessOrEqualThan(StarkValue v1, StarkValue v2) {
        if (v1 instanceof StarkInteger intValue) {
            return intValue.isLessOrEqualThan(v2);
        }
        if (v1 instanceof StarkReal realValue) {
            return realValue.isLessOrEqualThan(v2);
        }
        return StarkValue.ERROR_VALUE;
    }

    static StarkValue isEqualTo(StarkValue v1, StarkValue v2) {
        if (v1 instanceof StarkInteger intValue) {
            return intValue.isEqualTo(v2);
        }
        if (v1 instanceof StarkReal realValue) {
            return realValue.isEqualTo(v2);
        }
        return StarkValue.ERROR_VALUE;
    }


    static StarkValue isGreaterOrEqualThan(StarkValue v1, StarkValue v2) {
        if (v1 instanceof StarkInteger intValue) {
            return intValue.isGreaterOrEqualThan(v2);
        }
        if (v1 instanceof StarkReal realValue) {
            return realValue.isGreaterOrEqualThan(v2);
        }
        return StarkValue.ERROR_VALUE;
    }

    static StarkValue isGreaterThan(StarkValue v1, StarkValue v2) {
        if (v1 instanceof StarkInteger intValue) {
            return intValue.isGreaterThan(v2);
        }
        if (v1 instanceof StarkReal realValue) {
            return realValue.isGreaterThan(v2);
        }
        return StarkValue.ERROR_VALUE;
    }


    static StarkValue and(StarkValue v1, StarkValue v2) {
        if (v1 instanceof StarkBoolean booleanValue) {
            return booleanValue.and(v2);
        }
        return StarkValue.ERROR_VALUE;
    }

    static StarkValue or(StarkValue v1, StarkValue v2) {
        if (v1 instanceof StarkBoolean booleanValue) {
            return booleanValue.or(v2);
        }
        return StarkValue.ERROR_VALUE;
    }


     static StarkValue sampleNormal(RandomGenerator rg, StarkValue v1, StarkValue v2) {
        return new StarkReal(rg.nextDouble()*doubleOf(v1)+doubleOf(v2));
    }

    static StarkValue sample(RandomGenerator rg, StarkValue from, StarkValue to) {
        double fromValue = doubleOf(from);
        double gapValue = doubleOf(to)-fromValue;
        return new StarkReal(fromValue+rg.nextDouble()*gapValue);
    }

    static double doubleOf(StarkValue v) {
        if (v instanceof StarkInteger integerValue) {
            return integerValue.value();
        }
        if (v instanceof StarkReal realValue) {
            return realValue.value();
        }
        return Double.NaN;
    }

    static StarkValue ifThenElse(StarkValue eval, Supplier<StarkValue> v1, Supplier<StarkValue> v2) {
        if (eval instanceof StarkBoolean booleanValue) {
            return (booleanValue.value()?v1.get(): v2.get());
        } else {
            return StarkValue.ERROR_VALUE;
        }
    }

    double toDouble();
}
