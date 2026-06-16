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
 * This interface is used to model data types occurring in a JSpear specification.
 */
public sealed interface StarkType permits StarkBooleanType, StarkCustomType, StarkErrorType, StarkIntegerType, StarkRandomType, StarkRealType {


    /**
     * Type assigned to expressions with errors.
     */
    StarkType ERROR_TYPE = StarkErrorType.getInstance();

    /**
     * Type assigned to boolean expressions.
     */
    StarkType BOOLEAN_TYPE = StarkBooleanType.getInstance();

    /**
     * Type assigned to integer expressions.
     */
    StarkType INTEGER_TYPE = StarkIntegerType.getInstance();

    /**
     * Type assigned to real expressions.
     */
    StarkType REAL_TYPE = StarkRealType.getInstance();



    /**
     * Type assigned to array expressions.
     */
    String INTEGER_TYPE_STRING = "int";
    String REAL_TYPE_STRING = "real";
    String BOOLEAN_TYPE_STRING = "bool";
    String ERROR_TYPE_STRING = "error";
    String RANDOM_TYPE_STRING = "random";

    /**
     * Returns the type obtained by merging <code>this</code> type with the <code>other</code>. An error type
     * is returned if the two types are not compatible.
     *
     * @param other another type.
     * @return the type obtained by merging <code>this</code> type with the <code>other</code>.
     */
    StarkType merge(StarkType other);

    /**
     * Returns the type obtained by merging <code>one</code> type with the <code>other</code>. An error type
     *  is returned if the two types are not compatible.
     * @param one a type.
     * @param other another type.
     * @return the type obtained by merging <code>one</code> type with the <code>other</code>.
     */
    static StarkType merge(StarkType one, StarkType other) {
        return one.merge(other);
    }

    /**
     * This method returns true if <code>this</code> type is compatible with
     * the <code>other</code>. Namely, if we can assign to a variable of <code>this</code>
     * type a value of the <code>other</code> type.
     *
     * @param other a type.
     * @return true if <code>this</code> type is compatible with the <code>other</code>.
     */
    boolean isCompatibleWith(StarkType other);

    /**
     * Returns true if <code>this</code> type represents numerical values.
     *
     * @return true if <code>this</code> type represents numerical values.
     */
    boolean isNumerical();

    /**
     * Returns true if <code>this</code> type represents an error.
     *
     * @return true if <code>this</code> type represents an error.
     */
    boolean isError();


    /**
     * Returns true if <code>this</code> type can be merged with the <code>other</code> type.
     *
     * @return true if <code>this</code> type can be merged with the <code>other</code> type.
     */
    boolean canBeMergedWith(StarkType other);

    /**
     * Returns true if <code>this</code> type represents integer values.
     *
     * @return true if <code>this</code> type represents integer values.
     */
    default boolean isInteger() {
        return false;
    }

    /**
     * Returns true if <code>this</code> type represents boolean values.
     *
     * @return true if <code>this</code> type represents boolean values.
     */
    default boolean isBoolean() {
        return false;
    }

    /**
     * Returns true if <code>this</code> type represents real values.
     *
     * @return true if <code>this</code> type represents real values.
     */
    default boolean isReal() {
        return false;
    }

    /**
     * Returns true if <code>this</code> type represents a user defined type.
     *
     * @return true if <code>this</code> type represents a user defined type.
     */
    default boolean isCustom() {
        return false;
    }

    /**
     * Returns true if this type represents random values.
     *
     * @return true if this type represents random values.
     */
    default boolean isRandom() { return false; }

    /**
     * Returns the deterministic version of this type.
     *
     * @return the deterministic version of this type.
     */
    default StarkType deterministicType() {
        return this;
    }

    /**
     * Returns the value represented by the given parameter.
     *
     * @param v double representation of a value
     * @return the value represented by the given parameter.
     */
    StarkValue valueOf(double v);

    DataRange getDefaultDataRange();

}
