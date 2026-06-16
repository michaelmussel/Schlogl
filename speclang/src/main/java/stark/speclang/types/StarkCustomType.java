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
import stark.speclang.values.StarkCustomValue;
import stark.speclang.values.StarkValue;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class describes a user defined types. Each custom type has a unique name and consists of a set of <i>elements</i>.
 * Each element is associated with an integer value.
 */
public final class StarkCustomType implements StarkType {

    private final String customTypeName;

    private final String[] typeElements;

    private final Map<String,Integer> elementsCode;

    /**
     * Creates a new custom type with the given name and with the given elements.
     *
     * @param customTypeName name of the custom type.
     * @param typeElements names of the elements in the type.
     */
    public StarkCustomType(String customTypeName, String[] typeElements) {
        this.customTypeName = customTypeName;
        this.typeElements = typeElements;
        this.elementsCode = IntStream.range(0, typeElements.length).boxed().collect(Collectors.toMap(i -> typeElements[i], i -> i));
    }

    /**
     * Returns the code assigned with the given name.
     *
     * @param elementName the name of an element in the type.
     * @return the code assigned with the given name or -1 if the name is unknown.
     */
    public int getCode(String elementName) {
        return this.elementsCode.getOrDefault(elementName, -1);
    }

    @Override
    public StarkType merge(StarkType other) {
        if (other.deterministicType().equals(this)) {
            return other;
        }
        return StarkCustomType.ERROR_TYPE;
    }

    @Override
    public boolean isCompatibleWith(StarkType other) {
        return this.equals(other.deterministicType());
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
        return this.equals(other.deterministicType());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StarkCustomType that = (StarkCustomType) o;
        return customTypeName.equals(that.customTypeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customTypeName);
    }

    @Override
    public boolean isCustom() {
        return true;
    }

    @Override
    public StarkValue valueOf(double v) {
        return new StarkCustomValue(this, (int) v);
    }

    @Override
    public DataRange getDefaultDataRange() {
        return new DataRange(0, this.typeElements.length);
    }

    @Override
    public String toString() {
        return customTypeName;
    }

    public StarkValue getValueOf(String name) {
        return new StarkCustomValue(this, getCode(name));
    }

    public StarkCustomValue[] getValues( ) {
        return IntStream.range(0, this.typeElements.length).mapToObj(i -> new StarkCustomValue(this, i)).toArray(StarkCustomValue[]::new);
    }

    public String getNameOf(int elementIndex) {
        return this.typeElements[elementIndex];
    }
}
