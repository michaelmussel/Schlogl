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

import org.antlr.v4.runtime.Token;

public interface TypeEvaluationContext {

    boolean isDefined(String name);

    boolean isAReference(String name);

    StarkType getTypeOf(String name);

    boolean isAFunction(String functionName);

    StarkType[] getArgumentsType(String functionName);

    StarkType getReturnType(String functionName);


    static TypeEvaluationContext letContext(TypeEvaluationContext outerContext, Token name, StarkType type) {
        return new LetTypeEvaluationContext(outerContext, name.getText(), type);
    }

}
