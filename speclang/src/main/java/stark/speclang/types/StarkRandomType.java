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

import java.util.Objects;

/**
 * A class representing a type for random values.
 */
public final class StarkRandomType implements StarkType {

    private final StarkType contentType;

    /**
     * Creates a new type with the given content.
     *
     * @param contentType the type of randomly generated values.
     */
    public StarkRandomType(StarkType contentType) {
        if (contentType.isRandom()) {
            this.contentType = contentType.deterministicType();
        } else {
            this.contentType = contentType;
        }
    }

    @Override
    public StarkType merge(StarkType other) {
        StarkType mergedContent;
        if (other instanceof StarkRandomType) {
            mergedContent = this.contentType.merge(((StarkRandomType) other).getContentType());
        } else {
            mergedContent = this.contentType.merge(other);
        }
        if (mergedContent.isError()||(mergedContent instanceof StarkRandomType)) {
            return mergedContent;
        }
        return new StarkRandomType(mergedContent);
    }

    @Override
    public boolean isCompatibleWith(StarkType other) {
        if (other instanceof StarkRandomType) {
            return this.contentType.isCompatibleWith(((StarkRandomType) other).contentType);
        } else {
            return this.contentType.isCompatibleWith(other);
        }
    }

    @Override
    public boolean isNumerical() {
        return this.contentType.isNumerical();
    }


    @Override
    public boolean isError() {
        return false;
    }

    @Override
    public boolean canBeMergedWith(StarkType other) {
        if (other instanceof StarkRandomType) {
            return this.contentType.canBeMergedWith(((StarkRandomType) other).contentType);
        } else {
            return this.contentType.canBeMergedWith(other);
        }
    }

    /**
     * Returns the type of the randomly generated values.
     *
     * @return  the type of the randomly generated values.
     */
    public StarkType getContentType() {
        return contentType;
    }

    @Override
    public boolean isRandom() {
        return true;
    }

    @Override
    public boolean isInteger() {
        return this.contentType.isInteger();
    }

    @Override
    public boolean isBoolean() {
        return this.contentType.isBoolean();
    }

    @Override
    public boolean isReal() {
        return this.contentType.isReal();
    }

    @Override
    public boolean isCustom() {
        return this.contentType.isCustom();
    }

    @Override
    public String toString() {
        return StarkType.RANDOM_TYPE_STRING+"["+contentType.toString()+"]";
    }

    @Override
    public StarkType deterministicType() {
        return this.contentType;
    }

    @Override
    public StarkValue valueOf(double v) {
        return this.contentType.valueOf(v);
    }

    @Override
    public DataRange getDefaultDataRange() {
        return this.contentType.getDefaultDataRange();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof StarkRandomType)) return false;
        return this.contentType.equals(((StarkRandomType) obj).contentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(RANDOM_TYPE_STRING, this.contentType);
    }
}
