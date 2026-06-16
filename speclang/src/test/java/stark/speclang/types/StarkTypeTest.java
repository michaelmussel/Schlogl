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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StarkTypeTest {

    private final StarkType customType = new StarkCustomType("testType", new String[] { "element1", "element2" , "element3"} );
    private final StarkType theSameCustomType = new StarkCustomType("testType", new String[] { "element1", "element2" , "element3"} );

    private final StarkType[][] UNMERGEABLE_TYPES = {
            {  customType, StarkType.BOOLEAN_TYPE },
            {  customType, StarkType.INTEGER_TYPE },
            {  customType, StarkType.REAL_TYPE },
            {  customType, new StarkRandomType(StarkType.BOOLEAN_TYPE) },
            {  customType, new StarkRandomType(StarkType.INTEGER_TYPE) },
            {  customType, new StarkRandomType(StarkType.REAL_TYPE) },
            {  StarkType.INTEGER_TYPE, StarkType.BOOLEAN_TYPE },
            {  StarkType.INTEGER_TYPE, new StarkRandomType(StarkType.BOOLEAN_TYPE) },
            {  StarkType.REAL_TYPE, StarkType.BOOLEAN_TYPE },
            {  StarkType.REAL_TYPE, new StarkRandomType(StarkType.BOOLEAN_TYPE) },
            {  StarkType.BOOLEAN_TYPE, StarkType.INTEGER_TYPE },
            {  StarkType.BOOLEAN_TYPE, StarkType.REAL_TYPE },
            {  StarkType.BOOLEAN_TYPE, new StarkRandomType(StarkType.INTEGER_TYPE) },
            {  StarkType.BOOLEAN_TYPE, new StarkRandomType(StarkType.REAL_TYPE) },
    };

    private final StarkType[][] MERGEABLE_TYPES = {
            //CUSTOM_TYPE
            {  customType, theSameCustomType, customType },
            {  customType, new StarkRandomType(theSameCustomType), new StarkRandomType(customType) },
            //INTEGER_TYPE
            { StarkType.INTEGER_TYPE, StarkType.INTEGER_TYPE , StarkType.INTEGER_TYPE},
            { StarkType.INTEGER_TYPE, StarkType.REAL_TYPE , StarkType.REAL_TYPE},
            { StarkType.INTEGER_TYPE, new StarkRandomType(StarkType.INTEGER_TYPE), new StarkRandomType(StarkType.INTEGER_TYPE) },
            { StarkType.INTEGER_TYPE, new StarkRandomType(StarkType.REAL_TYPE), new StarkRandomType(StarkType.REAL_TYPE) },
            //REAL_TYPE
            { StarkType.REAL_TYPE, StarkType.INTEGER_TYPE , StarkType.REAL_TYPE},
            { StarkType.REAL_TYPE, StarkType.REAL_TYPE , StarkType.REAL_TYPE},
            { StarkType.REAL_TYPE, new StarkRandomType(StarkType.INTEGER_TYPE), new StarkRandomType(StarkType.REAL_TYPE) },
            { StarkType.REAL_TYPE, new StarkRandomType(StarkType.REAL_TYPE), new StarkRandomType(StarkType.REAL_TYPE) },
            //BOOLEAN_TYPE
            { StarkType.BOOLEAN_TYPE, StarkType.BOOLEAN_TYPE , StarkType.BOOLEAN_TYPE},
            { StarkType.BOOLEAN_TYPE, new StarkRandomType(StarkType.BOOLEAN_TYPE) , new StarkRandomType(StarkType.BOOLEAN_TYPE) },
            //ARRAY_TYPE
            //RANDOM[INTEGER_TYPE]
            { new StarkRandomType(StarkType.INTEGER_TYPE), StarkType.INTEGER_TYPE , new StarkRandomType(StarkType.INTEGER_TYPE)},
            { new StarkRandomType(StarkType.INTEGER_TYPE), StarkType.REAL_TYPE , new StarkRandomType(StarkType.REAL_TYPE)},
            { new StarkRandomType(StarkType.INTEGER_TYPE), new StarkRandomType(StarkType.INTEGER_TYPE), new StarkRandomType(StarkType.INTEGER_TYPE) },
            { new StarkRandomType(StarkType.INTEGER_TYPE), new StarkRandomType(StarkType.REAL_TYPE), new StarkRandomType(StarkType.REAL_TYPE) },
            //RANDOM[INTEGER_TYPE]
            { new StarkRandomType(StarkType.REAL_TYPE), StarkType.INTEGER_TYPE , new StarkRandomType(StarkType.REAL_TYPE)},
            { new StarkRandomType(StarkType.REAL_TYPE), StarkType.REAL_TYPE , new StarkRandomType(StarkType.REAL_TYPE)},
            { new StarkRandomType(StarkType.REAL_TYPE), new StarkRandomType(StarkType.INTEGER_TYPE), new StarkRandomType(StarkType.REAL_TYPE) },
            { new StarkRandomType(StarkType.REAL_TYPE), new StarkRandomType(StarkType.REAL_TYPE), new StarkRandomType(StarkType.REAL_TYPE) },
            //RANDOM[BOOLEAN_TYPE]
            {  new StarkRandomType(StarkType.BOOLEAN_TYPE) , StarkType.BOOLEAN_TYPE, new StarkRandomType(StarkType.BOOLEAN_TYPE) },
            {  new StarkRandomType(StarkType.BOOLEAN_TYPE) , new StarkRandomType(StarkType.BOOLEAN_TYPE) , new StarkRandomType(StarkType.BOOLEAN_TYPE) },
            //RANDOM[ARRAY_TYPE]
    };

    private final StarkType[][] COMPATIBLE_TYPES = {
            {StarkType.BOOLEAN_TYPE, StarkType.BOOLEAN_TYPE},
            {StarkType.BOOLEAN_TYPE, new StarkRandomType(StarkType.BOOLEAN_TYPE) },
            {StarkType.INTEGER_TYPE, StarkType.INTEGER_TYPE},
            {StarkType.INTEGER_TYPE, new StarkRandomType(StarkType.INTEGER_TYPE) },
            {StarkType.REAL_TYPE, StarkType.INTEGER_TYPE},
            {StarkType.REAL_TYPE, StarkType.REAL_TYPE},
            {StarkType.REAL_TYPE, new StarkRandomType(StarkType.INTEGER_TYPE) },
            {StarkType.REAL_TYPE, new StarkRandomType(StarkType.REAL_TYPE) },
            {customType, theSameCustomType},
            {customType, new StarkRandomType(theSameCustomType)}
    };

    @Test
    public void testTypesThatShouldBeMerged() {
        for (StarkType[] mergeable_type : MERGEABLE_TYPES) {
            assertTrue(mergeable_type[0].canBeMergedWith(mergeable_type[1]), mergeable_type[0] + " should be merged with " + mergeable_type[1]);
        }
    }

    @Test
    public void testTypesThatCannotBeMerged() {
        for (StarkType[] incompatible_types : UNMERGEABLE_TYPES) {
            assertFalse(incompatible_types[0].canBeMergedWith(incompatible_types[1]), incompatible_types[0] + " should not be merged with " + incompatible_types[1]);
        }
    }

    @Test
    public void testMergingTypesResults() {
        for (StarkType[] mergeable_type : MERGEABLE_TYPES) {
            assertEquals(mergeable_type[2], mergeable_type[0].merge(mergeable_type[1]), mergeable_type[0] + ".merge("+ mergeable_type[1]+")");
        }
    }

    @Test
    public void testErrorMergingTypesResults() {
        for (StarkType[] incompatibleType : UNMERGEABLE_TYPES) {
            assertEquals(StarkType.ERROR_TYPE, incompatibleType[0].merge(incompatibleType[1]), incompatibleType[0] + ".merge("+ incompatibleType[1]+")");
        }
    }

    @Test
    public void testSubtyping() {
        for (StarkType[] subtypes : COMPATIBLE_TYPES) {
            assertTrue(subtypes[0].isCompatibleWith(subtypes[1]), subtypes[0] + ".isCompatibleWith("+ subtypes[1]+")");
        }
    }

}